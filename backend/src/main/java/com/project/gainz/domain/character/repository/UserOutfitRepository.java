package com.project.gainz.domain.character.repository;

import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.entity.UserOutfit;
import com.project.gainz.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOutfitRepository extends JpaRepository<UserOutfit, Long> {

    boolean existsByUserAndOutfitItem(User user, OutfitItem outfitItem);
}
