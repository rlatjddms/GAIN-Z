package com.project.gainz.domain.character.service;

import com.project.gainz.domain.character.dto.response.ExperienceResult;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.entity.UserOutfit;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
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
}
