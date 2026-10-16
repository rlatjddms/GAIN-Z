package com.project.gainz.domain.character.service;

import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.OutfitCategory;
import com.project.gainz.domain.character.repository.OutfitItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        verify(outfitItemRepository, never()).save(any());
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
