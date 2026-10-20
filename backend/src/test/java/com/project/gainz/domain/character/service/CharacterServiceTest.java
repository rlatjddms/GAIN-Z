package com.project.gainz.domain.character.service;

import com.project.gainz.domain.character.dto.request.CharacterUpdateRequest;
import com.project.gainz.domain.character.dto.request.WardrobeUpdateRequest;
import com.project.gainz.domain.character.dto.response.CharacterResponse;
import com.project.gainz.domain.character.dto.response.ExperienceResult;
import com.project.gainz.domain.character.dto.response.WardrobeResponse;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.entity.UserOutfit;
import com.project.gainz.domain.character.enums.Accessory;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.enums.LevelRewards;
import com.project.gainz.domain.character.enums.OutfitCategory;
import com.project.gainz.domain.character.enums.StageBackground;
import com.project.gainz.domain.character.enums.StageEffect;
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
import java.util.Map;
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
    @Mock private com.project.gainz.domain.record.repository.UserRecordRepository userRecordRepository;

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
    void 음수_경험치는_되돌리기만_하고_레벨업_판정을_하지_않는다() {
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        characterService.processExperienceAndUnlocks(USER_ID, 40, 40);

        ExperienceResult result = characterService.processExperienceAndUnlocks(USER_ID, -60, -60);

        assertThat(result.isLeveledUp()).isFalse();
        assertThat(result.currentLevel()).isEqualTo(1);
        assertThat(result.currentExp()).isEqualTo(-20);
        assertThat(character.getTotalPracticeTime()).isZero();
        verify(outfitItemRepository, never()).findUnlockableItems(anyInt(), anyInt());
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
        // Lv.2 운동화, Lv.3 비니
        assertThat(result.rewards()).extracting(LevelRewards.Reward::level, LevelRewards.Reward::key)
                .containsExactly(Tuple.tuple(2, "SNEAKERS"), Tuple.tuple(3, "BEANIE"));
    }

    @Test
    void 레벨업하지_않으면_받은_보상이_없다() {
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));

        assertThat(characterService.processExperienceAndUnlocks(USER_ID, 10, 10).rewards()).isEmpty();
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
        OutfitItem jacket = outfit(2L, "가죽 재킷", OutfitCategory.CLOTHES, 15);
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
                        Tuple.tuple(StageBackground.PRO, false), Tuple.tuple(StageBackground.LEGEND, false),
                        Tuple.tuple(StageBackground.LISTENING_ROOM, false));
        // 새 캐릭터는 방구석으로 시작하고, 레벨이 올라도 고른 배경은 그대로
        assertThat(result.stageBackground()).isEqualTo(StageBackground.ROOKIE);
    }

    @Test
    void 레벨이_닿은_보상_의상은_받은_기록이_없어도_가진_것으로_보인다() {
        OutfitItem hoodie = outfit(5L, "후드티", OutfitCategory.CLOTHES, 5);
        ReflectionTestUtils.setField(character, "level", 7);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        when(userOutfitRepository.findOutfitItemIdsByUserId(USER_ID)).thenReturn(List.of());
        when(outfitItemRepository.findAllByOrderByRequiredLevelAscIdAsc()).thenReturn(List.of(hoodie));

        assertThat(characterService.getWardrobe(USER_ID).outfits()).extracting(WardrobeResponse.OutfitView::owned).containsExactly(true);
    }

    @Test
    void 배경을_고른_적_없는_예전_캐릭터는_레벨로_얻은_가장_큰_무대가_보인다() {
        ReflectionTestUtils.setField(character, "stageBackground", null);
        ReflectionTestUtils.setField(character, "level", 12);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));

        assertThat(characterService.getCharacter(USER_ID).stageBackground()).isEqualTo(StageBackground.RISING);
    }

    @Test
    void 보유한_의상으로_갈아입고_해금한_배경을_고른다() {
        OutfitItem jacket = outfit(2L, "가죽 재킷", OutfitCategory.CLOTHES, 10);
        ReflectionTestUtils.setField(character, "level", 12);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        when(outfitItemRepository.findById(2L)).thenReturn(Optional.of(jacket));

        CharacterResponse result = characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(2L, StageBackground.RISING));

        assertThat(character.getCurrentOutfit()).isSameAs(jacket);
        assertThat(result.currentOutfitId()).isEqualTo(2L);
        assertThat(result.stageBackground()).isEqualTo(StageBackground.RISING);
    }

    @Test
    void 레벨이_올라도_예전에_얻은_배경으로_바꿀_수_있다() {
        OutfitItem basic = outfit(1L, "기본 의상", OutfitCategory.CLOTHES, 1);
        ReflectionTestUtils.setField(character, "level", 35);
        character.changeStageBackground(StageBackground.PRO);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        when(outfitItemRepository.findById(1L)).thenReturn(Optional.of(basic));

        characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(1L, StageBackground.ROOKIE));

        assertThat(character.getCurrentStageBackground()).isEqualTo(StageBackground.ROOKIE);
    }

    @Test
    void 가지고_있지_않은_의상은_입을_수_없다() {
        OutfitItem jacket = outfit(2L, "가죽 재킷", OutfitCategory.CLOTHES, 20);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));   // 레벨 1
        when(outfitItemRepository.findById(2L)).thenReturn(Optional.of(jacket));
        when(userOutfitRepository.existsByUserIdAndOutfitItemId(USER_ID, 2L)).thenReturn(false);

        assertThatThrownBy(() -> characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(2L, StageBackground.ROOKIE)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUTFIT_NOT_OWNED);
    }

    @Test
    void 해금하지_않은_배경은_고를_수_없다() {
        OutfitItem basic = outfit(1L, "기본 의상", OutfitCategory.CLOTHES, 1);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));   // 레벨 1
        when(outfitItemRepository.findById(1L)).thenReturn(Optional.of(basic));

        assertThatThrownBy(() -> characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(1L, StageBackground.RISING)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAGE_BACKGROUND_LOCKED);
        assertThat(character.getCurrentStageBackground()).isEqualTo(StageBackground.ROOKIE);
    }

    // ---------- 소품 ----------

    private void givenBasicOutfit() {
        OutfitItem basic = outfit(1L, "기본 의상", OutfitCategory.CLOTHES, 1);
        when(outfitItemRepository.findById(1L)).thenReturn(Optional.of(basic));
    }

    @Test
    void 레벨이_닿은_소품을_자리마다_쓰고_null이면_벗는다() {
        ReflectionTestUtils.setField(character, "level", 10);
        character.wear(com.project.gainz.domain.character.enums.AccessorySlot.NECK, Accessory.HEADPHONES);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        givenBasicOutfit();

        CharacterResponse result = characterService.updateWardrobe(USER_ID,
                new WardrobeUpdateRequest(1L, StageBackground.ROOKIE, Accessory.CAP, Accessory.ROUND_GLASSES, null, Accessory.SNEAKERS));

        assertThat(result.hat()).isEqualTo(Accessory.CAP);
        assertThat(result.glasses()).isEqualTo(Accessory.ROUND_GLASSES);
        assertThat(result.neck()).isNull();
        assertThat(result.shoes()).isEqualTo(Accessory.SNEAKERS);
    }

    @Test
    void 레벨이_닿은_하의를_입고_비우면_기본_청바지와_신발() {
        ReflectionTestUtils.setField(character, "level", 15);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        givenBasicOutfit();

        CharacterResponse worn = characterService.updateWardrobe(USER_ID,
                new WardrobeUpdateRequest(1L, StageBackground.ROOKIE, null, null, null, null, Accessory.CARGO_PANTS));
        assertThat(worn.bottom()).isEqualTo(Accessory.CARGO_PANTS);

        assertThatThrownBy(() -> characterService.updateWardrobe(USER_ID,
                new WardrobeUpdateRequest(1L, StageBackground.ROOKIE, null, null, null, null, Accessory.LEATHER_PANTS)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESSORY_LOCKED);

        CharacterResponse basic = characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(1L, StageBackground.ROOKIE));
        assertThat(basic.bottom()).isEqualTo(Accessory.JEANS);
        assertThat(basic.shoes()).isEqualTo(Accessory.BASIC_SHOES);
        assertThat(character.getBottom()).isEqualTo(Accessory.JEANS);
    }

    @Test
    void 귀걸이와_무대_효과를_고르고_null이면_끈다() {
        ReflectionTestUtils.setField(character, "level", 16);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        givenBasicOutfit();

        CharacterResponse on = characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(
                1L, StageBackground.RISING, null, null, null, null, null, Accessory.STUD_EARRINGS, StageEffect.SMOKE));
        assertThat(on.earrings()).isEqualTo(Accessory.STUD_EARRINGS);
        assertThat(on.stageEffect()).isEqualTo(StageEffect.SMOKE);

        CharacterResponse off = characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(1L, StageBackground.RISING));
        assertThat(off.earrings()).isNull();
        assertThat(off.stageEffect()).isNull();
    }

    private WardrobeUpdateRequest withColors(Map<String, String> colors) {
        return new WardrobeUpdateRequest(1L, StageBackground.ROOKIE, null, null, null, null, null, null, null, null, colors);
    }

    @Test
    void 아이템마다_고른_색을_저장하고_null이면_그대로_둔다() {
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        givenBasicOutfit();

        CharacterResponse saved = characterService.updateWardrobe(USER_ID, withColors(Map.of("HOODIE", "#ffd8a8", "JEANS", "#212529", "DEFAULT", "#343A40")));
        assertThat(saved.itemColors()).containsEntry("HOODIE", "#ffd8a8").containsEntry("JEANS", "#212529").hasSize(3);

        CharacterResponse kept = characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(1L, StageBackground.ROOKIE));
        assertThat(kept.itemColors()).hasSize(3);

        assertThat(characterService.updateWardrobe(USER_ID, withColors(Map.of())).itemColors()).isEmpty();
    }

    @Test
    void 모르는_아이템이나_색_형식이_틀리면_거절한다() {
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        givenBasicOutfit();

        assertThatThrownBy(() -> characterService.updateWardrobe(USER_ID, withColors(Map.of("UNKNOWN", "#ffffff"))))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_COLOR_INVALID);
        assertThatThrownBy(() -> characterService.updateWardrobe(USER_ID, withColors(Map.of("HOODIE", "red"))))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_COLOR_INVALID);
        assertThat(character.getItemColors()).isEmpty();
    }

    @Test
    void 악기를_내려놓고_다시_들_수_있다() {
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        givenBasicOutfit();
        assertThat(characterService.getCharacter(USER_ID).instrumentHidden()).isFalse();

        WardrobeUpdateRequest hide = new WardrobeUpdateRequest(1L, StageBackground.ROOKIE, null, null, null, null, null, null, null, null, null, true);
        assertThat(characterService.updateWardrobe(USER_ID, hide).instrumentHidden()).isTrue();
        // 값을 안 보내면 그대로
        assertThat(characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(1L, StageBackground.ROOKIE)).instrumentHidden()).isTrue();
        // 악기 종류는 바뀌지 않는다
        assertThat(character.getInstrumentType()).isEqualTo(InstrumentType.values()[0]);

        WardrobeUpdateRequest show = new WardrobeUpdateRequest(1L, StageBackground.ROOKIE, null, null, null, null, null, null, null, null, null, false);
        assertThat(characterService.updateWardrobe(USER_ID, show).instrumentHidden()).isFalse();
    }

    // ---------- 레코드 수집 보상 ----------

    private void givenRecords(com.project.gainz.domain.record.enums.RecordRarity... rarities) {
        java.util.Set<com.project.gainz.domain.record.enums.RecordRarity> wanted = java.util.Set.of(rarities);
        List<com.project.gainz.domain.record.entity.UserRecord> owned = java.util.Arrays.stream(com.project.gainz.domain.record.enums.RecordType.values())
                .filter(t -> rarities.length == 0 || wanted.contains(t.rarity()))
                .map(t -> new com.project.gainz.domain.record.entity.UserRecord(user, t))
                .toList();
        when(userRecordRepository.findAllByUserId(USER_ID)).thenReturn(owned);
    }

    @Test
    void 카세트를_다_모으면_카세트_세트를_무대에_놓는다() {
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        givenBasicOutfit();
        givenRecords(com.project.gainz.domain.record.enums.RecordRarity.COMMON);

        WardrobeUpdateRequest put = new WardrobeUpdateRequest(1L, StageBackground.ROOKIE, null, null, null, null, null, null, null, null, null, null,
                com.project.gainz.domain.character.enums.RoomDecor.CASSETTE);
        assertThat(characterService.updateWardrobe(USER_ID, put).roomDecor()).isEqualTo(com.project.gainz.domain.character.enums.RoomDecor.CASSETTE);

        WardrobeUpdateRequest cd = new WardrobeUpdateRequest(1L, StageBackground.ROOKIE, null, null, null, null, null, null, null, null, null, null,
                com.project.gainz.domain.character.enums.RoomDecor.CD);
        assertThatThrownBy(() -> characterService.updateWardrobe(USER_ID, cd))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_DECOR_LOCKED);
    }

    @Test
    void 리스닝_룸은_레코드를_전부_모아야_고를_수_있다() {
        ReflectionTestUtils.setField(character, "level", 30);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        givenBasicOutfit();
        givenRecords(com.project.gainz.domain.record.enums.RecordRarity.COMMON, com.project.gainz.domain.record.enums.RecordRarity.RARE);

        assertThatThrownBy(() -> characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(1L, StageBackground.LISTENING_ROOM)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAGE_BACKGROUND_LOCKED);

        givenRecords();
        assertThat(characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(1L, StageBackground.LISTENING_ROOM)).stageBackground())
                .isEqualTo(StageBackground.LISTENING_ROOM);
    }

    @Test
    void 옷장에_수집_보상_잠금이_보인다() {
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        when(userOutfitRepository.findOutfitItemIdsByUserId(USER_ID)).thenReturn(List.of());
        when(outfitItemRepository.findAllByOrderByRequiredLevelAscIdAsc()).thenReturn(List.of());
        givenRecords(com.project.gainz.domain.record.enums.RecordRarity.RARE);

        WardrobeResponse result = characterService.getWardrobe(USER_ID);

        assertThat(result.decorSets()).extracting(WardrobeResponse.DecorSetView::key, WardrobeResponse.DecorSetView::unlocked)
                .containsExactly(
                        Tuple.tuple(com.project.gainz.domain.character.enums.RoomDecor.CASSETTE, false),
                        Tuple.tuple(com.project.gainz.domain.character.enums.RoomDecor.CD, true),
                        Tuple.tuple(com.project.gainz.domain.character.enums.RoomDecor.LP, false));
        assertThat(result.backgrounds()).filteredOn(b -> b.key() == StageBackground.LISTENING_ROOM)
                .extracting(WardrobeResponse.BackgroundView::unlocked).containsExactly(false);
    }

    @Test
    void 벨트는_허리_자리에_쓴다() {
        ReflectionTestUtils.setField(character, "level", 16);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        givenBasicOutfit();

        CharacterResponse result = characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(
                1L, StageBackground.ROOKIE, null, null, null, null, null, null, null, Accessory.BELT));

        assertThat(result.waist()).isEqualTo(Accessory.BELT);
    }

    @Test
    void 예전_캐릭터는_기본_청바지와_신발을_입은_것으로_보인다() {
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));

        CharacterResponse result = characterService.getCharacter(USER_ID);

        assertThat(result.bottom()).isEqualTo(Accessory.JEANS);
        assertThat(result.shoes()).isEqualTo(Accessory.BASIC_SHOES);
    }

    @Test
    void 레벨이_닿지_않은_무대_효과는_고를_수_없다() {
        ReflectionTestUtils.setField(character, "level", 24);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        givenBasicOutfit();

        assertThatThrownBy(() -> characterService.updateWardrobe(USER_ID, new WardrobeUpdateRequest(
                1L, StageBackground.ROOKIE, null, null, null, null, null, null, StageEffect.SPOTLIGHT)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAGE_EFFECT_LOCKED);
        assertThat(character.getStageEffect()).isNull();
    }

    @Test
    void 레벨이_닿지_않은_소품은_쓸_수_없다() {
        ReflectionTestUtils.setField(character, "level", 27);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        givenBasicOutfit();

        assertThatThrownBy(() -> characterService.updateWardrobe(USER_ID,
                new WardrobeUpdateRequest(1L, StageBackground.ROOKIE, Accessory.CROWN, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESSORY_LOCKED);
        assertThat(character.getHat()).isNull();
    }

    @Test
    void 다른_자리의_소품은_쓸_수_없다() {
        ReflectionTestUtils.setField(character, "level", 30);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));
        givenBasicOutfit();

        assertThatThrownBy(() -> characterService.updateWardrobe(USER_ID,
                new WardrobeUpdateRequest(1L, StageBackground.ROOKIE, Accessory.SNEAKERS, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESSORY_SLOT_MISMATCH);
    }

    // ---------- 레벨 보상 헤어 ----------

    private CharacterUpdateRequest updateWithHair(HairStyle hair) {
        return new CharacterUpdateRequest("tester", character.getInstrumentType(), "#000000", character.getGender(), hair, "#000000", null);
    }

    @Test
    void 레벨이_닿은_보상_헤어로_바꿀_수_있다() {
        ReflectionTestUtils.setField(character, "level", 4);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));

        characterService.updateCharacter(USER_ID, updateWithHair(HairStyle.PONYTAIL));

        assertThat(character.getHairStyle()).isEqualTo(HairStyle.PONYTAIL);
    }

    @Test
    void 레벨이_닿지_않은_헤어로는_바꿀_수_없다() {
        ReflectionTestUtils.setField(character, "level", 3);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));

        assertThatThrownBy(() -> characterService.updateCharacter(USER_ID, updateWithHair(HairStyle.PONYTAIL)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HAIR_STYLE_LOCKED);
    }
}
