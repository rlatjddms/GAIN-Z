package com.project.gainz.domain.user.service;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

// 계정 메일 본문 (#193)
class AccountMailTemplatesTest {

    @Test
    void 인증_코드_메일에는_이미지_없는_로고와_한_번에_선택되는_코드가_있다() {
        String html = AccountMailTemplates.signupCode("012345");

        assertThat(html).contains("aria-label=\"GAIN-Z\"").doesNotContain("<img");
        // 숫자 사이에 띄어쓰기가 없어야 복사하면 코드만 붙는다
        assertThat(html).contains("user-select:all\">012345</span>");
        assertThat(html).contains("10분");
    }

    @Test
    void 재설정_메일은_이름을_이스케이프하고_버튼과_주소_둘_다_링크를_건다() {
        String link = "https://gainz.example/reset-password?token=abc";
        String html = AccountMailTemplates.passwordReset("<b>나</b>", link);

        assertThat(html).contains("&lt;b&gt;나&lt;/b&gt;님,").doesNotContain("<b>나</b>");
        assertThat(html.split("href=\"" + link.replace("?", "\\?") + "\"", -1)).hasSize(3);
        assertThat(html).contains("30분");
    }

    @Test
    void 로고는_7줄이고_줄마다_너비가_같다() {
        String logo = AccountMailTemplates.logo("#ffffff");
        // 줄마다 바깥 <tr><td> 안에 한 줄짜리 표
        String[] rows = logo.split("<tr><td style=\"padding:0\">");

        assertThat(rows).hasSize(8);
        for (int i = 1; i < rows.length; i++) {
            Matcher m = Pattern.compile("width=\"(\\d+)\"").matcher(rows[i]);
            int width = 0;
            while (m.find()) width += Integer.parseInt(m.group(1));
            // GAIN-Z 31칸 × 4px
            assertThat(width).isEqualTo(124);
        }
        assertThat(logo).contains("bgcolor=\"#ffffff\"");
    }
}
