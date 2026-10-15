package com.project.gainz.domain.character.service;

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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final OutfitItemRepository outfitItemRepository;
    private final UserOutfitRepository userOutfitRepository;
    private final UserRepository userRepository;

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

        OutfitItem defaultOutfit = outfitItemRepository
                .findByCategoryAndRequiredLevel(OutfitCategory.CLOTHES, 1)
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

    // 경험치 추가 후 레벨업 및 아이템 해금
    public ExperienceResult processExperienceAndUnlocks(Long userId, int expAmount, long practiceTimeMinutes) {

        Character character = characterRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHARACTER_NOT_FOUND));

        int prevLevel = character.getLevel();
        character.addPracticeTime(practiceTimeMinutes);

        boolean isLeveledUp = character.gainExperience(expAmount);
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
