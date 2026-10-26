package com.project.gainz.global.security.jwt;

import com.project.gainz.global.security.principal.CustomUserDetailsService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// 토큰 종류 구분 (#277): 로그인 유지 토큰으로는 API를 부를 수 없고, 접근 토큰으로는 재발급할 수 없다
class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-for-gainz-jwt-provider-0123456789";
    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, mock(CustomUserDetailsService.class));

    @Test void accessTokenIsOnlyForRequestsAndRefreshTokenOnlyForReissue() {
        String access = provider.createAccessToken("me@test.invalid", "ROLE_USER");
        String refresh = provider.createRefreshToken("me@test.invalid");

        assertThat(provider.validateAccessToken(access)).isTrue();
        assertThat(provider.validateRefreshToken(access)).isFalse();
        assertThat(provider.validateRefreshToken(refresh)).isTrue();
        assertThat(provider.validateAccessToken(refresh)).isFalse();
    }

    // 종류 칸이 생기기 전에 발급된 토큰: role이 있으면 접근 토큰, 없으면 로그인 유지 토큰으로 본다
    @Test void tokensIssuedBeforeTypeClaimAreToldApartByRole() {
        String legacyAccess = legacy("ROLE_USER", 60_000);
        String legacyRefresh = legacy(null, 60_000);

        assertThat(provider.validateAccessToken(legacyAccess)).isTrue();
        assertThat(provider.validateRefreshToken(legacyAccess)).isFalse();
        assertThat(provider.validateAccessToken(legacyRefresh)).isFalse();
        assertThat(provider.validateRefreshToken(legacyRefresh)).isTrue();
    }

    @Test void expiredOrForgedOrBrokenTokensAreRejected() {
        String expired = legacy("ROLE_USER", -1_000);
        String forged = Jwts.builder().setSubject("me@test.invalid").claim("typ", "access")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("another-secret-key-for-signing-tokens-9876543210".getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertThat(provider.validateAccessToken(expired)).isFalse();
        assertThat(provider.validateAccessToken(forged)).isFalse();
        assertThat(provider.validateAccessToken("not-a-token")).isFalse();
        assertThat(provider.validateRefreshToken("not-a-token")).isFalse();
    }

    private String legacy(String role, long validForMs) {
        var builder = Jwts.builder().setSubject("me@test.invalid").setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + validForMs))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256);
        if (role != null) builder.claim("role", role);
        return builder.compact();
    }
}
