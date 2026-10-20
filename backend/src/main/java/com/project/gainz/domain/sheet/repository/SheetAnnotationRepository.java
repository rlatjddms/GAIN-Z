package com.project.gainz.domain.sheet.repository;

import com.project.gainz.domain.sheet.entity.SheetAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SheetAnnotationRepository extends JpaRepository<SheetAnnotation, Long> {

    // 이 악보의 내 필기 전부
    List<SheetAnnotation> findAllByUserIdAndSheetPageSheetMusicId(Long userId, Long sheetId);

    Optional<SheetAnnotation> findByUserIdAndSheetPageIdAndPdfPage(Long userId, Long pageId, Integer pdfPage);

    // 장을 지울 때 그 장에 쓴 모든 사람의 필기도 지운다
    void deleteAllBySheetPageIdIn(Collection<Long> pageIds);
}
