package com.project.gainz.domain.notification.service;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.notification.dto.response.LiveChange;
import com.project.gainz.domain.notification.dto.response.NotificationResponse;
import com.project.gainz.domain.notification.entity.Notification;
import com.project.gainz.domain.notification.entity.NotificationOff;
import com.project.gainz.domain.notification.enums.NotificationCategory;
import com.project.gainz.domain.notification.enums.NotificationSetting;
import com.project.gainz.domain.notification.enums.NotificationType;
import com.project.gainz.domain.notification.repository.NotificationOffRepository;
import com.project.gainz.domain.notification.repository.NotificationRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 알림 만들기·조회·읽음 처리. 만들기는 각 기능의 트랜잭션 안에서 부르고, 저장은 그 트랜잭션이 커밋된 뒤에 한다
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    public static final int MAX_PAGE_SIZE = 50;
    public static final int KEEP_READ_DAYS = 30;

    private final NotificationRepository notificationRepository;
    private final NotificationOffRepository notificationOffRepository;
    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final PlatformTransactionManager transactionManager;
    private final NotificationStream notificationStream;

    /* ---------- 만들기 ---------- */

    // 한 사람에게
    public void notify(User receiver, NotificationType type, String title, String content, String redirectUrl) {
        if (receiver == null) return;
        notifyAll(List.of(receiver), null, type, title, content, redirectUrl);
    }

    // 여러 사람에게. 한 일을 한 사람(actorId)은 빼고, 같은 사람은 한 번만, 이 종류를 꺼 둔 사람은 빼고
    public void notifyAll(Collection<User> receivers, Long actorId, NotificationType type, String title, String content,
                          String redirectUrl) {
        Map<Long, User> unique = new LinkedHashMap<>();
        for (User receiver : receivers) {
            if (receiver != null && !receiver.getId().equals(actorId)) unique.putIfAbsent(receiver.getId(), receiver);
        }
        NotificationSetting setting = NotificationSetting.of(type);
        if (setting != null && !unique.isEmpty()) {
            notificationOffRepository.findUserIdsTurnedOff(setting, unique.keySet()).forEach(unique::remove);
        }
        if (unique.isEmpty()) return;
        saveAfterCommit(unique.values().stream()
                .map(receiver -> Notification.builder()
                        .receiver(receiver).type(type).title(title).content(content).redirectUrl(redirectUrl).build())
                .toList());
    }

    // 알림은 원래 일(곡 저장·친구 신청 등)이 커밋된 뒤 따로 저장하고, 저장되면 실시간 연결로 바로 보낸다.
    // 원래 일이 롤백되면 알림도 남지 않고, 알림 저장이 실패해도 원래 일은 그대로 성공한다
    private void saveAfterCommit(List<Notification> notifications) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            push(notificationRepository.saveAll(notifications));
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                List<Notification> saved;
                try {
                    TransactionTemplate template = new TransactionTemplate(transactionManager);
                    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    saved = template.execute(status -> notificationRepository.saveAll(notifications));
                } catch (RuntimeException e) {
                    log.warn("알림 저장 실패: {}개, 종류 {} ({})", notifications.size(),
                            notifications.get(0).getType(), e.getMessage());
                    return;
                }
                push(saved);
            }
        });
    }

    // 알림 없이 "바뀌었다"는 신호만 (#259). 한 사람(actorId)은 이미 화면에 반영됐으니 빼고, 원래 일이 커밋된 뒤에 보낸다.
    // DB에 남기지 않고 지금 연결된 화면에만 보낸다 (못 받은 화면은 다시 붙거나 돌아올 때 다시 불러온다)
    public void change(Collection<Long> userIds, Long actorId, LiveChange change) {
        Set<Long> targets = new LinkedHashSet<>();
        for (Long id : userIds) {
            if (id != null && !id.equals(actorId)) targets.add(id);
        }
        if (targets.isEmpty()) return;
        Runnable send = () -> targets.forEach(id -> notificationStream.sendChange(id, change));
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                send.run();
            }
        });
    }

    // 저장(커밋)된 알림만 받는 사람의 열린 화면으로
    private void push(List<Notification> saved) {
        if (saved == null) return;
        for (Notification n : saved) {
            notificationStream.send(n.getReceiver().getId(), NotificationResponse.from(n));
        }
    }

    // 실시간 연결 열기 (붙자마자 안 읽은 개수를 보낸다)
    @Transactional(readOnly = true)
    public SseEmitter subscribe(Long userId) {
        return notificationStream.subscribe(userId, notificationRepository.countByReceiverIdAndIsReadFalse(userId));
    }

    // 알림에 쓰는 이름 (캐릭터 닉네임, 없으면 User.nameWithoutCharacter)
    @Transactional(readOnly = true)
    public String nicknameOf(User user) {
        return characterRepository.findByUserId(user.getId()).map(Character::getNickname).orElseGet(user::nameWithoutCharacter);
    }

    /* ---------- 조회·읽음·지우기 ---------- */

    // category가 null이면 모든 영역, unreadOnly면 안 읽은 것만
    @Transactional(readOnly = true)
    public NotificationResponse.Page list(Long userId, int page, int size, NotificationCategory category, boolean unreadOnly) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Slice<Notification> slice = notificationRepository.findPage(userId, NotificationCategory.typesOf(category),
                unreadOnly, PageRequest.of(Math.max(page, 0), safeSize));
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

    // 고른 알림 읽음 (남의 알림 id는 무시)
    public void markRead(Long userId, Collection<Long> notificationIds) {
        notificationRepository.findAllByReceiverIdAndIdIn(userId, notificationIds).forEach(Notification::markRead);
    }

    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    public void delete(Long userId, Long notificationId) {
        notificationRepository.delete(find(userId, notificationId));
    }

    // 고른 알림 지우기 (남의 알림 id는 무시)
    public void delete(Long userId, Collection<Long> notificationIds) {
        notificationRepository.deleteAll(notificationRepository.findAllByReceiverIdAndIdIn(userId, notificationIds));
    }

    // 읽은 알림만 지우기
    public void deleteRead(Long userId) {
        notificationRepository.deleteAll(notificationRepository.findAllByReceiverIdAndIsReadTrue(userId));
    }

    // 내 알림 모두 지우기 (회원 탈퇴할 때도)
    public void deleteAll(Long userId) {
        List<Notification> mine = notificationRepository.findAllByReceiverId(userId);
        notificationRepository.deleteAll(mine);
    }

    /* ---------- 알림 설정 ---------- */

    // 항목마다 켜짐(true)·꺼짐(false), 설정 순서대로
    @Transactional(readOnly = true)
    public NotificationResponse.Settings settings(Long userId) {
        Set<NotificationSetting> off = notificationOffRepository.findAllByUserId(userId).stream()
                .map(NotificationOff::getSetting)
                .collect(Collectors.toSet());
        Map<NotificationSetting, Boolean> result = new LinkedHashMap<>();
        for (NotificationSetting setting : NotificationSetting.values()) {
            result.put(setting, !off.contains(setting));
        }
        return new NotificationResponse.Settings(result);
    }

    // 바꿀 항목만 받는다 (null 값은 그대로 둠)
    public NotificationResponse.Settings updateSettings(Long userId, Map<NotificationSetting, Boolean> changes) {
        Set<NotificationSetting> turnOn = new HashSet<>();
        Set<NotificationSetting> turnOff = new HashSet<>();
        changes.forEach((setting, on) -> {
            if (setting == null || on == null) return;
            (on ? turnOn : turnOff).add(setting);
        });
        if (!turnOn.isEmpty()) notificationOffRepository.turnOn(userId, turnOn);
        if (!turnOff.isEmpty()) {
            Set<NotificationSetting> already = notificationOffRepository.findAllByUserId(userId).stream()
                    .map(NotificationOff::getSetting)
                    .collect(Collectors.toSet());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            notificationOffRepository.saveAll(turnOff.stream()
                    .filter(setting -> !already.contains(setting))
                    .map(setting -> new NotificationOff(user, setting))
                    .toList());
        }
        return settings(userId);
    }

    // 회원 탈퇴: 알림과 알림 설정을 모두 지운다
    public void removeAllOf(Long userId) {
        deleteAll(userId);
        notificationOffRepository.deleteAllByUserId(userId);
    }

    // 매일 새벽 4시: 만든 지 30일 지난 읽은 알림을 지운다 (안 읽은 알림은 남긴다)
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void deleteOldReadNotifications() {
        LocalDateTime now = LocalDateTime.now();
        int deleted = notificationRepository.deleteReadBefore(now.minusDays(KEEP_READ_DAYS), now);
        if (deleted > 0) log.info("오래된 읽은 알림 {}개 정리", deleted);
    }

    private Notification find(Long userId, Long notificationId) {
        return notificationRepository.findByIdAndReceiverId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }
}
