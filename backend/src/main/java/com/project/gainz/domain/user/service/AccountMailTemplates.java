package com.project.gainz.domain.user.service;

import org.springframework.web.util.HtmlUtils;

// 계정 메일 본문 (#193): 가입 인증 코드·비밀번호 재설정.
// 메일 앱마다 CSS 지원이 달라 표(table) 배치와 인라인 스타일만 쓴다. 색은 프론트 theme.css의 기본 테마
final class AccountMailTemplates {

    private static final String PRIMARY = "#12b886";
    private static final String PRIMARY_STRONG = "#087f5b";
    private static final String PRIMARY_TEXT = "#099268";
    private static final String PRIMARY_SOFT = "#e6fcf5";
    private static final String PRIMARY_LINE = "#96f2d7";
    private static final String TEXT = "#1a1a1a";
    private static final String TEXT_2 = "#495057";
    private static final String MUTED = "#868e96";
    private static final String FAINT = "#adb5bd";
    private static final String FONT = "'Pretendard','Apple SD Gothic Neo','Malgun Gothic','맑은 고딕',sans-serif";

    private AccountMailTemplates() {
    }

    // 6자리 코드는 글자 사이 간격을 CSS로만 벌려서 복사하면 숫자만 붙는다. user-select: all 이면 한 번 눌러 통째로 선택
    static String signupCode(String frontendUrl, String code) {
        String body = """
                %s
                <p style="margin:0 0 28px;font-size:15px;line-height:1.7;color:%s">GAIN-Z 회원가입 화면에 아래 코드를 넣어 주세요.</p>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                  <tr><td align="center" style="padding:26px 16px 22px;background:%s;border:2px dashed %s;border-radius:16px">
                    <div style="font-size:12px;font-weight:700;letter-spacing:1px;color:%s;margin:0 0 10px">인증 코드</div>
                    <div style="font-family:'SFMono-Regular',Menlo,Consolas,'Courier New',monospace;font-size:38px;line-height:1.1;font-weight:800;letter-spacing:10px;padding-left:10px;color:%s;-webkit-user-select:all;user-select:all">%s</div>
                    <div style="font-size:12px;line-height:1.6;color:%s;margin:12px 0 0">코드를 길게 누르거나 두 번 눌러 복사할 수 있어요</div>
                  </td></tr>
                </table>
                %s
                """.formatted(title("이메일 인증", "인증 코드를 보내 드려요"), TEXT_2,
                PRIMARY_SOFT, PRIMARY_LINE, PRIMARY_TEXT, PRIMARY_STRONG, code, MUTED,
                notice("코드는 <strong style=\"color:%s\">10분</strong> 동안 쓸 수 있어요. 5번 틀리면 가입 화면에서 새 코드를 받아 주세요.".formatted(TEXT),
                        "직접 요청하지 않았다면 이 메일은 무시해 주세요. 누군가 이메일 주소를 잘못 입력했을 수 있어요."));
        return layout(frontendUrl, "GAIN-Z 인증 코드 " + code + " · 10분 동안 쓸 수 있어요", body);
    }

    static String passwordReset(String frontendUrl, String name, String link) {
        String who = name == null || name.isBlank() ? "" : HtmlUtils.htmlEscape(name) + "님, ";
        String body = """
                %s
                <p style="margin:0 0 28px;font-size:15px;line-height:1.7;color:%s">%s비밀번호 재설정을 요청하셨어요.<br>아래 버튼을 눌러 새 비밀번호를 정해 주세요.</p>
                <table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center">
                  <tr><td align="center" bgcolor="%s" style="border-radius:12px;background:%s">
                    <a href="%s" target="_blank" style="display:inline-block;padding:15px 34px;font-size:16px;font-weight:800;color:#ffffff;text-decoration:none;border-radius:12px">새 비밀번호 정하기</a>
                  </td></tr>
                </table>
                %s
                <p style="margin:24px 0 0;font-size:12px;line-height:1.6;color:%s;word-break:break-all">버튼이 안 눌리면 이 주소를 브라우저에 붙여 넣어 주세요.<br><a href="%s" target="_blank" style="color:%s;text-decoration:underline">%s</a></p>
                """.formatted(title("비밀번호 찾기", "새 비밀번호를 정해 주세요"), TEXT_2, who,
                PRIMARY, PRIMARY, link,
                notice("링크는 <strong style=\"color:%s\">30분</strong> 동안 한 번만 쓸 수 있어요.".formatted(TEXT),
                        "직접 요청하지 않았다면 이 메일은 무시해 주세요. 비밀번호는 바뀌지 않아요."),
                FAINT, link, PRIMARY_TEXT, link);
        return layout(frontendUrl, "비밀번호 재설정 링크 · 30분 동안 쓸 수 있어요", body);
    }

    // 작은 꼬리표 + 제목
    private static String title(String tag, String title) {
        return """
                <div style="display:inline-block;margin:0 0 14px;padding:5px 11px;border-radius:999px;background:%s;font-size:12px;font-weight:700;color:%s">%s</div>
                <h1 style="margin:0 0 12px;font-size:23px;line-height:1.4;font-weight:800;color:%s">%s</h1>
                """.formatted(PRIMARY_SOFT, PRIMARY_TEXT, tag, TEXT, title);
    }

    // 유효 시간·주의 안내
    private static String notice(String main, String sub) {
        return """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin:26px 0 0">
                  <tr><td style="padding:14px 16px;background:#f8f9fa;border-radius:12px;font-size:13px;line-height:1.65;color:%s">
                    %s<br><span style="color:%s">%s</span>
                  </td></tr>
                </table>
                """.formatted(TEXT_2, main, MUTED, sub);
    }

    // 바탕 → 로고 → 흰 카드 → 바닥글. preheader는 받은편지함 목록에 제목 옆으로 보이는 한 줄
    private static String layout(String frontendUrl, String preheader, String body) {
        return """
                <!doctype html>
                <html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="color-scheme" content="light"><title>GAIN-Z</title></head>
                <body style="margin:0;padding:0;background:#f1f3f5">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent">%s</div>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f1f3f5;font-family:%s">
                  <tr><td align="center" style="padding:36px 16px">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width:520px">
                      <tr><td align="center" style="padding:0 0 22px">
                        <a href="%s" target="_blank" style="text-decoration:none"><img src="%s/mail/logo.png" width="155" height="35" alt="GAIN-Z" style="display:block;border:0;width:155px;height:35px;font-size:24px;font-weight:900;color:%s;font-family:%s"></a>
                      </td></tr>
                      <tr><td style="padding:36px 32px;background:#ffffff;border:1px solid #e9ecef;border-radius:20px">
                %s
                      </td></tr>
                      <tr><td align="center" style="padding:22px 12px 0;font-size:12px;line-height:1.7;color:%s">
                        이 메일은 보내기 전용이라 답장을 받을 수 없어요.<br>
                        <a href="%s" target="_blank" style="color:%s;text-decoration:none;font-weight:700">GAIN-Z</a> · 악기 연습 앱
                      </td></tr>
                    </table>
                  </td></tr>
                </table>
                </body></html>
                """.formatted(preheader, FONT, frontendUrl, frontendUrl, PRIMARY, FONT, body, FAINT, frontendUrl, MUTED);
    }
}
