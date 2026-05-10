package com.project.gainz.domain.sheet.repository;

import com.project.gainz.domain.sheet.entity.SheetMusic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SheetMusicRepository extends JpaRepository<SheetMusic, Long> {
}
