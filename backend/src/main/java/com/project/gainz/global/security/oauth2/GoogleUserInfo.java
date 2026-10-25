package com.project.gainz.global.security.oauth2;

import java.util.Map;

public record GoogleUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public String getProvider() { return "google"; }

    @Override
    public String getProviderId() { return String.valueOf(attributes.get("sub")); }

    @Override
    public String getEmail(){ return (String) attributes.get("email"); }

    @Override
    public boolean isEmailVerified() { return OAuth2UserInfo.isTrue(attributes.get("email_verified")); }
}
