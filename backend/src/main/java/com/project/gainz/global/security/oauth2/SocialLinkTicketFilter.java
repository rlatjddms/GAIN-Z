package com.project.gainz.global.security.oauth2;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 소셜 로그인 시작(/oauth2/authorization/{provider})에 붙은 연결 표(?link=)를 세션에 둔다 (#189).
// 소셜에서 돌아오는 요청(CustomOAuth2UserService)이 같은 세션에서 꺼내 "로그인한 계정에 연결"로 처리한다.
// 표 없이 시작하면 지운다 (연결하다 그만두고 그냥 로그인하는 경우)
public class SocialLinkTicketFilter extends OncePerRequestFilter {

    public static final String SESSION_KEY = "gainz.socialLinkTicket";
    private static final String START_PATH = "/oauth2/authorization/";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(START_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ticket = request.getParameter("link");
        if (ticket != null && !ticket.isBlank()) {
            request.getSession(true).setAttribute(SESSION_KEY, ticket);
        } else if (request.getSession(false) != null) {
            request.getSession(false).removeAttribute(SESSION_KEY);
        }
        chain.doFilter(request, response);
    }
}
