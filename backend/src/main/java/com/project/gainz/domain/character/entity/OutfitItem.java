package com.project.gainz.domain.character.entity;

import com.project.gainz.domain.character.enums.OutfitCategory;
import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutfitItem extends BaseEntity {

    // 처음부터 가지고 있는 기본 의상
    public static final String DEFAULT_NAME = "기본 의상";
    public static final String DEFAULT_SPRITE_KEY = "DEFAULT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OutfitCategory category;

    @Column(nullable = false)
    private Integer requiredLevel;

    @Column(nullable = false)
    private String imageUrl;

    // 프론트 스프라이트 이름 (frontend/src/assets/sprites/outfit/<키>_<MALE|FEMALE>.png). 성별마다 다른 그림을 쓴다
    @Column(length = 40)
    private String spriteKey;

    @Builder
    public OutfitItem(String name, OutfitCategory category, Integer requiredLevel, String imageUrl, String spriteKey) {
        this.name = name;
        this.category = category;
        this.requiredLevel = requiredLevel;
        this.imageUrl = imageUrl != null ? imageUrl : "";
        this.spriteKey = spriteKey;
    }

    // 기본 의상 데이터 맞추기 (초기 데이터용)
    public void markAsDefault() {
        this.name = DEFAULT_NAME;
        this.spriteKey = DEFAULT_SPRITE_KEY;
    }

    // 레벨 보상 의상의 이름·그림이 바뀌었을 때 (예: SEQUIN_JACKET → GOLD_TRIM_JACKET)
    public void changeLevelReward(String name, String spriteKey, int requiredLevel) {
        this.name = name;
        this.spriteKey = spriteKey;
        this.requiredLevel = requiredLevel;
    }

    public boolean isDefault() {
        return DEFAULT_NAME.equals(name) && DEFAULT_SPRITE_KEY.equals(spriteKey);
    }
}
