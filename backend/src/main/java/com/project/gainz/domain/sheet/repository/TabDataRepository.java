package com.project.gainz.domain.sheet.repository;

import com.project.gainz.domain.sheet.entity.TabData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TabDataRepository extends JpaRepository<TabData, Long> {

    List<TabData> findAllBySheetMusicIdOrderByMeasureNumberAsc(Long sheetId);

    void deleteAllBySheetMusicId(Long sheetId);
}
