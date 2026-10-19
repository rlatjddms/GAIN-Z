package com.project.gainz.domain.notification.repository;

import com.project.gainz.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 내 알림 (최근 순)
    Slice<Notification> findAllByReceiverIdOrderByIdDesc(Long receiverId, Pageable pageable);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);

    List<Notification> findAllByReceiverId(Long receiverId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.receiver.id = :receiverId and n.isRead = false and n.isDeleted = false")
    int markAllRead(@Param("receiverId") Long receiverId);
}
