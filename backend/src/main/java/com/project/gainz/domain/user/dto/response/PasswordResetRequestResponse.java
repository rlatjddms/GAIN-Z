package com.project.gainz.domain.user.dto.response;

import com.project.gainz.domain.user.enums.SocialProvider;

// 비밀번호 재설정 메일 요청 결과 (#193).
// 가입 여부는 회원가입 중복 확인에서도 알 수 있어서, 기억이 안 나는 사람이 헷갈리지 않게 그대로 알려 준다
// status: SENT(보냄) / NOT_FOUND(가입되지 않은 이메일) / SOCIAL_ONLY(소셜로만 가입, provider로 로그인)
public record PasswordResetRequestResponse(String status, SocialProvider provider) {

    public static PasswordResetRequestResponse sent() {
        return new PasswordResetRequestResponse("SENT", null);
    }

    public static PasswordResetRequestResponse notFound() {
        return new PasswordResetRequestResponse("NOT_FOUND", null);
    }

    public static PasswordResetRequestResponse socialOnly(SocialProvider provider) {
        return new PasswordResetRequestResponse("SOCIAL_ONLY", provider);
    }
}
