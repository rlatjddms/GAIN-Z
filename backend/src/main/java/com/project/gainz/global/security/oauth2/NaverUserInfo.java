package com.project.gainz.global.security.oauth2;

import java.util.Map;

public record NaverUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public String getProvider() { return "naver"; }

    @Override
    public String getProviderId() { return String.valueOf(response().get("id")); }

    @Override
    public String getEmail() {
        return (String) response().get("email");
    }

    @Override
    public String getName() {
        return (String) response().get("name");
    }

    // 네이버는 이메일 인증 여부를 주지 않는다 → 자동 연결하지 않고 마이페이지에서 연결 (#189)
    @Override
    public boolean isEmailVerified() { return false; }

    @SuppressWarnings("unchecked")
    private Map<String, Object> response() {
        Object response = attributes.get("response");
        return response instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
