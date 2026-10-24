package com.project.gainz.domain.user.service;

import com.project.gainz.domain.user.enums.SocialProvider;
import lombok.Getter;

// 소셜 로그인·연결을 막을 때 (#189). code는 프론트로 돌려보내는 ?socialError= / ?linkError= 값
@Getter
public class SocialLoginException extends RuntimeException {

    // 같은 이메일 계정이 있는데 자동 연결 조건(양쪽 다 인증된 이메일)이 아님
    public static final String EMAIL_EXISTS = "EMAIL_EXISTS";
    // 소셜에서 이메일을 받지 못함 (제공 동의 안 함)
    public static final String EMAIL_REQUIRED = "EMAIL_REQUIRED";
    // 연결 표가 없거나 5분이 지남
    public static final String LINK_EXPIRED = "LINK_EXPIRED";
    // 이 소셜 계정은 이미 다른 GAIN-Z 계정에 연결됨
    public static final String LINK_TAKEN = "LINK_TAKEN";
    // 이 계정에 같은 종류의 다른 소셜 계정이 이미 연결됨
    public static final String LINK_PROVIDER_EXISTS = "LINK_PROVIDER_EXISTS";

    private final String code;
    private final SocialProvider provider;

    public SocialLoginException(String code, SocialProvider provider) {
        super(code);
        this.code = code;
        this.provider = provider;
    }

    // 연결(마이페이지) 중에 난 오류인지 → 마이페이지로 돌려보낸다
    public boolean isLinkError() {
        return code.startsWith("LINK_");
    }
}
