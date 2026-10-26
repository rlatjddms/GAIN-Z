package com.project.gainz.global.storage;

import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.global.mail.MailClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 저장소 전체 용량 한도와 알림 메일 (#262). 한도 100, 경고 80으로 줄여서 본다
class StorageQuotaTest {

    private final FileStore store = mock(FileStore.class);
    private final MailClient mail = mock(MailClient.class);
    private final MovableClock clock = new MovableClock();
    private final StorageQuota quota = new StorageQuota(store, mail, 100, 80, "admin@example.com", clock);

    @Test
    void 한도를_넘는_파일은_막고_한_번만_메일을_보낸다() throws IOException {
        when(store.totalSize()).thenReturn(90L);

        quota.check(10);
        full(() -> quota.check(11));
        full(() -> quota.check(50));

        verify(mail, times(1)).send(eq("admin@example.com"), contains("가득"), anyString());
    }

    @Test
    void 다시_세기_전에도_올린_만큼_더해서_본다() throws IOException {
        when(store.totalSize()).thenReturn(50L);

        quota.check(30);
        quota.added(30);
        // 80에 닿아서 경고 메일
        verify(mail).send(eq("admin@example.com"), contains("곧"), anyString());
        full(() -> quota.check(21));
        verify(store, times(1)).totalSize();
    }

    @Test
    void 십분이_지나면_다시_세고_줄었으면_다음에_또_알린다() throws IOException {
        when(store.totalSize()).thenReturn(100L, 10L, 100L);

        full(() -> quota.check(1));
        clock.later(StorageQuota.RECOUNT.toMinutes());
        // 파일을 지워서 줄었다
        quota.check(50);
        clock.later(StorageQuota.RECOUNT.toMinutes());
        full(() -> quota.check(1));

        verify(mail, times(2)).send(eq("admin@example.com"), contains("가득"), anyString());
    }

    @Test
    void 용량을_모르면_막지_않고_메일이_실패해도_막기는_한다() throws IOException {
        when(store.totalSize()).thenThrow(new IOException("목록 실패"));
        quota.check(1_000);

        FileStore unknown = mock(FileStore.class);
        when(unknown.totalSize()).thenReturn(-1L);
        StorageQuota noCount = new StorageQuota(unknown, mail, 100, 80, "", clock);
        noCount.check(1_000);

        doReturn(100L).when(store).totalSize();
        clock.later(StorageQuota.RECOUNT.toMinutes());
        doThrow(new RuntimeException("메일 실패")).when(mail).send(anyString(), anyString(), anyString());
        full(() -> quota.check(1));
    }

    @Test
    void 알림_주소가_없으면_메일을_보내지_않는다() throws IOException {
        when(store.totalSize()).thenReturn(100L);
        StorageQuota quiet = new StorageQuota(store, mail, 100, 80, "", clock);

        full(() -> quiet.check(1));
        verify(mail, never()).send(anyString(), anyString(), anyString());
    }

    private static void full(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STORAGE_FULL));
    }

    private static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-10-27T00:00:00Z");

        void later(long minutes) {
            now = now.plusSeconds(minutes * 60);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
