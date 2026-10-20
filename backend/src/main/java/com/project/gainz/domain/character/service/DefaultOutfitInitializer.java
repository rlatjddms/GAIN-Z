package com.project.gainz.domain.character.service;

import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.LevelOutfit;
import com.project.gainz.domain.character.enums.OutfitCategory;
import com.project.gainz.domain.character.repository.OutfitItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

// 서버가 뜰 때 의상 데이터를 맞춰 둔다.
// 1) 기본 의상(누구나 처음부터 가진 옷): 스프라이트 키가 DEFAULT인 의상이 없으면, 예전에 넣어 둔 Lv.1 의상을 기본 의상으로 바꾸고 그것도 없으면 새로 만든다
// 2) 레벨 보상 의상(LevelOutfit): 이름이 바뀐 예전 행은 고쳐 쓰고, 스프라이트 키로 찾아 없는 것만 만든다
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultOutfitInitializer implements ApplicationRunner {

    private final OutfitItemRepository outfitItemRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureDefaultOutfit();
        } catch (Exception e) {
            log.warn("기본 의상 맞추기 실패 ({})", e.getMessage());
        }
        try {
            ensureLevelOutfits();
        } catch (Exception e) {
            log.warn("레벨 보상 의상 맞추기 실패 ({})", e.getMessage());
        }
    }

    private void ensureDefaultOutfit() {
        if (outfitItemRepository.findFirstBySpriteKey(OutfitItem.DEFAULT_SPRITE_KEY).isPresent()) {
            return;
        }
        OutfitItem outfit = outfitItemRepository.findFirstByCategoryAndRequiredLevelOrderByIdAsc(OutfitCategory.CLOTHES, 1)
                .orElseGet(() -> OutfitItem.builder()
                        .name(OutfitItem.DEFAULT_NAME)
                        .category(OutfitCategory.CLOTHES)
                        .requiredLevel(1)
                        .spriteKey(OutfitItem.DEFAULT_SPRITE_KEY)
                        .build());
        outfit.markAsDefault();
        outfitItemRepository.save(outfit);
    }

    // 예전 이름 → 지금 레벨 의상. 예전 행을 고쳐 써서 그 옷을 입던 캐릭터도 그대로 새 옷을 입는다
    private static final Map<String, LevelOutfit> RENAMED = Map.of("SEQUIN_JACKET", LevelOutfit.GOLD_TRIM_JACKET);

    private void ensureLevelOutfits() {
        RENAMED.forEach((oldKey, outfit) -> outfitItemRepository.findFirstBySpriteKey(oldKey)
                .filter(old -> outfitItemRepository.findFirstBySpriteKey(outfit.getSpriteKey()).isEmpty())
                .ifPresent(old -> {
                    old.changeLevelReward(outfit.getLabel(), outfit.getSpriteKey(), outfit.getRequiredLevel());
                    outfitItemRepository.save(old);
                    log.info("레벨 보상 의상 이름 변경: {} → {}", oldKey, outfit.getSpriteKey());
                }));
        List<OutfitItem> missing = Arrays.stream(LevelOutfit.values())
                .filter(o -> outfitItemRepository.findFirstBySpriteKey(o.getSpriteKey()).isEmpty())
                .map(o -> OutfitItem.builder()
                        .name(o.getLabel())
                        .category(OutfitCategory.CLOTHES)
                        .requiredLevel(o.getRequiredLevel())
                        .spriteKey(o.getSpriteKey())
                        .build())
                .toList();
        if (missing.isEmpty()) return;
        outfitItemRepository.saveAll(missing);
        log.info("레벨 보상 의상 {}벌 추가", missing.size());
    }
}
