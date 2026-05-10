package com.project.gainz.domain.character.repository;

import com.project.gainz.domain.character.entity.OutfitItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutfitItemRepository extends JpaRepository<OutfitItem, Long> {
}
