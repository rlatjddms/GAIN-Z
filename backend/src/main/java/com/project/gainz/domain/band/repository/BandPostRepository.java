package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.BandPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BandPostRepository extends JpaRepository<BandPost, Long> {
}
