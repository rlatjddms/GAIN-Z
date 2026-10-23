package com.project.gainz.domain.notification.repository;

import com.project.gainz.domain.notification.entity.NotificationOff;
import com.project.gainz.domain.notification.enums.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NotificationOffRepository extends JpaRepository<NotificationOff, Long> {

    List<NotificationOff> findAllByUserId(Long userId);

    // 받을 사람 중 이 항목을 꺼 둔 사람
    @Query("select o.user.id from NotificationOff o where o.setting = :setting and o.user.id in :userIds")
    List<Long> findUserIdsTurnedOff(@Param("setting") NotificationSetting setting, @Param("userIds") Collection<Long> userIds);

    @Modifying
    @Query("delete from NotificationOff o where o.user.id = :userId and o.setting in :settings")
    int turnOn(@Param("userId") Long userId, @Param("settings") Collection<NotificationSetting> settings);

    @Modifying
    @Query("delete from NotificationOff o where o.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
