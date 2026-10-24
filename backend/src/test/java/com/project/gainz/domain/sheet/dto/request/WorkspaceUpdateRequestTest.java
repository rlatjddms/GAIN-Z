package com.project.gainz.domain.sheet.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 키 바꿔 치기에 내가 추가한 코드 입력 검증 (#192)
class WorkspaceUpdateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean valid(List<String> chords) {
        return validator.validate(new WorkspaceUpdateRequest(null, null, null, chords)).isEmpty();
    }

    @Test
    void 코드_모양이면_받는다() {
        assertThat(valid(null)).isTrue();
        assertThat(valid(List.of())).isTrue();
        assertThat(valid(List.of("Bb", "E7", "D/F#", "F#m7b5", "Cmaj7", "Gsus4", "Bbadd9", "Dm7(b5)", "C#°7"))).isTrue();
    }

    @Test
    void 코드가_아니거나_너무_많으면_받지_않는다() {
        assertThat(valid(List.of("hello"))).isFalse();
        assertThat(valid(List.of("bb"))).isFalse();
        assertThat(valid(List.of("C D"))).isFalse();
        assertThat(valid(List.of("<script>"))).isFalse();
        assertThat(valid(Collections.nCopies(21, "C"))).isFalse();
    }
}
