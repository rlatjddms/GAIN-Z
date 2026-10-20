package com.project.gainz.domain.character.service;

import com.project.gainz.domain.character.dto.request.WardrobeUpdateRequest;
import com.project.gainz.domain.character.dto.response.WardrobeResponse;
import com.project.gainz.domain.character.enums.Accessory;
import com.project.gainz.domain.character.enums.LevelOutfit;
import com.project.gainz.domain.character.enums.RoomDecor;
import com.project.gainz.domain.record.entity.UserRecord;
import com.project.gainz.domain.record.enums.RecordType;
import com.project.gainz.domain.record.repository.UserRecordRepository;
import com.project.gainz.domain.character.enums.AccessorySlot;
import com.project.gainz.domain.character.enums.StageBackground;
import com.project.gainz.domain.character.enums.StageEffect;
import com.project.gainz.domain.character.dto.request.CharacterCreateRequest;
import com.project.gainz.domain.character.dto.request.CharacterUpdateRequest;
import com.project.gainz.domain.character.dto.response.CharacterResponse;
import com.project.gainz.domain.character.dto.response.ExperienceResult;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.entity.UserOutfit;
import com.project.gainz.domain.character.enums.OutfitCategory;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.character.repository.OutfitItemRepository;
import com.project.gainz.domain.character.repository.UserOutfitRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final OutfitItemRepository outfitItemRepository;
    private final UserOutfitRepository userOutfitRepository;
    private final UserRepository userRepository;
    private final UserRecordRepository userRecordRepository;

    // 캐릭터 생성
    public Long createCharacter(Long userId, CharacterCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (characterRepository.findByUserId(userId).isPresent()) {
            throw new BusinessException(ErrorCode.CHARACTER_ALREADY_EXISTS);
        }
        if (characterRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        // 레벨 보상 머리는 처음엔 고를 수 없다
        if (!request.hairStyle().isUnlockedAt(1)) {
            throw new BusinessException(ErrorCode.HAIR_STYLE_LOCKED);
        }

        OutfitItem defaultOutfit = outfitItemRepository
                .findFirstBySpriteKey(OutfitItem.DEFAULT_SPRITE_KEY)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEFAULT_OUTFIT_NOT_FOUND));

        Character character = Character.builder()
                .user(user)
                .nickname(request.nickname())
                .instrumentType(request.instrumentType())
                .instrumentColor(request.instrumentColor())
                .gender(request.gender())
                .hairStyle(request.hairStyle())
                .hairColor(request.hairColor())
                .skinColor(request.skinColor())
                .currentOutfit(defaultOutfit)
                .build();

        characterRepository.save(character);

        UserOutfit userOutfit = UserOutfit.builder()
                .user(user)
                .outfitItem(defaultOutfit)
                .build();
        userOutfitRepository.save(userOutfit);

        return character.getId();
    }

    // 캐릭터 조회
    @Transactional(readOnly = true)
    public CharacterResponse getCharacter(Long userId) {
        Character character = characterRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHARACTER_NOT_FOUND));
        return CharacterResponse.from(character);
    }

    // 캐릭터 수정
    public void updateCharacter(Long userId, CharacterUpdateRequest request) {

        Character character = characterRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHARACTER_NOT_FOUND));

        if (!character.getNickname().equals(request.nickname()) &&
                characterRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // 레벨이 닿지 않은 머리로는 바꿀 수 없다 (지금 머리는 그대로 둘 수 있음)
        if (request.hairStyle() != character.getHairStyle() && !request.hairStyle().isUnlockedAt(character.getLevel())) {
            throw new BusinessException(ErrorCode.HAIR_STYLE_LOCKED);
        }

        character.update(
                request.nickname(),
                request.instrumentType(),
                request.instrumentColor(),
                request.gender(),
                request.hairStyle(),
                request.hairColor(),
                request.skinColor()
        );
    }

    // 옷장 조회: 전체 의상(보유·착용 여부)과 무대 배경(해금 여부)
    @Transactional(readOnly = true)
    public WardrobeResponse getWardrobe(Long userId) {
        Character character = characterRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHARACTER_NOT_FOUND));
        Set<Long> owned = new HashSet<>(userOutfitRepository.findOutfitItemIdsByUserId(userId));
        Long equippedId = character.getCurrentOutfit() != null ? character.getCurrentOutfit().getId() : null;

        List<WardrobeResponse.OutfitView> outfits = outfitItemRepository.findAllByOrderByRequiredLevelAscIdAsc().stream()
                .filter(item -> item.getCategory() != OutfitCategory.BACKGROUND)
                .map(item -> WardrobeResponse.OutfitView.of(item, owns(character, owned, item), item.getId().equals(equippedId)))
                .toList();
        Set<RecordType> records = ownedRecords(userId);
        List<WardrobeResponse.BackgroundView> backgrounds = Arrays.stream(StageBackground.values())
                .map(bg -> new WardrobeResponse.BackgroundView(bg, bg.getRequiredLevel(), isUnlocked(bg, character, records)))
                .toList();
        List<WardrobeResponse.DecorSetView> decorSets = Arrays.stream(RoomDecor.values())
                .map(d -> new WardrobeResponse.DecorSetView(d, d.isCompletedBy(records)))
                .toList();
        return new WardrobeResponse(outfits, backgrounds, character.getCurrentStageBackground(), decorSets);
    }

    // 옷장 저장: 보유한 의상으로 갈아입고, 얻은 무대 배경·효과를 고르고, 자리마다 하의·소품을 쓴다
    public CharacterResponse updateWardrobe(Long userId, WardrobeUpdateRequest request) {
        Character character = characterRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHARACTER_NOT_FOUND));

        OutfitItem outfit = outfitItemRepository.findById(request.outfitId())
                .orElseThrow(() -> new BusinessException(ErrorCode.OUTFIT_NOT_OWNED));
        boolean ownsOutfit = character.getLevel() >= outfit.getRequiredLevel()
                || userOutfitRepository.existsByUserIdAndOutfitItemId(userId, request.outfitId());
        if (!ownsOutfit) {
            throw new BusinessException(ErrorCode.OUTFIT_NOT_OWNED);
        }
        StageBackground background = request.stageBackground();
        RoomDecor decor = request.roomDecor();
        // 레코드 수집 보상(리스닝 룸·방 꾸미기)을 고를 때만 레코드를 읽는다
        Set<RecordType> records = background.isRecordReward() || decor != null ? ownedRecords(userId) : Set.of();
        if (!isUnlocked(background, character, records)) {
            throw new BusinessException(ErrorCode.STAGE_BACKGROUND_LOCKED);
        }
        if (decor != null && !decor.isCompletedBy(records)) {
            throw new BusinessException(ErrorCode.ROOM_DECOR_LOCKED);
        }

        Map<AccessorySlot, Accessory> accessories = new EnumMap<>(AccessorySlot.class);
        accessories.put(AccessorySlot.HAT, request.hat());
        accessories.put(AccessorySlot.GLASSES, request.glasses());
        accessories.put(AccessorySlot.EARRINGS, request.earrings());
        accessories.put(AccessorySlot.NECK, request.neck());
        accessories.put(AccessorySlot.WAIST, request.waist());
        accessories.put(AccessorySlot.SHOES, request.shoes());
        accessories.put(AccessorySlot.BOTTOM, request.bottom());
        // 늘 입는 자리(하의·신발)를 비우면 기본 아이템
        accessories.replaceAll((slot, accessory) -> accessory != null ? accessory : Accessory.defaultFor(slot));
        accessories.forEach((slot, accessory) -> {
            if (accessory == null) return;
            if (accessory.getSlot() != slot) throw new BusinessException(ErrorCode.ACCESSORY_SLOT_MISMATCH);
            if (!accessory.isUnlockedAt(character.getLevel())) throw new BusinessException(ErrorCode.ACCESSORY_LOCKED);
        });

        StageEffect effect = request.stageEffect();
        if (effect != null && !effect.isUnlockedAt(character.getLevel())) {
            throw new BusinessException(ErrorCode.STAGE_EFFECT_LOCKED);
        }

        if (request.itemColors() != null) {
            validateItemColors(request.itemColors());
        }

        character.changeOutfit(outfit);
        character.changeStageBackground(background);
        if (request.itemColors() != null) character.changeItemColors(request.itemColors());
        if (request.instrumentHidden() != null) character.changeInstrumentHidden(request.instrumentHidden());
        character.changeStageEffect(effect);
        character.changeRoomDecor(decor);
        accessories.forEach(character::wear);
        return CharacterResponse.from(character);
    }

    private Set<RecordType> ownedRecords(Long userId) {
        Set<RecordType> owned = new HashSet<>();
        for (UserRecord r : userRecordRepository.findAllByUserId(userId)) owned.add(r.getRecordType());
        return owned;
    }

    // 레벨 무대는 레벨로, 리스닝 룸은 레코드를 전부 모았는지로
    private static boolean isUnlocked(StageBackground bg, Character character, Set<RecordType> records) {
        return bg.isRecordReward() ? RoomDecor.allCollected(records) : bg.isUnlockedAt(character.getLevel());
    }

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final int MAX_ITEM_COLORS = 64;

    // 색을 고를 수 있는 아이템: 기본 의상·레벨 상의(스프라이트 키)와 하의·소품 (가진 것인지는 묻지 않는다)
    private static boolean isItemKey(String key) {
        if (OutfitItem.DEFAULT_SPRITE_KEY.equals(key)) return true;
        for (LevelOutfit o : LevelOutfit.values()) if (o.getSpriteKey().equals(key)) return true;
        for (Accessory a : Accessory.values()) if (a.name().equals(key)) return true;
        return false;
    }

    private void validateItemColors(Map<String, String> colors) {
        if (colors.size() > MAX_ITEM_COLORS) throw new BusinessException(ErrorCode.ITEM_COLOR_INVALID);
        colors.forEach((key, color) -> {
            if (!isItemKey(key) || color == null || !HEX_COLOR.matcher(color).matches()) {
                throw new BusinessException(ErrorCode.ITEM_COLOR_INVALID);
            }
        });
    }

    // 레벨 보상 의상은 레벨이 닿으면 가진 것 (예전에 레벨이 오른 사람도). 그 밖에는 받은 기록으로
    private boolean owns(Character character, Set<Long> ownedIds, OutfitItem item) {
        return ownedIds.contains(item.getId()) || character.getLevel() >= item.getRequiredLevel();
    }

    // 경험치 추가 후 레벨업 및 아이템 해금 (expAmount가 음수면 경험치를 되돌리고 레벨은 그대로)
    public ExperienceResult processExperienceAndUnlocks(Long userId, int expAmount, long practiceTimeMinutes) {

        Character character = characterRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHARACTER_NOT_FOUND));

        int prevLevel = character.getLevel();
        character.addPracticeTime(practiceTimeMinutes);

        boolean isLeveledUp = false;
        if (expAmount >= 0) isLeveledUp = character.gainExperience(expAmount);
        else character.loseExperience(-expAmount);
        int currentLevel = character.getLevel();

        List<ExperienceResult.NewUnlockedOutfit> unlockedOutfits = new ArrayList<>();

        if (isLeveledUp) {
            List<OutfitItem> targetItems = outfitItemRepository.findUnlockableItems(prevLevel, currentLevel);

            for (OutfitItem item : targetItems) {
                boolean alreadyOwned = userOutfitRepository.existsByUserAndOutfitItem(character.getUser(), item);

                if (!alreadyOwned) {
                    UserOutfit userOutfit = UserOutfit.builder()
                            .user(character.getUser())
                            .outfitItem(item)
                            .build();
                    userOutfitRepository.save(userOutfit);

                    unlockedOutfits.add(ExperienceResult.NewUnlockedOutfit.from(item));
                }
            }
        }

        return new ExperienceResult(
                prevLevel,
                currentLevel,
                character.getExp(),
                isLeveledUp,
                unlockedOutfits
        );
    }
}
