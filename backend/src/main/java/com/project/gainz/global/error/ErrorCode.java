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
    CHARACTER_NOT_FOUND(404, "C004", "존재하지 않는 캐릭터입니다."),
    OUTFIT_NOT_OWNED(400, "C005", "가지고 있지 않은 의상입니다."),
    STAGE_BACKGROUND_LOCKED(400, "C006", "아직 해금하지 않은 무대 배경입니다."),

    /* Practice (연습) */
    PRACTICE_DAY_LIMIT_EXCEEDED(400, "P001", "하루 연습 시간은 24시간을 넘을 수 없습니다."),
    PRACTICE_EDIT_PERIOD_EXPIRED(400, "P002", "연습 기록은 최근 7일까지만 수정할 수 있습니다."),
    INVALID_PRACTICE_PERIOD(400, "P003", "조회 기간이 올바르지 않습니다. (최대 1년)"),

    /* Routine (연습 루틴) */
    ROUTINE_NOT_FOUND(404, "R001", "존재하지 않는 루틴입니다."),
    ROUTINE_LIMIT_EXCEEDED(400, "R002", "루틴은 최대 20개까지 만들 수 있습니다."),
    ROUTINE_CHECK_PERIOD_EXPIRED(400, "R003", "루틴 체크는 최근 7일까지만 할 수 있습니다."),
    ROUTINE_NOT_SCHEDULED(400, "R004", "그날은 이 루틴 일정이 없습니다."),
    INVALID_ROUTINE_SCHEDULE(400, "R005", "반복 설정이 올바르지 않습니다. (요일 반복은 요일을 하나 이상, 종료일은 시작일 이후)"),

    /* Sheet (악보·보관함) */
    SHEET_NOT_FOUND(404, "S001", "존재하지 않는 악보입니다."),
    WORKSPACE_ITEM_NOT_FOUND(404, "S002", "보관함에 없는 악보입니다."),
    SHEET_ALREADY_IN_WORKSPACE(409, "S003", "이미 보관함에 담긴 악보입니다."),
    SHEET_EDIT_DENIED(403, "S004", "직접 만든 악보만 고칠 수 있습니다."),
    WORKSPACE_LIMIT_EXCEEDED(400, "S005", "보관함에는 악보를 최대 200개까지 담을 수 있습니다.");

    private final int status;
    private final String code;
    private final String message;
}
