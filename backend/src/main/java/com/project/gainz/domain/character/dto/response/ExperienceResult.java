package com.project.gainz.domain.character.dto.response;

import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.LevelRewards;

import java.util.List;

public record ExperienceResult(

        int previousLevel,
        int currentLevel,
        int currentExp,
        boolean isLeveledUp,
        List<NewUnlockedOutfit> unlockedOutfits,
        List<LevelRewards.Reward> rewards   // 이번에 오른 레벨들에서 받은 보상
) {
    public ExperienceResult(int previousLevel, int currentLevel, int currentExp, boolean isLeveledUp, List<NewUnlockedOutfit> unlockedOutfits) {
        this(previousLevel, currentLevel, currentExp, isLeveledUp, unlockedOutfits,
                isLeveledUp ? LevelRewards.between(previousLevel, currentLevel) : List.of());
    }

    public record NewUnlockedOutfit(Long id, String name, String imageUrl) {
        public static NewUnlockedOutfit from(OutfitItem item) {
            return new NewUnlockedOutfit(item.getId(), item.getName(), item.getImageUrl());
        }
    }
}
