package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.JamTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JamTrackRepository extends JpaRepository<JamTrack, Long> {
}
