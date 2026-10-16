package com.project.gainz.domain.character.dto.response;

import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.OutfitCategory;
import com.project.gainz.domain.character.enums.StageBackground;

import java.util.List;

// 옷장: 전체 의상(보유·착용 여부)과 무대 배경(해금 여부), 지금 고른 배경 (null = 레벨에 맞춰 자동)
public record WardrobeResponse(
        List<OutfitView> outfits,
        List<BackgroundView> backgrounds,
        StageBackground stageBackground
) {
    public record OutfitView(Long id, String name, OutfitCategory category, Integer requiredLevel,
                             String imageUrl, boolean owned, boolean equipped) {
        public static OutfitView of(OutfitItem item, boolean owned, boolean equipped) {
            return new OutfitView(item.getId(), item.getName(), item.getCategory(), item.getRequiredLevel(),
                    item.getImageUrl(), owned, equipped);
        }
    }

    public record BackgroundView(StageBackground key, int requiredLevel, boolean unlocked) {
    }
}
