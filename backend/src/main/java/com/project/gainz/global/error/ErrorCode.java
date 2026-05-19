package com.project.gainz.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /* User (유저) */
    DUPLICATE_EMAIL(409, "U001", "이미 존재하는 이메일입니다."),
    USER_NOT_FOUND(404, "U002", "존재하지 않는 사용자입니다."),
    INVALID_PASSWORD(400, "U003", "비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(401, "U005", "유효하지 않은 토큰입니다."),

    /* Character (캐릭터) */
    CHARACTER_ALREADY_EXISTS(409, "C001", "이미 캐릭터를 보유하고 있는 유저입니다."),
    DUPLICATE_NICKNAME(409, "C002", "이미 사용 중인 캐릭터 닉네임입니다."),
    DEFAULT_OUTFIT_NOT_FOUND(500, "C003", "시스템 오류: 초기 기본 의상 아이템이 설정되지 않았습니다."),
    CHARACTER_NOT_FOUND(404, "C004", "존재하지 않는 캐릭터입니다.");

    private final int status;
    private final String code;
    private final String message;
}
