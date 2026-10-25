package com.project.gainz.global.security.jwt;

import com.project.gainz.global.security.principal.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // 토큰 종류 (#277). 로그인 유지 토큰(refresh)을 접근 토큰처럼 쓰지 못하게 구분한다
    static final String TYPE_CLAIM = "typ";
    static final String ACCESS = "access";
    static final String REFRESH = "refresh";

    private final SecretKey key;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            CustomUserDetailsService customUserDetailsService) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.customUserDetailsService = customUserDetailsService;
    }

    // Access 토큰 생성
    public String createAccessToken(String email, String role) {
        long accessTokenValidityTime = 1000L * 60 * 30;  // 30분
        return createToken(email, role, ACCESS, accessTokenValidityTime);
    }

    // Refresh 토큰 생성
    public String createRefreshToken(String email) {
        long refreshTokenValidityTime = 1000L * 60 * 60 * 24 * 14;  // 14일
        return createToken(email, null, REFRESH, refreshTokenValidityTime);
    }

    // 공통 토큰 생성 메서드
    private String createToken(String email, String role, String type, long validityTime) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityTime);

        var builder = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(validity)
                .claim(TYPE_CLAIM, type)
                .signWith(key, SignatureAlgorithm.HS256);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    // 서명·만료가 맞는 접근 토큰인지 (API 요청 인증용)
    public boolean validateAccessToken(String token) {
        Claims claims = parse(token);
        return claims != null && ACCESS.equals(typeOf(claims));
    }

    // 서명·만료가 맞는 로그인 유지 토큰인지 (재발급용)
    public boolean validateRefreshToken(String token) {
        Claims claims = parse(token);
        return claims != null && REFRESH.equals(typeOf(claims));
    }

    private Claims parse(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (Exception e) {
            return null;
        }
    }

    // 종류 칸이 생기기 전(#277)에 발급된 토큰은 role이 있으면 접근 토큰, 없으면 로그인 유지 토큰이었다
    private static String typeOf(Claims claims) {
        String type = claims.get(TYPE_CLAIM, String.class);
        if (type != null) return type;
        return claims.get("role") != null ? ACCESS : REFRESH;
    }

    // 토큰에서 인증 정보 가져오기
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String email = claims.getSubject();
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }
}
