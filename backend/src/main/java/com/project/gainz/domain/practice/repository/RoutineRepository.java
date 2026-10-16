package com.project.gainz.domain.practice.repository;

import com.project.gainz.domain.practice.entity.Routine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoutineRepository extends JpaRepository<Routine, Long> {

    List<Routine> findAllByUserIdAndIsActiveTrueOrderByIdAsc(Long userId);

    long countByUserIdAndIsActiveTrue(Long userId);

    // 본인 루틴만 찾는다 (다른 사람 루틴은 없는 것으로 취급)
    Optional<Routine> findByIdAndUserIdAndIsActiveTrue(Long id, Long userId);
}
