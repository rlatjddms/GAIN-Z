package com.project.gainz.domain.character.repository;

import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.OutfitCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutfitItemRepository extends JpaRepository<OutfitItem, Long> {

    Optional<OutfitItem> findByCategoryAndRequiredLevel(OutfitCategory category, Integer requiredLevel);

    // 옷장 목록: 해금 레벨 순
    List<OutfitItem> findAllByOrderByRequiredLevelAscIdAsc();
    @Query("SELECT o FROM OutfitItem o WHERE o.requiredLevel > :prevLevel AND o.requiredLevel <= :currentLevel")
    List<OutfitItem> findUnlockableItems(@Param("prevLevel") int prevLevel, @Param("currentLevel") int currentLevel);
}
