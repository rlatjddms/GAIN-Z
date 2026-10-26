package com.project.gainz.global.storage;

import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// 브라우저가 바로 올리고 받는 주소 (#262)
class FileLinksTest {

    private static final Instant NOW = Instant.parse("2026-10-26T00:00:00Z");
    private final FileStore store = mock(FileStore.class);
    private final FileLinks links = new FileLinks(store, "secret", Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void 서명_주소를_못_만드는_저장소는_서버_주소를_주고_표로_되읽는다() {
        String upload = links.uploadUrl("tmp/a", "video/mp4", 100);
        String download = links.downloadUrl("band/b", "공연 영상.mp4", "video/mp4", true);

        assertThat(upload).startsWith(FileLinks.SERVER_PATH);
        FileLinks.Ticket put = links.verify(token(upload), "put");
        assertThat(put.key()).isEqualTo("tmp/a");
        assertThat(put.type()).isEqualTo("video/mp4");
        assertThat(put.maxBytes()).isEqualTo(100);
        FileLinks.Ticket get = links.verify(token(download), "get");
        assertThat(get.key()).isEqualTo("band/b");
        assertThat(get.name()).isEqualTo("공연 영상.mp4");
        assertThat(get.download()).isTrue();
    }

    @Test
    void 저장소가_서명_주소를_주면_그대로_쓴다() {
        when(store.presignPut(anyString(), anyString(), any(Duration.class))).thenReturn("https://r2/put");
        when(store.presignGet(anyString(), anyString(), anyString(), anyBoolean(), any(Duration.class))).thenReturn("https://r2/get");

        assertThat(links.uploadUrl("tmp/a", "video/mp4", 100)).isEqualTo("https://r2/put");
        assertThat(links.downloadUrl("band/b", "a.mp4", "video/mp4", false)).isEqualTo("https://r2/get");
    }

    @Test
    void 고친_표_다른_용도_시간이_지난_표는_거절한다() {
        String token = token(links.downloadUrl("band/b", "a.mp4", "video/mp4", false));
        String payload = token.substring(0, token.indexOf('.'));

        rejects(() -> links.verify(token, "put"));
        rejects(() -> links.verify(payload + "x" + token.substring(payload.length()), "get"));
        rejects(() -> links.verify(payload + ".AAAA", "get"));
        rejects(() -> links.verify("nonsense", "get"));
        // 다른 비밀값으로 만든 표
        rejects(() -> new FileLinks(store, "other", Clock.fixed(NOW, ZoneOffset.UTC)).verify(token, "get"));
        // 받기 주소는 10분
        FileLinks later = new FileLinks(store, "secret", Clock.fixed(NOW.plus(Duration.ofMinutes(11)), ZoneOffset.UTC));
        rejects(() -> later.verify(token, "get"));
    }

    private static String token(String url) {
        return url.substring(FileLinks.SERVER_PATH.length());
    }

    private static void rejects(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_LINK_INVALID));
    }
}
