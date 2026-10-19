package com.project.gainz.domain.friend.service;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.friend.dto.request.FriendRequestCreateRequest;
import com.project.gainz.domain.friend.dto.response.FriendCodeResponse;
import com.project.gainz.domain.friend.dto.response.FriendRequestResponse;
import com.project.gainz.domain.friend.dto.response.FriendResponse;
import com.project.gainz.domain.friend.entity.Friendship;
import com.project.gainz.domain.friend.enums.FriendshipStatus;
import com.project.gainz.domain.friend.repository.FriendshipRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.common.enums.InstrumentType;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    private static final Long ME = 1L;
    private static final Long OTHER = 2L;
    private static final Long STRANGER = 3L;

    @Mock private FriendshipRepository friendshipRepository;
    @Mock private UserRepository userRepository;
    @Mock private CharacterRepository characterRepository;

    @InjectMocks private FriendService friendService;

    /* ---------- 준비 ---------- */

    private User user(Long id, String name) {
        User user = User.builder()
                .email("user" + id + "@example.com")
                .password("pw")
                .name(name)
                .provider(SocialProvider.LOCAL)
                .role(UserRole.ROLE_USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private User user(Long id) {
        return user(id, "유저" + id);
    }

    private Character character(User user, String nickname) {
        return Character.builder()
                .user(user)
                .nickname(nickname)
                .instrumentType(InstrumentType.DRUM)
                .instrumentColor("#000000")
                .gender(Gender.values()[0])
                .hairStyle(HairStyle.values()[0])
                .hairColor("#000000")
                .currentOutfit(mock(OutfitItem.class))
                .build();
    }

    private Friendship friendship(Long id, User requester, User receiver, boolean accepted) {
        Friendship friendship = Friendship.builder().requester(requester).receiver(receiver).build();
        ReflectionTestUtils.setField(friendship, "id", id);
        if (accepted) friendship.accept();
        return friendship;
    }

    private void found(User user) {
        lenient().when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    /* ---------- 친구 코드 ---------- */

    @Test
    void 친구_코드가_없으면_처음_조회할_때_만든다() {
        User me = user(ME);
        found(me);
        when(userRepository.existsByFriendCode(anyString())).thenReturn(false);

        FriendCodeResponse response = friendService.getMyCode(ME);

        assertThat(response.code()).hasSize(FriendService.FRIEND_CODE_LENGTH);
        assertThat(response.code().chars()).allMatch(c -> FriendService.FRIEND_CODE_CHARS.indexOf(c) >= 0);
        assertThat(me.getFriendCode()).isEqualTo(response.code());
    }

    @Test
    void 친구_코드가_있으면_그대로_돌려준다() {
        User me = user(ME);
        me.changeFriendCode("ABCD2345");
        found(me);

        assertThat(friendService.getMyCode(ME).code()).isEqualTo("ABCD2345");
        verify(userRepository, never()).existsByFriendCode(anyString());
    }

    @Test
    void 새_코드를_받으면_예전_코드와_바뀐다() {
        User me = user(ME);
        me.changeFriendCode("ABCD2345");
        found(me);
        // 처음 만든 코드가 겹치면 다시 만든다
        when(userRepository.existsByFriendCode(anyString())).thenReturn(true, false);

        FriendCodeResponse response = friendService.regenerateCode(ME);

        assertThat(response.code()).hasSize(FriendService.FRIEND_CODE_LENGTH);
        assertThat(me.getFriendCode()).isEqualTo(response.code());
    }

    @Test
    void 코드가_계속_겹치면_만들지_못한다() {
        found(user(ME));
        when(userRepository.existsByFriendCode(anyString())).thenReturn(true);

        assertThatThrownBy(() -> friendService.regenerateCode(ME))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FRIEND_CODE_GENERATION_FAILED);
    }

    /* ---------- 친구 신청 ---------- */

    @Test
    void 친구_코드로_신청하면_대기_상태로_저장하고_받는_사람을_돌려준다() {
        User me = user(ME);
        User other = user(OTHER, "드러머");
        found(me);
        when(userRepository.findByFriendCode("ABCD2345")).thenReturn(Optional.of(other));
        when(friendshipRepository.findAllBetween(ME, OTHER)).thenReturn(List.of());
        when(friendshipRepository.save(any(Friendship.class))).thenAnswer(inv -> {
            Friendship saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 50L);
            return saved;
        });
        when(characterRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of(character(other, "쿵쿵")));

        // 앞뒤 공백·소문자도 받는다
        FriendRequestResponse response = friendService.sendRequest(ME, new FriendRequestCreateRequest("  abcd2345 "));

        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository).save(captor.capture());
        assertThat(captor.getValue().getRequester()).isSameAs(me);
        assertThat(captor.getValue().getReceiver()).isSameAs(other);
        assertThat(captor.getValue().getStatus()).isEqualTo(FriendshipStatus.PENDING);
        assertThat(response.requestId()).isEqualTo(50L);
        assertThat(response.user().userId()).isEqualTo(OTHER);
        assertThat(response.user().name()).isEqualTo("드러머");
        assertThat(response.user().character().nickname()).isEqualTo("쿵쿵");
    }

    @Test
    void 없는_코드로는_신청할_수_없다() {
        found(user(ME));
        when(userRepository.findByFriendCode("ZZZZ9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.sendRequest(ME, new FriendRequestCreateRequest("ZZZZ9999")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FRIEND_CODE);
    }

    @Test
    void 내_코드로는_신청할_수_없다() {
        User me = user(ME);
        found(me);
        when(userRepository.findByFriendCode("ABCD2345")).thenReturn(Optional.of(me));

        assertThatThrownBy(() -> friendService.sendRequest(ME, new FriendRequestCreateRequest("ABCD2345")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_FRIEND_SELF);
    }

    @Test
    void 이미_친구면_신청할_수_없다() {
        User me = user(ME);
        User other = user(OTHER);
        found(me);
        when(userRepository.findByFriendCode("ABCD2345")).thenReturn(Optional.of(other));
        when(friendshipRepository.findAllBetween(ME, OTHER)).thenReturn(List.of(friendship(1L, other, me, true)));

        assertThatThrownBy(() -> friendService.sendRequest(ME, new FriendRequestCreateRequest("ABCD2345")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_FRIENDS);
    }

    @Test
    void 이미_보낸_신청이_있으면_다시_보낼_수_없다() {
        User me = user(ME);
        User other = user(OTHER);
        found(me);
        when(userRepository.findByFriendCode("ABCD2345")).thenReturn(Optional.of(other));
        when(friendshipRepository.findAllBetween(ME, OTHER)).thenReturn(List.of(friendship(1L, me, other, false)));

        assertThatThrownBy(() -> friendService.sendRequest(ME, new FriendRequestCreateRequest("ABCD2345")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FRIEND_REQUEST_ALREADY_SENT);
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void 상대가_먼저_신청했으면_받은_신청에서_수락하라고_알린다() {
        User me = user(ME);
        User other = user(OTHER);
        found(me);
        when(userRepository.findByFriendCode("ABCD2345")).thenReturn(Optional.of(other));
        when(friendshipRepository.findAllBetween(ME, OTHER)).thenReturn(List.of(friendship(1L, other, me, false)));

        assertThatThrownBy(() -> friendService.sendRequest(ME, new FriendRequestCreateRequest("ABCD2345")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FRIEND_REQUEST_ALREADY_RECEIVED);
    }

    @Test
    void 친구가_가득_차면_신청할_수_없다() {
        User me = user(ME);
        User other = user(OTHER);
        found(me);
        when(userRepository.findByFriendCode("ABCD2345")).thenReturn(Optional.of(other));
        when(friendshipRepository.findAllBetween(ME, OTHER)).thenReturn(List.of());
        when(friendshipRepository.countByUserIdAndStatus(ME, FriendshipStatus.ACCEPTED)).thenReturn((long) FriendService.MAX_FRIENDS);

        assertThatThrownBy(() -> friendService.sendRequest(ME, new FriendRequestCreateRequest("ABCD2345")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FRIEND_LIMIT_EXCEEDED);
    }

    @Test
    void 받은_신청에는_보낸_사람이_보낸_신청에는_받는_사람이_보인다() {
        User me = user(ME);
        User sender = user(OTHER, "보낸사람");
        User receiver = user(STRANGER, "받는사람");
        when(friendshipRepository.findAllByReceiverIdAndStatusOrderByIdDesc(ME, FriendshipStatus.PENDING))
                .thenReturn(List.of(friendship(1L, sender, me, false)));
        when(friendshipRepository.findAllByRequesterIdAndStatusOrderByIdDesc(ME, FriendshipStatus.PENDING))
                .thenReturn(List.of(friendship(2L, me, receiver, false)));
        when(characterRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of());

        List<FriendRequestResponse> received = friendService.getReceivedRequests(ME);
        List<FriendRequestResponse> sent = friendService.getSentRequests(ME);

        assertThat(received).extracting(r -> r.user().name()).containsExactly("보낸사람");
        assertThat(sent).extracting(r -> r.user().name()).containsExactly("받는사람");
        assertThat(received.get(0).user().character()).isNull();
    }

    /* ---------- 수락·거절·취소 ---------- */

    @Test
    void 받은_신청을_수락하면_친구가_되고_보낸_사람을_돌려준다() {
        User me = user(ME);
        User sender = user(OTHER, "보낸사람");
        Friendship request = friendship(7L, sender, me, false);
        when(friendshipRepository.findById(7L)).thenReturn(Optional.of(request));
        when(characterRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of());

        FriendResponse response = friendService.acceptRequest(ME, 7L);

        assertThat(request.isAccepted()).isTrue();
        assertThat(request.getAcceptedAt()).isNotNull();
        assertThat(response.userId()).isEqualTo(OTHER);
        assertThat(response.since()).isEqualTo(request.getAcceptedAt());
    }

    @Test
    void 내가_보낸_신청은_내가_수락할_수_없다() {
        User me = user(ME);
        Friendship request = friendship(7L, me, user(OTHER), false);
        when(friendshipRepository.findById(7L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> friendService.acceptRequest(ME, 7L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        assertThat(request.isPending()).isTrue();
    }

    @Test
    void 상대_친구가_가득_차면_수락할_수_없다() {
        User me = user(ME);
        Friendship request = friendship(7L, user(OTHER), me, false);
        when(friendshipRepository.findById(7L)).thenReturn(Optional.of(request));
        when(friendshipRepository.countByUserIdAndStatus(ME, FriendshipStatus.ACCEPTED)).thenReturn(3L);
        when(friendshipRepository.countByUserIdAndStatus(OTHER, FriendshipStatus.ACCEPTED)).thenReturn((long) FriendService.MAX_FRIENDS);

        assertThatThrownBy(() -> friendService.acceptRequest(ME, 7L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FRIEND_LIMIT_EXCEEDED);
        assertThat(request.isPending()).isTrue();
    }

    @Test
    void 받은_사람은_거절하고_보낸_사람은_취소할_수_있다() {
        User me = user(ME);
        Friendship received = friendship(7L, user(OTHER), me, false);
        Friendship sent = friendship(8L, me, user(STRANGER), false);
        when(friendshipRepository.findById(7L)).thenReturn(Optional.of(received));
        when(friendshipRepository.findById(8L)).thenReturn(Optional.of(sent));

        friendService.deleteRequest(ME, 7L);
        friendService.deleteRequest(ME, 8L);

        verify(friendshipRepository).delete(received);
        verify(friendshipRepository).delete(sent);
    }

    @Test
    void 남의_신청이나_이미_친구인_관계는_신청으로_지울_수_없다() {
        Friendship others = friendship(7L, user(OTHER), user(STRANGER), false);
        Friendship accepted = friendship(8L, user(OTHER), user(ME), true);
        when(friendshipRepository.findById(7L)).thenReturn(Optional.of(others));
        when(friendshipRepository.findById(8L)).thenReturn(Optional.of(accepted));

        assertThatThrownBy(() -> friendService.deleteRequest(ME, 7L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        assertThatThrownBy(() -> friendService.deleteRequest(ME, 8L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        verify(friendshipRepository, never()).delete(any());
    }

    /* ---------- 친구 목록·끊기 ---------- */

    @Test
    void 친구_목록은_내가_신청했든_받았든_상대를_이름_순으로_보여_준다() {
        User me = user(ME);
        User b = user(OTHER, "베이시스트");
        User a = user(STRANGER, "가수");
        when(friendshipRepository.findAllByUserIdAndStatus(ME, FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(friendship(1L, me, b, true), friendship(2L, a, me, true)));
        when(characterRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of(character(a, "보컬왕")));

        List<FriendResponse> friends = friendService.getFriends(ME);

        assertThat(friends).extracting(FriendResponse::name).containsExactly("가수", "베이시스트");
        assertThat(friends.get(0).character().nickname()).isEqualTo("보컬왕");
        assertThat(friends.get(1).character()).isNull();
        assertThat(friends).allMatch(f -> f.since() != null);
    }

    @Test
    void 친구를_끊으면_관계를_지운다() {
        Friendship accepted = friendship(1L, user(OTHER), user(ME), true);
        when(friendshipRepository.findAllBetween(ME, OTHER)).thenReturn(List.of(accepted));

        friendService.removeFriend(ME, OTHER);

        verify(friendshipRepository).delete(accepted);
    }

    @Test
    void 친구가_아니면_끊을_수_없다() {
        when(friendshipRepository.findAllBetween(ME, OTHER)).thenReturn(List.of(friendship(1L, user(ME), user(OTHER), false)));

        assertThatThrownBy(() -> friendService.removeFriend(ME, OTHER))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FRIEND);
        verify(friendshipRepository, never()).delete(any());
    }

    @Test
    void 친구인지는_수락된_관계만_본다() {
        when(friendshipRepository.findAllBetween(ME, OTHER)).thenReturn(List.of(friendship(1L, user(ME), user(OTHER), true)));
        when(friendshipRepository.findAllBetween(ME, STRANGER)).thenReturn(List.of(friendship(2L, user(ME), user(STRANGER), false)));

        assertThat(friendService.areFriends(ME, OTHER)).isTrue();
        assertThat(friendService.areFriends(ME, STRANGER)).isFalse();
    }

    @Test
    void 탈퇴하면_친구와_신청을_모두_지운다() {
        List<Friendship> all = List.of(friendship(1L, user(ME), user(OTHER), true), friendship(2L, user(STRANGER), user(ME), false));
        when(friendshipRepository.findAllByUserId(ME)).thenReturn(all);

        friendService.removeAll(ME);

        verify(friendshipRepository).deleteAll(all);
    }
}
