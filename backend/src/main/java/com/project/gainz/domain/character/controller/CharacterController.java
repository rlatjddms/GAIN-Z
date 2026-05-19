package com.project.gainz.domain.character.controller;

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

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateCharacter(
            @Valid @RequestBody CharacterUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        characterService.updateCharacter(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
