package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.BandMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BandMemberRepository extends JpaRepository<BandMember, Long> {

    Optional<BandMember> findByBandIdAndUserId(Long bandId, Long userId);

    Optional<BandMember> findByIdAndBandId(Long id, Long bandId);

    // 내가 들어간 밴드 (들어간 순서)
    List<BandMember> findAllByUserIdOrderByIdAsc(Long userId);

    List<BandMember> findAllByBandIdOrderByIdAsc(Long bandId);

    List<BandMember> findAllByBandIdInOrderByIdAsc(Collection<Long> bandIds);

    long countByUserId(Long userId);
}
