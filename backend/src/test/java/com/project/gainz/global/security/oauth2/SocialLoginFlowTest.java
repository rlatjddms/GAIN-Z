package com.project.gainz.global.security.oauth2;

import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.domain.user.service.SocialAccountService;
import com.project.gainz.domain.user.service.SocialLoginException;
import com.project.gainz.domain.user.service.SocialProfile;
import com.project.gainz.global.security.principal.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 소셜 로그인·연결의 요청 흐름 (#189): 연결 표 → 세션 → 돌아온 요청에서 연결, 막히면 프론트로
@ExtendWith(MockitoExtension.class)
class SocialLoginFlowTest {

    @Mock private SocialAccountService socialAccountService;
    @InjectMocks private CustomOAuth2UserService userService;

    private static final Map<String, Object> GOOGLE = Map.of("sub", "g-1", "email", "me@test.invalid", "name", "나", "email_verified", true);

    private static User user() {
        return User.builder().email("me@test.invalid").password("encoded")
                .provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build();
    }

    @Test
    void 연결_표는_시작할_때_세션에_두고_표_없이_시작하면_지운다() throws Exception {
        SocialLinkTicketFilter filter = new SocialLinkTicketFilter();
        MockHttpSession session = new MockHttpSession();

        MockHttpServletRequest start = new MockHttpServletRequest("GET", "/oauth2/authorization/kakao");
        start.setSession(session);
        start.setParameter("link", "ticket");
        filter.doFilter(start, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(session.getAttribute(SocialLinkTicketFilter.SESSION_KEY)).isEqualTo("ticket");

        MockHttpServletRequest plain = new MockHttpServletRequest("GET", "/oauth2/authorization/kakao");
        plain.setSession(session);
        filter.doFilter(plain, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(session.getAttribute(SocialLinkTicketFilter.SESSION_KEY)).isNull();
    }

    @Test
    void 세션에_연결_표가_있으면_연결하고_한_번_쓰면_지운다() {
        MockHttpServletRequest callback = new MockHttpServletRequest();
        callback.getSession(true).setAttribute(SocialLinkTicketFilter.SESSION_KEY, "ticket");
        when(socialAccountService.link(anyString(), any())).thenReturn(user());

        UserPrincipal principal = (UserPrincipal) userService.load("google", GOOGLE, callback);

        ArgumentCaptor<SocialProfile> profile = ArgumentCaptor.forClass(SocialProfile.class);
        verify(socialAccountService).link(org.mockito.ArgumentMatchers.eq("ticket"), profile.capture());
        assertThat(profile.getValue()).isEqualTo(new SocialProfile(SocialProvider.GOOGLE, "g-1", "me@test.invalid", true));
        assertThat(principal.getUsername()).isEqualTo("me@test.invalid");
        assertThat(callback.getAttribute(CustomOAuth2UserService.LINKED_PROVIDER)).isEqualTo("GOOGLE");
        assertThat(callback.getSession().getAttribute(SocialLinkTicketFilter.SESSION_KEY)).isNull();
        verify(socialAccountService, never()).login(any());
    }

    @Test
    void 막히면_코드를_담아_프론트로_돌려보낸다() throws Exception {
        when(socialAccountService.login(any()))
                .thenThrow(new SocialLoginException(SocialLoginException.EMAIL_EXISTS, SocialProvider.NAVER));
        Map<String, Object> naver = Map.of("response", Map.of("id", "n-1", "email", "me@test.invalid", "name", "나"));
        MockHttpServletRequest callback = new MockHttpServletRequest();

        OAuth2AuthenticationException error = (OAuth2AuthenticationException) org.assertj.core.api.Assertions
                .catchThrowable(() -> userService.load("naver", naver, callback));
        assertThat(error.getError().getErrorCode()).isEqualTo("EMAIL_EXISTS");

        OAuth2FailureHandler failure = new OAuth2FailureHandler("https://gainz.example/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        failure.onAuthenticationFailure(callback, response, error);
        assertThat(response.getRedirectedUrl()).isEqualTo("https://gainz.example/?socialError=EMAIL_EXISTS&provider=NAVER");
    }

    @Test
    void 연결_중에_막히면_마이페이지로_그_밖의_오류는_FAILED로() throws Exception {
        OAuth2FailureHandler failure = new OAuth2FailureHandler("https://gainz.example");
        SocialLoginException taken = new SocialLoginException(SocialLoginException.LINK_TAKEN, SocialProvider.KAKAO);

        MockHttpServletResponse linked = new MockHttpServletResponse();
        failure.onAuthenticationFailure(new MockHttpServletRequest(), linked,
                new OAuth2AuthenticationException(new OAuth2Error(taken.getCode()), taken.getCode(), taken));
        assertThat(linked.getRedirectedUrl()).isEqualTo("https://gainz.example/mypage?linkError=LINK_TAKEN&provider=KAKAO");

        MockHttpServletResponse other = new MockHttpServletResponse();
        failure.onAuthenticationFailure(new MockHttpServletRequest(), other, new OAuth2AuthenticationException("invalid_token_response"));
        assertThat(other.getRedirectedUrl()).isEqualTo("https://gainz.example/?socialError=FAILED");
    }

    @Test
    void 소셜마다_고유_ID와_이메일_인증_여부를_읽는다() {
        assertThat(new GoogleUserInfo(GOOGLE).isEmailVerified()).isTrue();
        assertThat(new GoogleUserInfo(Map.of("sub", "g-1")).isEmailVerified()).isFalse();

        Map<String, Object> kakao = Map.of("id", 123L, "kakao_account",
                Map.of("email", "me@test.invalid", "is_email_verified", true, "is_email_valid", false));
        assertThat(new KakaoUserInfo(kakao).getProviderId()).isEqualTo("123");
        // 인증됐어도 지금 쓸 수 없는 이메일이면 인증되지 않은 것으로
        assertThat(new KakaoUserInfo(kakao).isEmailVerified()).isFalse();

        // 네이버는 인증 여부를 주지 않는다
        assertThat(new NaverUserInfo(Map.of("response", Map.of("id", "n-1"))).isEmailVerified()).isFalse();
        assertThatThrownBy(() -> userService.load("apple", Map.of(), null)).isInstanceOf(OAuth2AuthenticationException.class);
    }
}
