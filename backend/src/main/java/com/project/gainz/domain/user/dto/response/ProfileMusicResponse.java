package com.project.gainz.domain.user.dto.response;

import com.project.gainz.domain.user.entity.User;

// 요즘 듣는 음악. 링크가 없으면 url은 null. 커버·미리듣기는 곡 검색으로 골랐을 때만 (#187)
public record ProfileMusicResponse(
        String title,
        String artist,
        String url,
        String artworkUrl,
        String previewUrl
) {
    public ProfileMusicResponse(String title, String artist, String url) {
        this(title, artist, url, null, null);
    }

    // 설정하지 않았으면 null
    public static ProfileMusicResponse from(User user) {
        if (!user.hasProfileMusic()) return null;
        return new ProfileMusicResponse(user.getProfileMusicTitle(), user.getProfileMusicArtist(), user.getProfileMusicUrl(),
                user.getProfileMusicArtworkUrl(), user.getProfileMusicPreviewUrl());
    }
}
