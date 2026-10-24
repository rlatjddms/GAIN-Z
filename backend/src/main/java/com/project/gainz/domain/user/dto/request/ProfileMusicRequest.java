package com.project.gainz.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 요즘 듣는 음악 설정. 링크는 선택 (http/https만).
// 커버·미리듣기는 곡 검색(iTunes)으로 골랐을 때만 오고, Apple 주소만 받는다 (#187). 아무 주소나 이미지·소리로 띄우지 않게
public record ProfileMusicRequest(

        @NotBlank(message = "곡 제목을 입력해 주세요.")
        @Size(max = 100, message = "곡 제목은 100자 이하로 입력해 주세요.")
        String title,

        @NotBlank(message = "아티스트를 입력해 주세요.")
        @Size(max = 100, message = "아티스트는 100자 이하로 입력해 주세요.")
        String artist,

        @Size(max = 500, message = "링크는 500자 이하로 입력해 주세요.")
        @Pattern(regexp = "^\\s*$|^https?://\\S+$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "링크는 http:// 또는 https://로 시작해야 합니다.")
        String url,

        @Size(max = 500, message = "커버 주소가 너무 길어요.")
        @Pattern(regexp = "^\\s*$|^https://is\\d+-ssl\\.mzstatic\\.com/\\S+$", message = "검색에서 고른 곡의 커버만 넣을 수 있어요.")
        String artworkUrl,

        @Size(max = 500, message = "미리듣기 주소가 너무 길어요.")
        @Pattern(regexp = "^\\s*$|^https://audio-ssl\\.itunes\\.apple\\.com/\\S+$", message = "검색에서 고른 곡의 미리듣기만 넣을 수 있어요.")
        String previewUrl
) {
    public ProfileMusicRequest(String title, String artist, String url) {
        this(title, artist, url, null, null);
    }
}
