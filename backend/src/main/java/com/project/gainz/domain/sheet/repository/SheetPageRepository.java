package com.project.gainz.domain.sheet.repository;

import com.project.gainz.domain.sheet.entity.SheetPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SheetPageRepository extends JpaRepository<SheetPage, Long> {

    List<SheetPage> findAllBySheetMusicIdOrderByPageNumberAsc(Long sheetId);

    // 보관함 목록에서 악보마다 장 수·첫 장을 한 번에 구한다
    List<SheetPage> findAllBySheetMusicIdInOrderByPageNumberAsc(Collection<Long> sheetIds);

    Optional<SheetPage> findByIdAndSheetMusicId(Long id, Long sheetId);
}
