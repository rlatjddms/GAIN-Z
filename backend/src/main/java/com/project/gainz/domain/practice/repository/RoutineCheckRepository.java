package com.project.gainz.domain.practice.repository;

import com.project.gainz.domain.practice.entity.RoutineCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoutineCheckRepository extends JpaRepository<RoutineCheck, Long> {

    List<RoutineCheck> findAllByRoutineIdInAndPracticeDate(Collection<Long> routineIds, LocalDate practiceDate);

    Optional<RoutineCheck> findByRoutineIdAndPracticeDate(Long routineId, LocalDate practiceDate);

    List<RoutineCheck> findAllByRoutineIdInAndPracticeDateBetween(Collection<Long> routineIds, LocalDate from, LocalDate to);
}
