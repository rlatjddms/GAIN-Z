package com.project.gainz.domain.user.service;

import org.springframework.web.util.HtmlUtils;

// 계정 메일 본문 (#193): 가입 인증 코드·비밀번호 재설정. 초록 헤더(흰 픽셀 로고) + 흰 본문 + 회색 바닥글.
// 메일 앱마다 CSS 지원이 달라 표(table) 배치와 인라인 스타일만 쓴다. 색은 프론트 theme.css의 기본 테마
final class AccountMailTemplates {

    private static final String PRIMARY = "#12b886";
    private static final String PRIMARY_STRONG = "#087f5b";
    private static final String PRIMARY_TEXT = "#099268";
    private static final String PRIMARY_SOFT = "#e6fcf5";
    private static final String TEXT = "#1a1a1a";
    private static final String TEXT_2 = "#495057";
    private static final String MUTED = "#868e96";
    private static final String FAINT = "#adb5bd";
    // 프론트 pixel/logo.js와 같은 5×7 글자 (Z는 번개). 이미지는 메일 앱이 막거나 주소를 못 불러 깨질 수 있어서 표 칸으로 그린다
    private static final String[][] LOGO = {
            {"01111", "10000", "10000", "10011", "10001", "10001", "01110"},
            {"01110", "10001", "10001", "11111", "10001", "10001", "10001"},
            {"111", "010", "010", "010", "010", "010", "111"},
            {"10001", "11001", "10101", "10011", "10001", "10001", "10001"},
            {"000", "000", "000", "111", "000", "000", "000"},
            {"00011", "00110", "01100", "11111", "00110", "01100", "11000"},
    };
    private static final int LOGO_PIXEL = 4;
    private static final String FONT = "'Pretendard','Apple SD Gothic Neo','Malgun Gothic','맑은 고딕',sans-serif";

    private AccountMailTemplates() {
    }

    // 숫자 사이 간격은 CSS로만 벌려서 복사하면 숫자 6자리만 붙는다. user-select: all 이면 눌러서 통째로 선택
    static String signupCode(String code) {
        String body = """
                <h1 style="margin:0 0 8px;font-size:20px;line-height:1.4;font-weight:700;color:%s">가입 인증 코드</h1>
                <p style="margin:0 0 22px;font-size:14px;line-height:1.7;color:%s">회원가입 화면에 아래 코드를 입력해 주세요.</p>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                  <tr><td align="center" bgcolor="%s" style="padding:20px 0;background:%s;border-radius:10px;font-family:'SFMono-Regular',Menlo,Consolas,'Courier New',monospace;font-size:32px;line-height:1.2;font-weight:700;letter-spacing:10px;color:%s"><span style="padding-left:10px;-webkit-user-select:all;user-select:all">%s</span></td></tr>
                </table>
                <p style="margin:18px 0 0;font-size:13px;line-height:1.7;color:%s">10분 동안 유효해요. 직접 요청하지 않았다면 무시해도 돼요.</p>
                """.formatted(TEXT, TEXT_2, PRIMARY_SOFT, PRIMARY_SOFT, PRIMARY_STRONG, code, MUTED);
        return layout("GAIN-Z 인증 코드 " + code + " · 10분 동안 유효해요", body);
    }

    static String passwordReset(String name, String link) {
        String who = name == null || name.isBlank() ? "" : HtmlUtils.htmlEscape(name) + "님, ";
        String body = """
                <h1 style="margin:0 0 8px;font-size:20px;line-height:1.4;font-weight:700;color:%s">비밀번호 재설정</h1>
                <p style="margin:0 0 24px;font-size:14px;line-height:1.7;color:%s">%s아래 버튼을 눌러 새 비밀번호를 정해 주세요.</p>
                <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                  <tr><td align="center" bgcolor="%s" style="border-radius:10px;background:%s">
                    <a href="%s" target="_blank" style="display:inline-block;padding:14px 28px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;border-radius:10px">새 비밀번호 정하기</a>
                  </td></tr>
                </table>
                <p style="margin:22px 0 0;font-size:13px;line-height:1.7;color:%s">링크는 30분 동안 한 번만 쓸 수 있어요. 직접 요청하지 않았다면 무시해도 돼요. 비밀번호는 바뀌지 않아요.</p>
                <p style="margin:16px 0 0;font-size:12px;line-height:1.6;color:%s;word-break:break-all">버튼이 안 눌리면 이 주소를 브라우저에 붙여 넣어 주세요.<br><a href="%s" target="_blank" style="color:%s">%s</a></p>
                """.formatted(TEXT, TEXT_2, who, PRIMARY, PRIMARY, link, MUTED, FAINT, link, PRIMARY_TEXT, link);
        return layout("비밀번호 재설정 링크 · 30분 동안 유효해요", body);
    }

    // 줄마다 같은 색이 이어지는 칸은 하나로 합쳐 칸 수를 줄인다. 줄마다 칸 경계가 달라서 한 줄을 표 하나로 따로 둔다
    static String logo(String color) {
        StringBuilder rows = new StringBuilder();
        for (int y = 0; y < 7; y++) {
            StringBuilder bits = new StringBuilder();
            for (int g = 0; g < LOGO.length; g++) {
                if (g > 0) bits.append('0');
                bits.append(LOGO[g][y]);
            }
            rows.append("<tr><td style=\"padding:0\"><table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\"><tr>");
            int x = 0;
            while (x < bits.length()) {
                char bit = bits.charAt(x);
                int run = 1;
                while (x + run < bits.length() && bits.charAt(x + run) == bit) run++;
                int w = run * LOGO_PIXEL;
                String fill = bit == '1' ? " bgcolor=\"%s\"".formatted(color) : "";
                String bg = bit == '1' ? "background:%s;".formatted(color) : "";
                rows.append("<td width=\"%d\" height=\"%d\"%s style=\"width:%dpx;height:%dpx;padding:0;%sfont-size:0;line-height:0;mso-line-height-rule:exactly\"></td>"
                        .formatted(w, LOGO_PIXEL, fill, w, LOGO_PIXEL, bg));
                x += run;
            }
            rows.append("</tr></table></td></tr>");
        }
        return "<table role=\"img\" aria-label=\"GAIN-Z\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">"
                + rows + "</table>";
    }

    // 흰 바탕 → 테두리 카드: 초록 헤더(흰 로고) → 흰 본문 → 회색 바닥글. preheader는 받은편지함 목록에 제목 옆으로 보이는 한 줄
    private static String layout(String preheader, String body) {
        return """
                <!doctype html>
                <html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="color-scheme" content="light"><title>GAIN-Z</title></head>
                <body style="margin:0;padding:0;background:#ffffff">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent">%s</div>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#ffffff;font-family:%s">
                  <tr><td align="center" style="padding:24px 12px">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width:480px;background:#ffffff;border:1px solid #e9ecef;border-radius:12px;border-collapse:separate">
                      <tr><td bgcolor="%s" style="padding:22px 28px;background:%s;border-radius:12px 12px 0 0">
                        %s
                      </td></tr>
                      <tr><td style="padding:30px 28px 28px">
                %s
                      </td></tr>
                      <tr><td bgcolor="#f8f9fa" style="padding:14px 28px;background:#f8f9fa;border-radius:0 0 12px 12px;font-size:12px;line-height:1.6;color:%s">
                        보내기 전용 메일이라 답장을 받을 수 없어요.
                      </td></tr>
                    </table>
                  </td></tr>
                </table>
                </body></html>
                """.formatted(preheader, FONT, PRIMARY, PRIMARY, logo("#ffffff"), body, FAINT);
    }
}
