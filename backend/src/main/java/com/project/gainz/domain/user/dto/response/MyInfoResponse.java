package com.project.gainz.domain.user.dto.response;

import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.ProfileVisibility;
import com.project.gainz.domain.user.enums.SocialProvider;

import java.time.LocalDateTime;
import java.util.List;

// 내 계정 정보 (마이페이지). 프로필 사진이 없으면 profileImageUrl은 null, 음악을 안 정했으면 profileMusic은 null
public record MyInfoResponse(
        Long id,
        String email,
        String name,
        SocialProvider provider,
        LocalDateTime createdAt,
        String profileImageUrl,
        ProfileMusicResponse profileMusic,
        ProfileVisibility profileVisibility,
        // 이메일·비밀번호로 로그인할 수 있는지, 연결된 소셜 (#189)
        boolean hasPassword,
        List<SocialProvider> socialLinks
) {
    public static MyInfoResponse from(User user) {
        return new MyInfoResponse(user.getId(), user.getEmail(), user.getName(), user.getProvider(),
                user.getCreatedAt(), profileImageUrl(user), ProfileMusicResponse.from(user), user.getProfileVisibility(),
                user.hasPassword(), user.linkedProviders());
    }

    // 사진을 바꾸면 주소도 바뀌게 파일 이름(UUID)을 붙인다 (브라우저 캐시 무효화)
    public static String profileImageUrl(User user) {
        if (!user.hasProfileImage()) return null;
        String version = user.getProfileImage().replaceFirst("\\.[^.]+$", "");
        return "/api/v1/users/" + user.getId() + "/profile-image?v=" + version;
    }
}
