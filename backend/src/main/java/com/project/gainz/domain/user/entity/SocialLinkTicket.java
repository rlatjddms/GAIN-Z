package com.project.gainz.domain.user.entity;

import com.project.gainz.domain.user.enums.SocialProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

// 마이페이지에서 소셜 연결을 시작할 때 주는 5분짜리 한 번용 표 (#189).
// 소셜 로그인은 브라우저가 서버로 바로 가서 로그인 토큰을 실을 수 없으니, 이 표로 "누가 연결하는지"를 알린다. id는 표의 SHA-256
@Getter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "socialLinkTicket", timeToLive = 300)
public class SocialLinkTicket {

    @Id
    private String id;

    private Long userId;

    private SocialProvider provider;
}
