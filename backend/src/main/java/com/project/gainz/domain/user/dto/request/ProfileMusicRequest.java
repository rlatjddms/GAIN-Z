package com.project.gainz.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 요즘 듣는 음악 설정. 링크는 선택 (http/https만)
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
        String url
) {
}
