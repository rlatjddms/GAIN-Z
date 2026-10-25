package com.project.gainz.domain.user.service;

import com.project.gainz.domain.user.enums.SocialProvider;

// 소셜 로그인에서 받은 정보 (#189). emailVerified: 소셜 쪽에서 인증된 이메일인지 (모르면 false)
public record SocialProfile(SocialProvider provider, String providerId, String email, boolean emailVerified) {
}
