package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.Setlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SetlistRepository extends JpaRepository<Setlist, Long> {

    // 목록 순서가 비어 있는 예전 셋리스트는 만든 순서로 뒤에 둔다
    List<Setlist> findAllByBandIdOrderByListOrderAscIdAsc(Long bandId);

    Optional<Setlist> findByIdAndBandId(Long id, Long bandId);

    long countByBandId(Long bandId);
}
