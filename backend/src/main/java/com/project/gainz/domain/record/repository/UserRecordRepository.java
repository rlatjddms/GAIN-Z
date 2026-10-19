package com.project.gainz.domain.record.repository;

import com.project.gainz.domain.record.entity.UserRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRecordRepository extends JpaRepository<UserRecord, Long> {

    List<UserRecord> findAllByUserId(Long userId);

    @Modifying
    @Query("delete from UserRecord r where r.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
