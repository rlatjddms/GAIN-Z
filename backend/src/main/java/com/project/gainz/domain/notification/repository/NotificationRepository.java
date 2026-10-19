package com.project.gainz.domain.notification.repository;

import com.project.gainz.domain.notification.entity.Notification;
import com.project.gainz.domain.notification.enums.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 내 알림 (최근 순). types로 영역을 거르고, unreadOnly면 안 읽은 것만
    @Query("select n from Notification n where n.receiver.id = :receiverId and n.type in :types"
            + " and (:unreadOnly = false or n.isRead = false) order by n.id desc")
    Slice<Notification> findPage(@Param("receiverId") Long receiverId, @Param("types") Collection<NotificationType> types,
                                 @Param("unreadOnly") boolean unreadOnly, Pageable pageable);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);

    // 고른 알림 중 내 것만
    List<Notification> findAllByReceiverIdAndIdIn(Long receiverId, Collection<Long> ids);

    List<Notification> findAllByReceiverId(Long receiverId);

    List<Notification> findAllByReceiverIdAndIsReadTrue(Long receiverId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.receiver.id = :receiverId and n.isRead = false and n.isDeleted = false")
    int markAllRead(@Param("receiverId") Long receiverId);

    // 오래된 읽은 알림 정리 (소프트 삭제)
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isDeleted = true, n.deletedAt = :now"
            + " where n.isRead = true and n.isDeleted = false and n.createdAt < :before")
    int deleteReadBefore(@Param("before") LocalDateTime before, @Param("now") LocalDateTime now);
}
