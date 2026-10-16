package com.project.gainz.domain.sheet.repository;

import com.project.gainz.domain.sheet.entity.SheetMusic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SheetMusicRepository extends JpaRepository<SheetMusic, Long> {

    // 공용 악보 (만든 사람이 없는 악보)
    List<SheetMusic> findAllByOwnerIsNullOrderByTitleAsc();
}
