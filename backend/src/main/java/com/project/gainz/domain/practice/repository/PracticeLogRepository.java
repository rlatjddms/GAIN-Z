package com.project.gainz.domain.practice.repository;

import com.project.gainz.domain.practice.dto.response.DailyPracticeResponse;
import com.project.gainz.domain.practice.entity.PracticeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PracticeLogRepository extends JpaRepository<PracticeLog, Long> {

    // 기간 안에서 기록이 있는 날의 일별 합계 (날짜 오름차순)
    @Query("""
            select new com.project.gainz.domain.practice.dto.response.DailyPracticeResponse(
                p.createdDate, coalesce(sum(p.durationSeconds), 0), coalesce(sum(p.expGained), 0))
            from PracticeLog p
            where p.user.id = :userId and p.createdDate between :from and :to
            group by p.createdDate
            order by p.createdDate
            """)
    List<DailyPracticeResponse> findDailyTotals(@Param("userId") Long userId,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to);
}
