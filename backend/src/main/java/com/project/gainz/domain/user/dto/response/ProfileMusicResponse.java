package com.project.gainz.domain.user.dto.response;

import com.project.gainz.domain.user.entity.User;

// 요즘 듣는 음악. 링크가 없으면 url은 null
public record ProfileMusicResponse(
        String title,
        String artist,
        String url
) {
    // 설정하지 않았으면 null
    public static ProfileMusicResponse from(User user) {
        if (!user.hasProfileMusic()) return null;
        return new ProfileMusicResponse(user.getProfileMusicTitle(), user.getProfileMusicArtist(), user.getProfileMusicUrl());
    }
}
