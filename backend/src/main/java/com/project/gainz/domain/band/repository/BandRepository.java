package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.Band;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BandRepository extends JpaRepository<Band, Long> {

    Optional<Band> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
