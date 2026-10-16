package com.project.gainz.domain.character.service;

import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.OutfitCategory;
import com.project.gainz.domain.character.repository.OutfitItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 서버가 뜰 때 기본 의상(누구나 처음부터 가진 옷)을 맞춰 둔다.
// 스프라이트 키가 DEFAULT인 의상이 없으면, 예전에 넣어 둔 Lv.1 의상을 기본 의상으로 바꾸고 그것도 없으면 새로 만든다
@Component
@RequiredArgsConstructor
public class DefaultOutfitInitializer implements ApplicationRunner {

    private final OutfitItemRepository outfitItemRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (outfitItemRepository.findFirstBySpriteKey(OutfitItem.DEFAULT_SPRITE_KEY).isPresent()) {
            return;
        }
        outfitItemRepository.findFirstByCategoryAndRequiredLevelOrderByIdAsc(OutfitCategory.CLOTHES, 1)
                .ifPresentOrElse(
                        OutfitItem::markAsDefault,
                        () -> outfitItemRepository.save(OutfitItem.builder()
                                .name(OutfitItem.DEFAULT_NAME)
                                .category(OutfitCategory.CLOTHES)
                                .requiredLevel(1)
                                .spriteKey(OutfitItem.DEFAULT_SPRITE_KEY)
                                .build()));
    }
}
