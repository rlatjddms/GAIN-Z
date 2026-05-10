package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.BandApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BandApplicationRepository extends JpaRepository<BandApplication, Long> {
}
