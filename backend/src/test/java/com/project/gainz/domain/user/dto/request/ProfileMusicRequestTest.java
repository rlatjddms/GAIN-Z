package com.project.gainz.domain.user.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 요즘 듣는 음악 입력 검증 (링크는 선택, http/https만)
class ProfileMusicRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean valid(ProfileMusicRequest request) {
        return validator.validate(request).isEmpty();
    }

    @Test
    void 링크는_없어도_되고_http나_https만_받는다() {
        assertThat(valid(new ProfileMusicRequest("Hysteria", "Muse", null))).isTrue();
        assertThat(valid(new ProfileMusicRequest("Hysteria", "Muse", ""))).isTrue();
        assertThat(valid(new ProfileMusicRequest("Hysteria", "Muse", "https://youtu.be/abc"))).isTrue();
        assertThat(valid(new ProfileMusicRequest("Hysteria", "Muse", "HTTP://open.spotify.com/track/1"))).isTrue();

        assertThat(valid(new ProfileMusicRequest("Hysteria", "Muse", "javascript:alert(1)"))).isFalse();
        assertThat(valid(new ProfileMusicRequest("Hysteria", "Muse", "youtu.be/abc"))).isFalse();
        assertThat(valid(new ProfileMusicRequest("Hysteria", "Muse", "https://a b"))).isFalse();
    }

    @Test
    void 제목과_아티스트는_꼭_넣고_길이를_넘으면_안_된다() {
        assertThat(valid(new ProfileMusicRequest(" ", "Muse", null))).isFalse();
        assertThat(valid(new ProfileMusicRequest("Hysteria", "", null))).isFalse();
        assertThat(valid(new ProfileMusicRequest("a".repeat(101), "Muse", null))).isFalse();
        assertThat(valid(new ProfileMusicRequest("Hysteria", "Muse", "https://" + "a".repeat(500)))).isFalse();
    }
}
