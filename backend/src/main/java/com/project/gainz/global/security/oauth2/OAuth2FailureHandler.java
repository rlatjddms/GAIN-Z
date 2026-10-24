package com.project.gainz.global.security.oauth2;

import com.project.gainz.domain.user.service.SocialLoginException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

// 소셜 로그인·연결이 막히면 프론트로 돌려보낸다 (#189). 예전에는 서버의 /login?error 로 가서 빈 화면이었다.
// 로그인: /?socialError=코드&provider=KAKAO, 마이페이지 연결: /mypage?linkError=코드&provider=KAKAO
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    static final String FAILED = "FAILED";

    private final String frontendUrl;

    public OAuth2FailureHandler(@Value("${gainz.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        String code = FAILED;
        String provider = null;
        boolean link = false;
        if (exception instanceof OAuth2AuthenticationException oauth && exception.getCause() instanceof SocialLoginException social) {
            code = oauth.getError().getErrorCode();
            provider = social.getProvider() == null ? null : social.getProvider().name();
            link = social.isLinkError();
        }
        UriComponentsBuilder target = UriComponentsBuilder.fromUriString(frontendUrl + (link ? "/mypage" : "/"))
                .queryParam(link ? "linkError" : "socialError", code);
        if (provider != null) target.queryParam("provider", provider);
        getRedirectStrategy().sendRedirect(request, response, target.build().toUriString());
    }
}
