package com.project.gainz.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /* User (유저) */
    DUPLICATE_EMAIL(409, "U001", "이미 존재하는 이메일입니다.");

    private final int status;
    private final String code;
    private final String message;
}
