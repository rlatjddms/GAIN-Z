package com.project.gainz.global.security.jwt;

import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.security.principal.CustomUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// 탈퇴한 계정의 토큰 (#279): 500이 아니라 로그인 안 한 요청으로 넘긴다 (막힌 주소면 시큐리티가 401)
class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-for-gainz-jwt-provider-0123456789";

    @AfterEach void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test void tokenOfWithdrawnUserPassesThroughUnauthenticated() throws Exception {
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail("gone@test.invalid")).thenReturn(Optional.empty());
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, new CustomUserDetailsService(users));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader("Authorization", "Bearer " + provider.createAccessToken("gone@test.invalid", "ROLE_USER"));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test void userLookupFailureIsUsernameNotFound() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail("gone@test.invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CustomUserDetailsService(users).loadUserByUsername("gone@test.invalid"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
