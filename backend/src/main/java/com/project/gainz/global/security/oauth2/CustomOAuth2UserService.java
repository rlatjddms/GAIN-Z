package com.project.gainz.global.security.oauth2;

import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.service.SocialAccountService;
import com.project.gainz.domain.user.service.SocialLoginException;
import com.project.gainz.domain.user.service.SocialProfile;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

// 소셜 로그인에서 돌아왔을 때 (#189): 마이페이지에서 시작한 연결이면 그 계정에 붙이고, 아니면 로그인·가입.
// 규칙은 SocialAccountService. 막히면 OAuth2FailureHandler가 프론트로 돌려보낸다
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    // 연결이면 OAuth2SuccessHandler가 ?linked=KAKAO 를 붙인다
    public static final String LINKED_PROVIDER = "gainz.linkedProvider";

    private final SocialAccountService socialAccountService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        return load(registrationId, oAuth2User.getAttributes(), currentRequest());
    }

    OAuth2User load(String registrationId, Map<String, Object> attributes, HttpServletRequest request) {
        OAuth2UserInfo userInfo = switch (registrationId) {
            case "google" -> new GoogleUserInfo(attributes);
            case "kakao" -> new KakaoUserInfo(attributes);
            case "naver" -> new NaverUserInfo(attributes);
            default -> throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2FailureHandler.FAILED), "지원하지 않는 소셜 로그인입니다.");
        };
        SocialProfile profile = new SocialProfile(SocialProvider.from(userInfo.getProvider()), userInfo.getProviderId(),
                userInfo.getEmail(), userInfo.getName(), userInfo.isEmailVerified());

        String ticket = takeLinkTicket(request);
        try {
            User user;
            if (ticket != null) {
                user = socialAccountService.link(ticket, profile);
                if (request != null) request.setAttribute(LINKED_PROVIDER, profile.provider().name());
            } else {
                user = socialAccountService.login(profile);
            }
            return UserPrincipal.create(user, attributes);
        } catch (SocialLoginException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error(e.getCode()), e.getCode(), e);
        }
    }

    // 한 번 꺼내면 지운다
    private static String takeLinkTicket(HttpServletRequest request) {
        HttpSession session = request == null ? null : request.getSession(false);
        if (session == null) return null;
        Object ticket = session.getAttribute(SocialLinkTicketFilter.SESSION_KEY);
        session.removeAttribute(SocialLinkTicketFilter.SESSION_KEY);
        return ticket instanceof String s ? s : null;
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs ? attrs.getRequest() : null;
    }
}
