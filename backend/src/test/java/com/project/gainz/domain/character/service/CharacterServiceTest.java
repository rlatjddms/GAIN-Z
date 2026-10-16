package com.project.gainz.domain.character.service;

import com.project.gainz.domain.character.dto.request.WardrobeUpdateRequest;
import com.project.gainz.domain.character.dto.response.CharacterResponse;
import com.project.gainz.domain.character.dto.response.ExperienceResult;
import com.project.gainz.domain.character.dto.response.WardrobeResponse;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.entity.UserOutfit;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.enums.OutfitCategory;
import com.project.gainz.domain.character.enums.StageBackground;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.character.repository.OutfitItemRepository;
import com.project.gainz.domain.character.repository.UserOutfitRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.common.enums.InstrumentType;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

    private static final Long USER_ID = 1L;

    @Mock private CharacterRepository characterRepository;
    @Mock private OutfitItemRepository outfitItemRepository;
    @Mock private UserOutfitRepository userOutfitRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CharacterService characterService;

    private User user;
    private Character character;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        character = Character.builder()
                .user(user)
                .nickname("tester")
                .instrumentType(InstrumentType.values()[0])
                .instrumentColor("#000000")
                .gender(Gender.values()[0])
                .hairStyle(HairStyle.values()[0])
                .hairColor("#000000")
                .currentOutfit(mock(OutfitItem.class))
                .build();
    }

    @Test
    void 캐릭터가_없으면_예외가_발생한다() {
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> characterService.processExperienceAndUnlocks(USER_ID, 10, 1))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHARACTER_NOT_FOUND);
    }

    @Test
    void 레벨업하지_않으면_경험치와_연습_시간만_누적된다() {
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));

        ExperienceResult result = characterService.processExperienceAndUnlocks(USER_ID, 40, 30);

        assertThat(result.isLeveledUp()).isFalse();
        assertThat(result.previousLevel()).isEqualTo(1);
        assertThat(result.currentLevel()).isEqualTo(1);
        assertThat(result.currentExp()).isEqualTo(40);
        assertThat(result.unlockedOutfits()).isEmpty();
        assertThat(character.getTotalPracticeTime()).isEqualTo(30L);
        verify(outfitItemRepository, never()).findUnlockableItems(anyInt(), anyInt());
        verify(userOutfitRepository, never()).save(any());
    }

    @Test
    void 레벨업하면_새로_해금되는_의상을_지급한다() {
        OutfitItem newItem = mock(OutfitItem.class);
        when(newItem.getId()).thenReturn(10L);
        when(newItem.getName()).thenReturn("록스타 재킷");
        when(newItem.getImageUrl()).thenReturn("https://example.com/jacket.png");

        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        when(outfitItemRepository.findUnlockableItems(1, 2)).thenReturn(List.of(newItem));
        when(userOutfitRepository.existsByUserAndOutfitItem(user, newItem)).thenReturn(false);

        ExperienceResult result = characterService.processExperienceAndUnlocks(USER_ID, 100, 60);

        assertThat(result.isLeveledUp()).isTrue();
        assertThat(result.previousLevel()).isEqualTo(1);
        assertThat(result.currentLevel()).isEqualTo(2);
        assertThat(result.unlockedOutfits())
                .extracting(ExperienceResult.NewUnlockedOutfit::id, ExperienceResult.NewUnlockedOutfit::name)
                .containsExactly(Tuple.tuple(10L, "록스타 재킷"));
        verify(userOutfitRepository, times(1)).save(any(UserOutfit.class));
    }

    @Test
    void 이미_보유한_의상은_다시_지급하지_않는다() {
        OutfitItem ownedItem = mock(OutfitItem.class);

        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        when(outfitItemRepository.findUnlockableItems(1, 2)).thenReturn(List.of(ownedItem));
        when(userOutfitRepository.existsByUserAndOutfitItem(user, ownedItem)).thenReturn(true);

        ExperienceResult result = characterService.processExperienceAndUnlocks(USER_ID, 100, 60);

        assertThat(result.isLeveledUp()).isTrue();
        assertThat(result.unlockedOutfits()).isEmpty();
        verify(userOutfitRepository, never()).save(any());
    }

    @Test
    void 여러_레벨을_한_번에_올리면_건너뛴_레벨의_의상도_조회한다() {
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        when(outfitItemRepository.findUnlockableItems(1, 3)).thenReturn(List.of());

        ExperienceResult result = characterService.processExperienceAndUnlocks(USER_ID, 250, 60);

        assertThat(result.currentLevel()).isEqualTo(3);
        verify(outfitItemRepository).findUnlockableItems(1, 3);
    }

    // ---------- 옷장 ----------

    private OutfitItem outfit(Long id, String name, OutfitCategory category, int requiredLevel) {
        OutfitItem item = mock(OutfitItem.class);
        org.mockito.Mockito.lenient().when(item.getId()).thenReturn(id);
        org.mockito.Mockito.lenient().when(item.getName()).thenReturn(name);
        org.mockito.Mockito.lenient().when(item.getCategory()).thenReturn(category);
        org.mockito.Mockito.lenient().when(item.getRequiredLevel()).thenReturn(requiredLevel);
        return item;
    }

    @Test
    void 옷장은_보유_착용_여부와_레벨에_따른_배경_해금을_보여_준다() {
        OutfitItem basic = outfit(1L, "기본 티셔츠", OutfitCategory.CLOTHES, 1);
        OutfitItem jacket = outfit(2L, "가죽 재킷", OutfitCategory.CLOTHES, 10);
        OutfitItem bg = outfit(3L, "배경 아이템", OutfitCategory.BACKGROUND, 1);
        character.changeOutfit(basic);
        ReflectionTestUtils.setField(character, "level", 12);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        when(userOutfitRepository.findOutfitItemIdsByUserId(USER_ID)).thenReturn(List.of(1L));
        when(outfitItemRepository.findAllByOrderByRequiredLevelAscIdAsc()).thenReturn(List.of(basic, jacket, bg));

        WardrobeResponse result = characterService.getWardrobe(USER_ID);

        // 배경 카테고리 아이템은 의상 목록에서 뺀다
        assertThat(result.outfits()).extracting(WardrobeResponse.OutfitView::id, WardrobeResponse.OutfitView::owned, WardrobeResponse.OutfitView::equipped)
                .containsExactly(Tuple.tuple(1L, true, true), Tuple.tuple(2L, false, false));
        assertThat(result.backgrounds()).extracting(WardrobeResponse.BackgroundView::key, WardrobeResponse.BackgroundView::unlocked)
                .containsExactly(
                        Tuple.tuple(StageBackground.ROOKIE, true), Tuple.tuple(StageBackground.RISING, true),
                        Tuple.tuple(StageBackground.PRO, false), Tuple.tuple(StageBackground.LEGEND, false));
        assertThat(result.stageBackground()).isNull();
    }

    @Test
    void 보유한_의상으로_갈아입고_해금한_배경을_고른다() {
        OutfitItem jacket = outfit(2L, "가죽 재킷", OutfitCategory.CLOTHES, 10);
        ReflectionTestUtils.setField(character, "level", 12);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        when(userOutfitRepository.existsByUserIdAndOutfitItemId(USER_ID, 2L)).thenReturn(true);
        when(outfitItemRepository.findById(2L)).thenReturn(Optional.of(jacket));

        CharacterResponse result = characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(2L, StageBackground.RISING));

        assertThat(character.getCurrentOutfit()).isSameAs(jacket);
        assertThat(result.currentOutfitId()).isEqualTo(2L);
        assertThat(result.stageBackground()).isEqualTo(StageBackground.RISING);
    }

    @Test
    void 배경을_비우면_레벨에_맞춰_자동으로_돌아간다() {
        OutfitItem basic = outfit(1L, "기본 티셔츠", OutfitCategory.CLOTHES, 1);
        character.changeStageBackground(StageBackground.ROOKIE);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        when(userOutfitRepository.existsByUserIdAndOutfitItemId(USER_ID, 1L)).thenReturn(true);
        when(outfitItemRepository.findById(1L)).thenReturn(Optional.of(basic));

        characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(1L, null));

        assertThat(character.getStageBackground()).isNull();
    }

    @Test
    void 가지고_있지_않은_의상은_입을_수_없다() {
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        when(userOutfitRepository.existsByUserIdAndOutfitItemId(USER_ID, 2L)).thenReturn(false);

        assertThatThrownBy(() -> characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(2L, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUTFIT_NOT_OWNED);
    }

    @Test
    void 해금하지_않은_배경은_고를_수_없다() {
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));   // 레벨 1
        when(userOutfitRepository.existsByUserIdAndOutfitItemId(USER_ID, 1L)).thenReturn(true);

        assertThatThrownBy(() -> characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(1L, StageBackground.RISING)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAGE_BACKGROUND_LOCKED);
        assertThat(character.getStageBackground()).isNull();
    }
}
