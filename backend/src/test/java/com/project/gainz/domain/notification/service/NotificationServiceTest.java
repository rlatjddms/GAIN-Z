package com.project.gainz.domain.notification.service;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.notification.dto.response.NotificationResponse;
import com.project.gainz.domain.notification.entity.Notification;
import com.project.gainz.domain.notification.enums.NotificationType;
import com.project.gainz.domain.notification.repository.NotificationRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.global.common.enums.InstrumentType;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long ME = 1L;

    @Mock private NotificationRepository notificationRepository;
    @Mock private CharacterRepository characterRepository;

    @InjectMocks private NotificationService notificationService;

    private User user(Long id, String name) {
        User user = User.builder().email("u" + id + "@example.com").password("pw").name(name)
                .provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Notification notification(Long id, User receiver) {
        Notification n = Notification.builder().receiver(receiver).type(NotificationType.NOTICE)
                .title("밴드").content("새 공지: 합주").redirectUrl("/home?band=10&post=20").build();
        ReflectionTestUtils.setField(n, "id", id);
        return n;
    }

    /* ---------- 만들기 ---------- */

    @Test
    @SuppressWarnings("unchecked")
    void 여러_사람에게_보낼_때_한_사람은_빼고_같은_사람은_한_번만() {
        User me = user(ME, "나");
        User a = user(2L, "가");
        User b = user(3L, "나나");

        notificationService.notifyAll(List.of(me, a, b, a), ME, NotificationType.NOTICE, "밴드", "새 공지: 합주", "/home");

        ArgumentCaptor<List<Notification>> saved = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(n -> n.getReceiver().getId()).containsExactly(2L, 3L);
        assertThat(saved.getValue()).allSatisfy(n -> {
            assertThat(n.getType()).isEqualTo(NotificationType.NOTICE);
            assertThat(n.getTitle()).isEqualTo("밴드");
            assertThat(n.getContent()).isEqualTo("새 공지: 합주");
            assertThat(n.getRedirectUrl()).isEqualTo("/home");
            assertThat(n.getIsRead()).isFalse();
        });
    }

    @Test
    void 받을_사람이_없으면_저장하지_않는다() {
        notificationService.notifyAll(List.of(user(ME, "나")), ME, NotificationType.NOTICE, "밴드", "내용", null);
        notificationService.notify(null, NotificationType.BAND_KICKED, "밴드", "내용", null);

        verify(notificationRepository, never()).saveAll(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void 긴_내용은_잘라서_저장한다() {
        Notification n = Notification.builder().receiver(user(ME, "나")).type(NotificationType.COMMENT)
                .title("밴드").content("가".repeat(300)).build();

        assertThat(n.getContent()).hasSize(Notification.CONTENT_MAX).endsWith("…");
    }

    @Test
    void 밴드_알림_이름은_캐릭터_닉네임_없으면_계정_이름() {
        User withCharacter = user(2L, "김철수");
        User without = user(3L, "이영희");
        Character character = Character.builder().user(withCharacter).nickname("드럼짱").instrumentType(InstrumentType.DRUM)
                .instrumentColor("#000000").gender(Gender.values()[0]).hairStyle(HairStyle.values()[0]).hairColor("#000000")
                .currentOutfit(mock(OutfitItem.class)).build();
        when(characterRepository.findByUserId(2L)).thenReturn(Optional.of(character));
        when(characterRepository.findByUserId(3L)).thenReturn(Optional.empty());

        assertThat(notificationService.nicknameOf(withCharacter)).isEqualTo("드럼짱");
        assertThat(notificationService.nicknameOf(without)).isEqualTo("이영희");
    }

    /* ---------- 조회·읽음·지우기 ---------- */

    @Test
    void 목록은_최근_순이고_페이지_크기는_1에서_50_사이로_맞춘다() {
        User me = user(ME, "나");
        when(notificationRepository.findAllByReceiverIdOrderByIdDesc(eq(ME), any(Pageable.class)))
                .thenAnswer(inv -> new SliceImpl<>(List.of(notification(9L, me)), inv.getArgument(1), true));

        NotificationResponse.Page page = notificationService.list(ME, -1, 500);

        verify(notificationRepository).findAllByReceiverIdOrderByIdDesc(ME, PageRequest.of(0, NotificationService.MAX_PAGE_SIZE));
        assertThat(page.hasNext()).isTrue();
        assertThat(page.items()).extracting(NotificationResponse::id).containsExactly(9L);
        assertThat(page.items().get(0).read()).isFalse();
        assertThat(page.items().get(0).redirectUrl()).isEqualTo("/home?band=10&post=20");
    }

    @Test
    void 안_읽은_개수를_센다() {
        when(notificationRepository.countByReceiverIdAndIsReadFalse(ME)).thenReturn(4L);

        assertThat(notificationService.unreadCount(ME).count()).isEqualTo(4L);
    }

    @Test
    void 내_알림만_읽음으로_바꾸거나_지울_수_있다() {
        Notification mine = notification(9L, user(ME, "나"));
        when(notificationRepository.findByIdAndReceiverId(9L, ME)).thenReturn(Optional.of(mine));
        when(notificationRepository.findByIdAndReceiverId(8L, ME)).thenReturn(Optional.empty());

        notificationService.markRead(ME, 9L);
        notificationService.delete(ME, 9L);

        assertThat(mine.getIsRead()).isTrue();
        verify(notificationRepository).delete(mine);
        assertThatThrownBy(() -> notificationService.markRead(ME, 8L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
        assertThatThrownBy(() -> notificationService.delete(ME, 8L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void 모두_읽음과_모두_지우기() {
        List<Notification> mine = List.of(notification(9L, user(ME, "나")), notification(10L, user(ME, "나")));
        when(notificationRepository.findAllByReceiverId(ME)).thenReturn(mine);

        notificationService.markAllRead(ME);
        notificationService.deleteAll(ME);

        verify(notificationRepository).markAllRead(ME);
        verify(notificationRepository).deleteAll(mine);
    }
}
