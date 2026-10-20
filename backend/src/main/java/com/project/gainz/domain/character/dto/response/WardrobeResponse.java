package com.project.gainz.domain.character.dto.response;

import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.OutfitCategory;
import com.project.gainz.domain.character.enums.RoomDecor;
import com.project.gainz.domain.character.enums.StageBackground;

import java.util.List;

// 옷장: 전체 아이템(보유·착용 여부, 카테고리별로 프론트에서 나눔)과 무대 배경(해금 여부), 지금 고른 배경
public record WardrobeResponse(
        List<OutfitView> outfits,
        List<BackgroundView> backgrounds,
        StageBackground stageBackground,
        List<DecorSetView> decorSets       // 방 꾸미기 세트와 잠금 여부 (레코드 수집 보상)
) {
    public record DecorSetView(RoomDecor key, boolean unlocked) {
    }

    public record OutfitView(Long id, String name, OutfitCategory category, Integer requiredLevel,
                             String imageUrl, String spriteKey, boolean owned, boolean equipped) {
        public static OutfitView of(OutfitItem item, boolean owned, boolean equipped) {
            return new OutfitView(item.getId(), item.getName(), item.getCategory(), item.getRequiredLevel(),
                    item.getImageUrl(), item.getSpriteKey(), owned, equipped);
        }
    }

    public record BackgroundView(StageBackground key, int requiredLevel, boolean unlocked) {
    }
}
