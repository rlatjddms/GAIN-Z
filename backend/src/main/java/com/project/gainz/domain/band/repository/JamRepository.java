package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.Jam;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JamRepository extends JpaRepository<Jam, Long> {

    // 목록 카드에 트랙 수를 보여 주려고 트랙을 함께 읽는다
    @EntityGraph(attributePaths = "tracks")
    List<Jam> findAllByBandIdOrderByIdDesc(Long bandId);

    Optional<Jam> findByIdAndBandId(Long id, Long bandId);

    long countByBandId(Long bandId);
}
