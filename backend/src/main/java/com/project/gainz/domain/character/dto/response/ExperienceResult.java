package com.project.gainz.domain.character.dto.response;

import com.project.gainz.domain.character.entity.OutfitItem;

import java.util.List;

public record ExperienceResult(

        int previousLevel,
        int currentLevel,
        int currentExp,
        boolean isLeveledUp,
        List<NewUnlockedOutfit> unlockedOutfits
) {
    public record NewUnlockedOutfit(Long id, String name, String imageUrl) {
        public static NewUnlockedOutfit from(OutfitItem item) {
            return new NewUnlockedOutfit(item.getId(), item.getName(), item.getImageUrl());
        }
    }
}
