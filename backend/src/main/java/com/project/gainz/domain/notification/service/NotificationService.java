package com.project.gainz.domain.notification.service;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.notification.dto.response.NotificationResponse;
import com.project.gainz.domain.notification.entity.Notification;
import com.project.gainz.domain.notification.enums.NotificationType;
import com.project.gainz.domain.notification.repository.NotificationRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 알림 만들기·조회·읽음 처리. 만들기는 각 기능의 트랜잭션 안에서 부르고, 저장은 그 트랜잭션이 커밋된 뒤에 한다
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    public static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final CharacterRepository characterRepository;
    private final PlatformTransactionManager transactionManager;

    /* ---------- 만들기 ---------- */

    // 한 사람에게
    public void notify(User receiver, NotificationType type, String title, String content, String redirectUrl) {
        if (receiver == null) return;
        saveAfterCommit(List.of(Notification.builder()
                .receiver(receiver).type(type).title(title).content(content).redirectUrl(redirectUrl).build()));
    }

    // 여러 사람에게. 한 일을 한 사람(actorId)은 빼고, 같은 사람은 한 번만
    public void notifyAll(Collection<User> receivers, Long actorId, NotificationType type, String title, String content,
                          String redirectUrl) {
        Map<Long, User> unique = new LinkedHashMap<>();
        for (User receiver : receivers) {
            if (receiver != null && !receiver.getId().equals(actorId)) unique.putIfAbsent(receiver.getId(), receiver);
        }
        if (unique.isEmpty()) return;
        saveAfterCommit(unique.values().stream()
                .map(receiver -> Notification.builder()
                        .receiver(receiver).type(type).title(title).content(content).redirectUrl(redirectUrl).build())
                .toList());
    }

    // 알림은 원래 일(곡 저장·친구 신청 등)이 커밋된 뒤 따로 저장한다.
    // 원래 일이 롤백되면 알림도 남지 않고, 알림 저장이 실패해도 원래 일은 그대로 성공한다
    private void saveAfterCommit(List<Notification> notifications) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationRepository.saveAll(notifications);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    TransactionTemplate template = new TransactionTemplate(transactionManager);
                    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    template.executeWithoutResult(status -> notificationRepository.saveAll(notifications));
                } catch (RuntimeException e) {
                    log.warn("알림 저장 실패: {}개, 종류 {} ({})", notifications.size(),
                            notifications.get(0).getType(), e.getMessage());
                }
            }
        });
    }

    // 밴드 알림에 쓰는 이름 (밴드 화면처럼 캐릭터 닉네임, 없으면 계정 이름)
    @Transactional(readOnly = true)
    public String nicknameOf(User user) {
        return characterRepository.findByUserId(user.getId()).map(Character::getNickname).orElse(user.getName());
    }

    /* ---------- 조회·읽음·지우기 ---------- */

    @Transactional(readOnly = true)
    public NotificationResponse.Page list(Long userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Slice<Notification> slice = notificationRepository.findAllByReceiverIdOrderByIdDesc(userId,
                PageRequest.of(Math.max(page, 0), safeSize));
        return new NotificationResponse.Page(slice.getContent().stream().map(NotificationResponse::from).toList(),
                slice.hasNext());
    }

    @Transactional(readOnly = true)
    public NotificationResponse.UnreadCount unreadCount(Long userId) {
        return new NotificationResponse.UnreadCount(notificationRepository.countByReceiverIdAndIsReadFalse(userId));
    }

    public void markRead(Long userId, Long notificationId) {
        find(userId, notificationId).markRead();
    }

    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    public void delete(Long userId, Long notificationId) {
        notificationRepository.delete(find(userId, notificationId));
    }

    // 내 알림 모두 지우기 (회원 탈퇴할 때도)
    public void deleteAll(Long userId) {
        List<Notification> mine = notificationRepository.findAllByReceiverId(userId);
        notificationRepository.deleteAll(mine);
    }

    private Notification find(Long userId, Long notificationId) {
        return notificationRepository.findByIdAndReceiverId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }
}
