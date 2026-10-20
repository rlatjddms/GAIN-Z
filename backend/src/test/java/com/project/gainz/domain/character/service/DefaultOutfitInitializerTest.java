package com.project.gainz.domain.character.service;

import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.LevelOutfit;
import com.project.gainz.domain.character.enums.OutfitCategory;
import com.project.gainz.domain.character.repository.OutfitItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultOutfitInitializerTest {

    @Mock private OutfitItemRepository outfitItemRepository;

    @InjectMocks private DefaultOutfitInitializer initializer;

    @Test
    void 기본_의상이_이미_있으면_아무것도_하지_않는다() {
        OutfitItem existing = OutfitItem.builder().name(OutfitItem.DEFAULT_NAME).category(OutfitCategory.CLOTHES)
                .requiredLevel(1).spriteKey(OutfitItem.DEFAULT_SPRITE_KEY).build();
        when(outfitItemRepository.findFirstBySpriteKey(OutfitItem.DEFAULT_SPRITE_KEY)).thenReturn(Optional.of(existing));

        initializer.run(null);

        verify(outfitItemRepository, never()).save(any());
    }

    @Test
    void 예전에_넣어_둔_Lv1_의상을_기본_의상으로_바꾼다() {
        OutfitItem old = OutfitItem.builder().name("테스트용").category(OutfitCategory.CLOTHES).requiredLevel(1).imageUrl("e").build();
        when(outfitItemRepository.findFirstBySpriteKey(OutfitItem.DEFAULT_SPRITE_KEY)).thenReturn(Optional.empty());
        when(outfitItemRepository.findFirstByCategoryAndRequiredLevelOrderByIdAsc(OutfitCategory.CLOTHES, 1)).thenReturn(Optional.of(old));

        initializer.run(null);

        assertThat(old.getName()).isEqualTo(OutfitItem.DEFAULT_NAME);
        assertThat(old.getSpriteKey()).isEqualTo(OutfitItem.DEFAULT_SPRITE_KEY);
        verify(outfitItemRepository).save(old);
    }

    @SuppressWarnings("unchecked")
    @Test
    void 레벨_보상_의상은_없는_것만_만든다() {
        OutfitItem hoodie = OutfitItem.builder().name("후드티").category(OutfitCategory.CLOTHES).requiredLevel(5).spriteKey("HOODIE").build();
        when(outfitItemRepository.findFirstBySpriteKey(any())).thenReturn(Optional.empty());
        when(outfitItemRepository.findFirstBySpriteKey(OutfitItem.DEFAULT_SPRITE_KEY)).thenReturn(Optional.of(hoodie));
        when(outfitItemRepository.findFirstBySpriteKey("HOODIE")).thenReturn(Optional.of(hoodie));

        initializer.run(null);

        ArgumentCaptor<List<OutfitItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(outfitItemRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(OutfitItem::getSpriteKey)
                .containsExactly("CROP", "BAND_TEE", "DENIM_JACKET", "LEATHER_JACKET", "STAGE_SUIT", "GOLD_TRIM_JACKET");
        assertThat(captor.getValue()).extracting(OutfitItem::getRequiredLevel).containsExactly(7, 10, 15, 20, 25, 30);
        assertThat(LevelOutfit.values()).hasSize(7);
    }

    @Test
    void 이름이_바뀐_예전_레벨_의상은_고쳐_쓴다() {
        OutfitItem sequin = OutfitItem.builder().name("반짝이 재킷").category(OutfitCategory.CLOTHES).requiredLevel(30).spriteKey("SEQUIN_JACKET").build();
        when(outfitItemRepository.findFirstBySpriteKey(any())).thenReturn(Optional.empty());
        when(outfitItemRepository.findFirstBySpriteKey("SEQUIN_JACKET")).thenReturn(Optional.of(sequin));

        initializer.run(null);

        assertThat(sequin.getSpriteKey()).isEqualTo("GOLD_TRIM_JACKET");
        assertThat(sequin.getName()).isEqualTo("금장 재킷");
        verify(outfitItemRepository).save(sequin);
    }

    @Test
    void 의상이_하나도_없으면_기본_의상을_만든다() {
        when(outfitItemRepository.findFirstBySpriteKey(OutfitItem.DEFAULT_SPRITE_KEY)).thenReturn(Optional.empty());
        when(outfitItemRepository.findFirstByCategoryAndRequiredLevelOrderByIdAsc(OutfitCategory.CLOTHES, 1)).thenReturn(Optional.empty());

        initializer.run(null);

        ArgumentCaptor<OutfitItem> captor = ArgumentCaptor.forClass(OutfitItem.class);
        verify(outfitItemRepository).save(captor.capture());
        assertThat(captor.getValue().isDefault()).isTrue();
        assertThat(captor.getValue().getCategory()).isEqualTo(OutfitCategory.CLOTHES);
        assertThat(captor.getValue().getRequiredLevel()).isEqualTo(1);
    }
}
