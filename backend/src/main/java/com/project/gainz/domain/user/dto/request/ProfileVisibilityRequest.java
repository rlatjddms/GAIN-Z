package com.project.gainz.domain.user.dto.request;

import com.project.gainz.domain.user.enums.ProfileVisibility;
import jakarta.validation.constraints.NotNull;

public record ProfileVisibilityRequest(

        @NotNull(message = "공개 범위를 골라 주세요.")
        ProfileVisibility visibility
) {
}
