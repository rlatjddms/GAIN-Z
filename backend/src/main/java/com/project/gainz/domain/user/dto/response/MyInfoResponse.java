package com.project.gainz.domain.user.dto.response;

import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;

import java.time.LocalDateTime;

// 내 계정 정보 (마이페이지). 프로필 사진이 없으면 profileImageUrl은 null
public record MyInfoResponse(
        Long id,
        String email,
        String name,
        SocialProvider provider,
        LocalDateTime createdAt,
        String profileImageUrl
) {
    public static MyInfoResponse from(User user) {
        return new MyInfoResponse(user.getId(), user.getEmail(), user.getName(), user.getProvider(),
                user.getCreatedAt(), profileImageUrl(user));
    }

    // 사진을 바꾸면 주소도 바뀌게 파일 이름(UUID)을 붙인다 (브라우저 캐시 무효화)
    private static String profileImageUrl(User user) {
        if (!user.hasProfileImage()) return null;
        String version = user.getProfileImage().replaceFirst("\\.[^.]+$", "");
        return "/api/v1/users/" + user.getId() + "/profile-image?v=" + version;
    }
}
