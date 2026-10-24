package com.project.gainz.global.security.oauth2;

import java.util.Map;

public record KakaoUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public String getProvider() { return "kakao"; }

    @Override
    public String getProviderId() { return String.valueOf(attributes.get("id")); }

    @Override
    public String getEmail() {
        return (String) account().get("email");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getName() {
        Object profile = attributes.get("properties");
        return profile instanceof Map<?, ?> map ? (String) ((Map<String, Object>) map).get("nickname") : null;
    }

    // 카카오는 인증된 이메일이면서 지금도 쓸 수 있는 이메일일 때만
    @Override
    public boolean isEmailVerified() {
        Map<String, Object> account = account();
        return OAuth2UserInfo.isTrue(account.get("is_email_verified")) && OAuth2UserInfo.isTrue(account.get("is_email_valid"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> account() {
        Object account = attributes.get("kakao_account");
        return account instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
