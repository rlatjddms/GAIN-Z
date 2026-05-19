package com.project.gainz.domain.user.dto.response;

public record TokenResponse(

        String accessToken,
        String refreshToken
) {
}
