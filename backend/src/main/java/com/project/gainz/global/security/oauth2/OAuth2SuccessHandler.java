package com.project.gainz.global.security.oauth2;

import com.project.gainz.domain.user.entity.RefreshToken;
import com.project.gainz.domain.user.repository.RefreshTokenRepository;
import com.project.gainz.global.security.jwt.JwtTokenProvider;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String email = principal.getUsername();
        String role = principal.getAuthorities().iterator().next().getAuthority();

        String accessToken = jwtTokenProvider.createAccessToken(email, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);

        // 일반 로그인처럼 재발급용 토큰을 저장해야 접속 토큰이 만료돼도 재발급된다 (#144)
        refreshTokenRepository.save(new RefreshToken(email, refreshToken));

        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth2/redirect")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
