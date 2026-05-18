package com.project.gainz.domain.character.repository;

import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.OutfitCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OutfitItemRepository extends JpaRepository<OutfitItem, Long> {

    Optional<OutfitItem> findByCategoryAndRequiredLevel(OutfitCategory category, Integer requiredLevel);
}
