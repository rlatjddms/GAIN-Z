package com.project.gainz.global.security.oauth2;

import com.project.gainz.domain.user.entity.RefreshToken;
import com.project.gainz.domain.user.repository.RefreshTokenRepository;
import com.project.gainz.global.security.jwt.JwtTokenProvider;
import com.project.gainz.global.security.principal.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private OAuth2SuccessHandler handler;

    @Test
    void 간편로그인에_성공하면_재발급_토큰을_저장하고_토큰을_넘긴다() throws Exception {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getUsername()).thenReturn("kakao@example.com");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(principal).getAuthorities();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtTokenProvider.createAccessToken("kakao@example.com", "ROLE_USER")).thenReturn("access");
        when(jwtTokenProvider.createRefreshToken("kakao@example.com")).thenReturn("refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        // 일반 로그인과 같이 저장해야 재발급(reissue)에서 찾을 수 있다
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("kakao@example.com");
        assertThat(saved.getValue().getToken()).isEqualTo("refresh");
        assertThat(response.getRedirectedUrl()).contains("accessToken=access").contains("refreshToken=refresh");
    }
}
