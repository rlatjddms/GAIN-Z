package com.project.gainz.global.security.oauth2;

public interface OAuth2UserInfo {

    String getProvider();

    // 소셜 쪽 고유 ID (이메일은 바뀔 수 있어서 연결은 이걸로 찾는다, #189)
    String getProviderId();

    String getEmail();

    String getName();

    // 소셜 쪽에서 인증된 이메일인지. 알 수 없으면 false (#189: 자동 연결은 인증된 이메일만)
    boolean isEmailVerified();

    static boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }
}
