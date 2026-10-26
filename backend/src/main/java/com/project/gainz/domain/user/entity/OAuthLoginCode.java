package com.project.gainz.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

// 소셜 로그인을 마치고 프론트로 돌아갈 때 주는 1분짜리 한 번용 코드 (#281).
// 토큰을 주소에 실으면 방문 기록·서버 기록에 남아서, 주소에는 이 코드만 싣고 프론트가 POST로 토큰과 바꾼다. id는 코드의 SHA-256
@Getter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "oauthLoginCode", timeToLive = 60)
public class OAuthLoginCode {

    @Id
    private String id;

    private String accessToken;

    private String refreshToken;
}
