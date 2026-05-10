package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.Setlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SetlistRepository extends JpaRepository<Setlist, Long> {
}
