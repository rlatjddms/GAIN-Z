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
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 이메일·소셜 로그인 계정 연동 (#189)
@ExtendWith(MockitoExtension.class)
class SocialAccountServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SocialAccountRepository socialAccountRepository;
    @Mock private SocialLinkTicketRepository socialLinkTicketRepository;
    @Mock private com.project.gainz.domain.character.repository.CharacterRepository characterRepository;
    @InjectMocks private SocialAccountService service;

    private static User user(long id, String email, String password, Boolean emailVerified) {
        User user = User.builder().email(email).password(password)
                .provider(password == null ? SocialProvider.KAKAO : SocialProvider.LOCAL)
                .role(UserRole.ROLE_USER).emailVerified(emailVerified).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static SocialProfile google(String email, boolean verified) {
        return new SocialProfile(SocialProvider.GOOGLE, "g-1", email, verified);
    }

    private static void assertSocialError(ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOf(SocialLoginException.class).extracting("code").isEqualTo(code);
    }

    /* ---------- 소셜 로그인 ---------- */

    @Test
    void 연결된_소셜_고유_ID가_있으면_이메일과_상관없이_그_계정으로_로그인한다() {
        User me = user(1, "me@test.invalid", "encoded", true);
        me.addSocialAccount(SocialProvider.GOOGLE, "g-1");
        SocialAccount account = me.findSocialAccount(SocialProvider.GOOGLE).orElseThrow();
        when(socialAccountRepository.findByProviderAndProviderId(SocialProvider.GOOGLE, "g-1")).thenReturn(Optional.of(account));

        assertThat(service.login(google("other@test.invalid", false))).isSameAs(me);
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void 같은_이메일_계정이_있으면_양쪽_다_인증된_이메일일_때만_자동_연결한다() {
        User me = user(1, "me@test.invalid", "encoded", true);
        when(userRepository.findByEmail("me@test.invalid")).thenReturn(Optional.of(me));

        assertThat(service.login(google("me@test.invalid", true))).isSameAs(me);
        assertThat(me.linkedProviders()).containsExactly(SocialProvider.GOOGLE);
        assertThat(me.findSocialAccount(SocialProvider.GOOGLE).orElseThrow().getProviderId()).isEqualTo("g-1");
    }

    @Test
    void 소셜_쪽_이메일이_인증되지_않았으면_자동_연결하지_않는다() {
        User me = user(1, "me@test.invalid", "encoded", true);
        when(userRepository.findByEmail("me@test.invalid")).thenReturn(Optional.of(me));

        assertSocialError(() -> service.login(google("me@test.invalid", false)), SocialLoginException.EMAIL_EXISTS);
        assertThat(me.linkedProviders()).isEmpty();
    }

    @Test
    void 기존_계정의_이메일이_인증되지_않았으면_자동_연결하지_않는다() {
        User unverified = user(1, "me@test.invalid", "encoded", false);
        when(userRepository.findByEmail("me@test.invalid")).thenReturn(Optional.of(unverified));

        assertSocialError(() -> service.login(google("me@test.invalid", true)), SocialLoginException.EMAIL_EXISTS);
        assertThat(unverified.linkedProviders()).isEmpty();
    }

    @Test
    void 연결_표가_생기기_전에_이_소셜로_가입한_계정은_고유_ID를_채운다() {
        User legacy = user(1, "me@test.invalid", null, null);
        legacy.addSocialAccount(SocialProvider.GOOGLE, null);
        when(userRepository.findByEmail("me@test.invalid")).thenReturn(Optional.of(legacy));

        // 소셜 쪽 인증 여부와 상관없이 (원래 이 소셜로 가입했으니)
        assertThat(service.login(google("me@test.invalid", false))).isSameAs(legacy);
        assertThat(legacy.findSocialAccount(SocialProvider.GOOGLE).orElseThrow().getProviderId()).isEqualTo("g-1");
    }

    @Test
    void 처음이면_비밀번호_없는_계정을_만들고_소셜을_연결한다() {
        when(userRepository.findByEmail("new@test.invalid")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = service.login(new SocialProfile(SocialProvider.NAVER, "n-1", "new@test.invalid", false));

        assertThat(created.hasPassword()).isFalse();
        assertThat(created.getProvider()).isEqualTo(SocialProvider.NAVER);
        // 소셜 이름은 받지 않는다 (#227): 캐릭터를 만들기 전에는 이메일 앞부분
        assertThat(created.nameWithoutCharacter()).isEqualTo("new");
        assertThat(created.isEmailVerified()).isFalse();
        assertThat(created.findSocialAccount(SocialProvider.NAVER).orElseThrow().getProviderId()).isEqualTo("n-1");
    }

    @Test
    void 소셜에서_이메일을_받지_못하면_가입하지_않는다() {
        assertSocialError(() -> service.login(new SocialProfile(SocialProvider.KAKAO, "k-1", null, false)),
                SocialLoginException.EMAIL_REQUIRED);
        verify(userRepository, never()).save(any());
    }

    /* ---------- 마이페이지에서 연결 ---------- */

    private User withTicket(String raw, User user, SocialProvider provider) {
        String id = AccountMailService.hash(raw);
        when(socialLinkTicketRepository.findById(id)).thenReturn(Optional.of(new SocialLinkTicket(id, user.getId(), provider)));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void 연결_표는_해시만_저장하고_소셜이_아니면_시작하지_않는다() {
        User me = user(1, "me@test.invalid", "encoded", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));

        String raw = service.startLink(1L, SocialProvider.KAKAO);

        ArgumentCaptor<SocialLinkTicket> saved = ArgumentCaptor.forClass(SocialLinkTicket.class);
        verify(socialLinkTicketRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(AccountMailService.hash(raw)).isNotEqualTo(raw);
        assertThat(saved.getValue().getUserId()).isEqualTo(1L);
        assertThat(saved.getValue().getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThatThrownBy(() -> service.startLink(1L, SocialProvider.LOCAL))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_SOCIAL_PROVIDER);
    }

    @Test
    void 연결은_이메일이_달라도_되고_표는_한_번만_쓴다() {
        User me = withTicket("ticket", user(1, "me@test.invalid", "encoded", true), SocialProvider.GOOGLE);

        assertThat(service.link("ticket", google("other@test.invalid", false))).isSameAs(me);

        assertThat(me.findSocialAccount(SocialProvider.GOOGLE).orElseThrow().getProviderId()).isEqualTo("g-1");
        verify(socialLinkTicketRepository).deleteById(AccountMailService.hash("ticket"));
    }

    @Test
    void 표가_없거나_다른_소셜용이면_연결하지_않는다() {
        when(socialLinkTicketRepository.findById(AccountMailService.hash("old"))).thenReturn(Optional.empty());
        assertSocialError(() -> service.link("old", google("me@test.invalid", true)), SocialLoginException.LINK_EXPIRED);

        User me = user(1, "me@test.invalid", "encoded", true);
        String id = AccountMailService.hash("kakao-ticket");
        when(socialLinkTicketRepository.findById(id)).thenReturn(Optional.of(new SocialLinkTicket(id, 1L, SocialProvider.KAKAO)));
        assertSocialError(() -> service.link("kakao-ticket", google("me@test.invalid", true)), SocialLoginException.LINK_EXPIRED);
        assertThat(me.linkedProviders()).isEmpty();
    }

    @Test
    void 다른_계정에_연결된_소셜은_연결하지_않는다() {
        withTicket("ticket", user(1, "me@test.invalid", "encoded", true), SocialProvider.GOOGLE);
        User other = user(2, "other@test.invalid", "encoded", true);
        other.addSocialAccount(SocialProvider.GOOGLE, "g-1");
        when(socialAccountRepository.findByProviderAndProviderId(SocialProvider.GOOGLE, "g-1"))
                .thenReturn(other.findSocialAccount(SocialProvider.GOOGLE));

        assertSocialError(() -> service.link("ticket", google("me@test.invalid", true)), SocialLoginException.LINK_TAKEN);
    }

    @Test
    void 같은_종류의_다른_소셜_계정이_이미_연결돼_있으면_연결하지_않는다() {
        User me = withTicket("ticket", user(1, "me@test.invalid", "encoded", true), SocialProvider.GOOGLE);
        me.addSocialAccount(SocialProvider.GOOGLE, "g-other");

        assertSocialError(() -> service.link("ticket", google("me@test.invalid", true)), SocialLoginException.LINK_PROVIDER_EXISTS);
    }

    /* ---------- 해제 ---------- */

    @Test
    void 로그인_방법이_하나만_남으면_해제할_수_없다() {
        User socialOnly = user(1, "me@test.invalid", null, true);
        socialOnly.addSocialAccount(SocialProvider.KAKAO, "k-1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(socialOnly));

        assertThatThrownBy(() -> service.unlink(1L, SocialProvider.KAKAO))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.LAST_LOGIN_METHOD);
        assertThatThrownBy(() -> service.unlink(1L, SocialProvider.GOOGLE))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.SOCIAL_NOT_LINKED);
        assertThat(socialOnly.linkedProviders()).containsExactly(SocialProvider.KAKAO);
    }

    @Test
    void 다른_방법이_남아_있으면_해제하고_내_정보를_돌려준다() {
        User me = user(1, "me@test.invalid", "encoded", true);
        me.addSocialAccount(SocialProvider.KAKAO, "k-1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));

        MyInfoResponse info = service.unlink(1L, SocialProvider.KAKAO);

        assertThat(info.socialLinks()).isEmpty();
        assertThat(info.hasPassword()).isTrue();
    }
}
