package com.project.gainz.domain.character.service;

import com.project.gainz.domain.character.dto.request.CharacterCreateRequest;
import com.project.gainz.domain.character.dto.request.CharacterUpdateRequest;
import com.project.gainz.domain.character.dto.response.CharacterResponse;
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
                request.hairColor()
        );
    }
}
