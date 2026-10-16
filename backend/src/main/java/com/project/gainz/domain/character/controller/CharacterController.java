package com.project.gainz.domain.character.controller;

import com.project.gainz.domain.character.dto.request.WardrobeUpdateRequest;
import com.project.gainz.domain.character.dto.response.WardrobeResponse;
import com.project.gainz.domain.character.dto.request.CharacterCreateRequest;
import com.project.gainz.domain.character.dto.request.CharacterUpdateRequest;
import com.project.gainz.domain.character.dto.response.CharacterResponse;
import com.project.gainz.domain.character.service.CharacterService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createCharacter(
            @Valid @RequestBody CharacterCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long characterId = characterService.createCharacter(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(characterId));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CharacterResponse>> getCharacter(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CharacterResponse response = characterService.getCharacter(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 옷장: 전체 의상(보유·착용)과 무대 배경(해금 여부)
    @GetMapping("/me/wardrobe")
    public ResponseEntity<ApiResponse<WardrobeResponse>> getWardrobe(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(characterService.getWardrobe(principal.getUserId())));
    }

    // 옷장 저장: 의상 갈아입기, 무대 배경 고르기
    @PutMapping("/me/wardrobe")
    public ResponseEntity<ApiResponse<CharacterResponse>> updateWardrobe(
            @Valid @RequestBody WardrobeUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(characterService.updateWardrobe(principal.getUserId(), request)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateCharacter(
            @Valid @RequestBody CharacterUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        characterService.updateCharacter(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
