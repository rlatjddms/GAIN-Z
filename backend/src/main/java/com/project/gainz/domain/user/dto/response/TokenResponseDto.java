package com.project.gainz.domain.user.dto.response;

public record TokenResponseDto(

        String accessToken,
        String refreshToken
) {
}
