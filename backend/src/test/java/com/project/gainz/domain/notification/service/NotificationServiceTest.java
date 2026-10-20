package com.project.gainz.domain.notification.service;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.notification.dto.response.NotificationResponse;
import com.project.gainz.domain.notification.entity.Notification;
import com.project.gainz.domain.notification.enums.NotificationCategory;
import com.project.gainz.domain.notification.enums.NotificationType;
import com.project.gainz.domain.notification.entity.NotificationOff;
import com.project.gainz.domain.notification.enums.NotificationSetting;
import com.project.gainz.domain.notification.repository.NotificationOffRepository;
import com.project.gainz.domain.notification.repository.NotificationRepository;
import com.project.gainz.domain.user.repository.UserRepository;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long ME = 1L;

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationOffRepository notificationOffRepository;
    @Mock private UserRepository userRepository;
    @Mock private CharacterRepository characterRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private NotificationStream notificationStream;

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
    void 트랜잭션_안에서는_커밋된_뒤에_따로_저장하고_실패해도_원래_일을_막지_않는다() {
        User other = user(2L, "가");
        TransactionSynchronizationManager.initSynchronization();
        try {
            notificationService.notify(other, NotificationType.SETLIST_ASSIGN, "밴드", "곡 담당", "/bands/10/setlists");
            // 커밋 전에는 저장하지 않는다 (원래 일이 롤백되면 알림도 없다)
            verify(notificationRepository, never()).saveAll(any());

            when(notificationRepository.saveAll(any())).thenThrow(new IllegalStateException("Data truncated for column 'type'"));
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            // 커밋 뒤 새 트랜잭션에서 저장을 시도하고, 실패는 삼킨다
            verify(transactionManager).getTransaction(argThat(d -> d.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
            verify(notificationRepository).saveAll(any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 저장된_알림은_받는_사람의_열린_화면으로_바로_보낸다() {
        User a = user(2L, "가");
        when(notificationRepository.saveAll(any())).thenAnswer(inv -> {
            List<Notification> list = inv.getArgument(0);
            list.forEach(n -> ReflectionTestUtils.setField(n, "id", 77L));
            return list;
        });

        notificationService.notify(a, NotificationType.FRIEND_ACCEPT, "친구", "수락", "/friends");

        verify(notificationStream).send(eq(2L), argThat(r -> r.id() == 77L && r.type() == NotificationType.FRIEND_ACCEPT && !r.read()));
    }

    @Test
    void 커밋_전에는_보내지_않고_저장이_실패하면_보내지_않는다() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            notificationService.notify(user(2L, "가"), NotificationType.NOTICE, "밴드", "공지", "/home");
            verify(notificationStream, never()).send(any(), any());

            when(notificationRepository.saveAll(any())).thenThrow(new IllegalStateException("저장 실패"));
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(notificationStream, never()).send(any(), any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
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

    /* ---------- 알림 설정 ---------- */

    @Test
    @SuppressWarnings("unchecked")
    void 이_종류를_꺼_둔_사람에게는_만들지_않는다() {
        User a = user(2L, "가");
        User b = user(3L, "나나");
        when(notificationOffRepository.findUserIdsTurnedOff(eq(NotificationSetting.COMMENT), anyCollection())).thenReturn(List.of(3L));

        // 답글(REPLY)은 '댓글·답글' 설정을 따른다
        notificationService.notifyAll(List.of(a, b), ME, NotificationType.REPLY, "밴드", "답글", "/home");

        ArgumentCaptor<List<Notification>> saved = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(n -> n.getReceiver().getId()).containsExactly(2L);
    }

    @Test
    void 모두_꺼_두었으면_저장하지_않고_설정_항목이_없는_종류는_확인하지_않는다() {
        when(notificationOffRepository.findUserIdsTurnedOff(eq(NotificationSetting.FRIEND_REQUEST), anyCollection())).thenReturn(List.of(2L));
        notificationService.notify(user(2L, "가"), NotificationType.FRIEND_REQUEST, "친구", "신청", "/friends");
        verify(notificationRepository, never()).saveAll(any());

        // 밴드 매칭 초대는 설정 항목이 없어 항상 보낸다
        notificationService.notify(user(3L, "나나"), NotificationType.BAND_INVITE, "밴드", "초대", "/home");
        verify(notificationOffRepository, never()).findUserIdsTurnedOff(eq(null), anyCollection());
        verify(notificationRepository).saveAll(any());
    }

    @Test
    void 설정은_처음에_모두_켜져_있고_꺼_둔_항목만_꺼짐() {
        User me = user(ME, "나");
        when(notificationOffRepository.findAllByUserId(ME)).thenReturn(List.of(new NotificationOff(me, NotificationSetting.JAM_NEW)));

        Map<NotificationSetting, Boolean> settings = notificationService.settings(ME).settings();

        assertThat(settings.keySet()).containsExactly(NotificationSetting.values());
        assertThat(settings).containsEntry(NotificationSetting.JAM_NEW, false).containsEntry(NotificationSetting.NOTICE, true);
        assertThat(settings.values()).filteredOn(on -> !on).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 켜면_꺼짐_줄을_지우고_끄면_없는_것만_새로_만든다() {
        User me = user(ME, "나");
        when(userRepository.findById(ME)).thenReturn(Optional.of(me));
        when(notificationOffRepository.findAllByUserId(ME)).thenReturn(List.of(new NotificationOff(me, NotificationSetting.NOTICE)));
        Map<NotificationSetting, Boolean> changes = new java.util.HashMap<>();
        changes.put(NotificationSetting.JAM_NEW, true);
        changes.put(NotificationSetting.NOTICE, false);
        changes.put(NotificationSetting.MENTION, false);
        changes.put(NotificationSetting.COMMENT, null);

        notificationService.updateSettings(ME, changes);

        verify(notificationOffRepository).turnOn(ME, Set.of(NotificationSetting.JAM_NEW));
        ArgumentCaptor<List<NotificationOff>> saved = ArgumentCaptor.forClass(List.class);
        verify(notificationOffRepository).saveAll(saved.capture());
        // 공지는 이미 꺼져 있어서 멘션만 새로
        assertThat(saved.getValue()).extracting(NotificationOff::getSetting).containsExactly(NotificationSetting.MENTION);
    }

    @Test
    void 탈퇴하면_알림과_설정을_모두_지운다() {
        when(notificationRepository.findAllByReceiverId(ME)).thenReturn(List.of());

        notificationService.removeAllOf(ME);

        verify(notificationRepository).deleteAll(List.of());
        verify(notificationOffRepository).deleteAllByUserId(ME);
    }

    @Test
    void 설정_항목이_없는_종류는_밴드_매칭_초대와_수락뿐이다() {
        for (NotificationType type : NotificationType.values()) {
            boolean always = type == NotificationType.BAND_INVITE || type == NotificationType.BAND_ACCEPT;
            assertThat(NotificationSetting.of(type)).as(type.name()).matches(s -> always == (s == null));
            assertThat(java.util.Arrays.stream(NotificationSetting.values()).filter(s -> s.types().contains(type)))
                    .as(type.name()).hasSize(always ? 0 : 1);
        }
    }

    /* ---------- 조회·읽음·지우기 ---------- */

    @Test
    void 목록은_최근_순이고_페이지_크기는_1에서_50_사이로_맞춘다() {
        User me = user(ME, "나");
        when(notificationRepository.findPage(eq(ME), any(), eq(false), any(Pageable.class)))
                .thenAnswer(inv -> new SliceImpl<>(List.of(notification(9L, me)), inv.getArgument(3), true));

        NotificationResponse.Page page = notificationService.list(ME, -1, 500, null, false);

        // 영역을 안 고르면 모든 종류
        verify(notificationRepository).findPage(ME, EnumSet.allOf(NotificationType.class), false,
                PageRequest.of(0, NotificationService.MAX_PAGE_SIZE));
        assertThat(page.hasNext()).isTrue();
        assertThat(page.items()).extracting(NotificationResponse::id).containsExactly(9L);
        assertThat(page.items().get(0).read()).isFalse();
        assertThat(page.items().get(0).redirectUrl()).isEqualTo("/home?band=10&post=20");
    }

    @Test
    void 영역과_안_읽음으로_걸러_본다() {
        when(notificationRepository.findPage(eq(ME), any(), eq(true), any(Pageable.class)))
                .thenAnswer(inv -> new SliceImpl<>(List.of(), inv.getArgument(3), false));

        notificationService.list(ME, 0, 20, NotificationCategory.BAND, true);

        // 밴드에는 커뮤니티·합주 알림도 들어간다
        verify(notificationRepository).findPage(ME, EnumSet.of(NotificationType.BAND_JOIN, NotificationType.BAND_KICKED,
                NotificationType.BAND_LEADER, NotificationType.BAND_DISBAND, NotificationType.BAND_LEAVE,
                NotificationType.BAND_INVITE, NotificationType.BAND_ACCEPT, NotificationType.NOTICE, NotificationType.COMMENT,
                NotificationType.REPLY, NotificationType.MENTION, NotificationType.SETLIST_ASSIGN, NotificationType.JAM_NEW,
                NotificationType.JAM_TRACK), true, PageRequest.of(0, 20));
    }

    @Test
    void 모든_알림_종류는_어느_한_영역에만_속한다() {
        for (NotificationType type : NotificationType.values()) {
            assertThat(java.util.Arrays.stream(NotificationCategory.values()).filter(c -> c.types().contains(type)))
                    .as(type.name()).hasSize(1);
        }
    }

    @Test
    void 고른_알림은_내_것만_읽음으로_바꾸거나_지운다() {
        List<Notification> mine = List.of(notification(9L, user(ME, "나")), notification(10L, user(ME, "나")));
        when(notificationRepository.findAllByReceiverIdAndIdIn(ME, List.of(9L, 10L, 99L))).thenReturn(mine);

        notificationService.markRead(ME, List.of(9L, 10L, 99L));
        notificationService.delete(ME, List.of(9L, 10L, 99L));

        assertThat(mine).allMatch(Notification::getIsRead);
        verify(notificationRepository).deleteAll(mine);
    }

    @Test
    void 읽은_알림만_지운다() {
        List<Notification> read = List.of(notification(9L, user(ME, "나")));
        when(notificationRepository.findAllByReceiverIdAndIsReadTrue(ME)).thenReturn(read);

        notificationService.deleteRead(ME);

        verify(notificationRepository).deleteAll(read);
    }

    @Test
    void 매일_30일_지난_읽은_알림을_정리한다() {
        notificationService.deleteOldReadNotifications();

        ArgumentCaptor<LocalDateTime> before = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> now = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationRepository).deleteReadBefore(before.capture(), now.capture());
        assertThat(before.getValue()).isEqualTo(now.getValue().minusDays(NotificationService.KEEP_READ_DAYS));
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
