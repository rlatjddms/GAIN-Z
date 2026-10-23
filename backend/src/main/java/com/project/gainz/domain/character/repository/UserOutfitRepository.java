package com.project.gainz.domain.character.repository;

import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.entity.UserOutfit;
import com.project.gainz.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserOutfitRepository extends JpaRepository<UserOutfit, Long> {

    boolean existsByUserAndOutfitItem(User user, OutfitItem outfitItem);

    boolean existsByUserIdAndOutfitItemId(Long userId, Long outfitItemId);

    // 보유한 의상 id 목록
    @Query("SELECT uo.outfitItem.id FROM UserOutfit uo WHERE uo.user.id = :userId")
    List<Long> findOutfitItemIdsByUserId(@Param("userId") Long userId);
}
