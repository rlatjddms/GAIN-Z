package com.project.gainz.domain.user.service;

import com.project.gainz.domain.user.dto.response.MyInfoResponse;
import com.project.gainz.domain.user.entity.SocialAccount;
import com.project.gainz.domain.user.entity.SocialLinkTicket;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.domain.user.repository.SocialAccountRepository;
import com.project.gainz.domain.user.repository.SocialLinkTicketRepository;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

// 이메일·소셜 로그인 계정 연동 (#189).
// 소셜 로그인: 연결된 소셜 고유 ID로 먼저 찾고, 없으면 같은 이메일 계정에 "양쪽 다 인증된 이메일일 때만" 자동 연결.
// 그 밖에는 막고 원래 방법으로 로그인한 뒤 마이페이지에서 연결하게 한다 (남의 이메일로 만든 소셜 계정으로 들어오는 것을 막는다)
@Service
@RequiredArgsConstructor
@Transactional
public class SocialAccountService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final SocialLinkTicketRepository socialLinkTicketRepository;
    private final SecureRandom random = new SecureRandom();

    /* ---------- 소셜 로그인 ---------- */

    public User login(SocialProfile profile) {
        Optional<SocialAccount> linked = socialAccountRepository.findByProviderAndProviderId(profile.provider(), profile.providerId());
        if (linked.isPresent()) {
            return linked.get().getUser();
        }
        if (profile.email() == null || profile.email().isBlank()) {
            throw new SocialLoginException(SocialLoginException.EMAIL_REQUIRED, profile.provider());
        }

        User existing = userRepository.findByEmail(profile.email()).orElse(null);
        if (existing == null) {
            return signUp(profile);
        }
        Optional<SocialAccount> sameProvider = existing.findSocialAccount(profile.provider());
        // 연결 표가 생기기 전에 이 소셜로 가입한 계정: 고유 ID를 채운다
        if (sameProvider.isPresent() && sameProvider.get().getProviderId() == null) {
            sameProvider.get().fillProviderId(profile.providerId());
            return existing;
        }
        if (sameProvider.isEmpty() && profile.emailVerified() && existing.isEmailVerified()) {
            existing.addSocialAccount(profile.provider(), profile.providerId());
            return existing;
        }
        throw new SocialLoginException(SocialLoginException.EMAIL_EXISTS, profile.provider());
    }

    private User signUp(SocialProfile profile) {
        // 소셜 이름은 쓰지 않는다 (#227): 캐릭터를 만들면 닉네임이 이름이 된다
        User user = User.builder()
                .email(profile.email())
                .name(User.nameFromEmail(profile.email()))
                .role(UserRole.ROLE_USER)
                .provider(profile.provider())
                .emailVerified(profile.emailVerified())
                .build();
        user.addSocialAccount(profile.provider(), profile.providerId());
        return userRepository.save(user);
    }

    /* ---------- 마이페이지에서 연결 ---------- */

    // 5분짜리 한 번용 표. 프론트가 /oauth2/authorization/{provider}?link=표 로 보낸다
    public String startLink(Long userId, SocialProvider provider) {
        requireSocial(provider);
        findUser(userId);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        socialLinkTicketRepository.save(new SocialLinkTicket(AccountMailService.hash(raw), userId, provider));
        return raw;
    }

    // 연결은 이메일이 달라도 된다 (다른 이메일로 만든 소셜 계정을 붙이는 게 목적). 이미 다른 계정에 붙은 소셜은 안 된다
    public User link(String rawTicket, SocialProfile profile) {
        String id = AccountMailService.hash(rawTicket);
        SocialLinkTicket ticket = socialLinkTicketRepository.findById(id)
                .orElseThrow(() -> new SocialLoginException(SocialLoginException.LINK_EXPIRED, profile.provider()));
        socialLinkTicketRepository.deleteById(id);
        if (ticket.getProvider() != profile.provider()) {
            throw new SocialLoginException(SocialLoginException.LINK_EXPIRED, profile.provider());
        }
        User user = userRepository.findById(ticket.getUserId())
                .orElseThrow(() -> new SocialLoginException(SocialLoginException.LINK_EXPIRED, profile.provider()));

        Optional<SocialAccount> linked = socialAccountRepository.findByProviderAndProviderId(profile.provider(), profile.providerId());
        if (linked.isPresent()) {
            if (linked.get().getUser().getId().equals(user.getId())) return user;
            throw new SocialLoginException(SocialLoginException.LINK_TAKEN, profile.provider());
        }
        Optional<SocialAccount> own = user.findSocialAccount(profile.provider());
        if (own.isPresent()) {
            if (own.get().getProviderId() != null) {
                throw new SocialLoginException(SocialLoginException.LINK_PROVIDER_EXISTS, profile.provider());
            }
            own.get().fillProviderId(profile.providerId());
            return user;
        }
        user.addSocialAccount(profile.provider(), profile.providerId());
        return user;
    }

    // 로그인 방법(비밀번호 + 연결된 소셜)이 하나만 남으면 해제할 수 없다
    public MyInfoResponse unlink(Long userId, SocialProvider provider) {
        requireSocial(provider);
        User user = findUser(userId);
        if (user.findSocialAccount(provider).isEmpty()) {
            throw new BusinessException(ErrorCode.SOCIAL_NOT_LINKED);
        }
        if (user.loginMethodCount() <= 1) {
            throw new BusinessException(ErrorCode.LAST_LOGIN_METHOD);
        }
        user.removeSocialAccount(provider);
        return MyInfoResponse.from(user);
    }

    /* ---------- 도움 ---------- */

    private void requireSocial(SocialProvider provider) {
        if (provider == null || provider == SocialProvider.LOCAL) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_PROVIDER);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
