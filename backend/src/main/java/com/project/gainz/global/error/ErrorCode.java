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
    SOCIAL_ACCOUNT_PASSWORD(400, "U004", "소셜 로그인 계정은 비밀번호를 바꿀 수 없습니다."),
    INVALID_TOKEN(401, "U005", "유효하지 않은 토큰입니다."),
    SAME_PASSWORD(400, "U006", "지금 비밀번호와 다른 비밀번호를 입력해 주세요."),
    PROFILE_IMAGE_REQUIRED(400, "U007", "프로필 사진 파일을 올려 주세요."),
    INVALID_PROFILE_IMAGE(400, "U008", "프로필 사진은 PNG·JPG·WEBP 이미지만 올릴 수 있습니다."),
    PROFILE_IMAGE_TOO_LARGE(400, "U009", "프로필 사진은 5MB까지 올릴 수 있습니다."),
    PROFILE_IMAGE_NOT_FOUND(404, "U010", "프로필 사진이 없습니다."),
    PROFILE_IMAGE_STORAGE_FAILED(500, "U011", "프로필 사진을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요."),

    /* Friend (친구) */
    INVALID_FRIEND_CODE(404, "F001", "친구 코드가 올바르지 않습니다. 친구에게 코드를 다시 확인해 주세요."),
    CANNOT_FRIEND_SELF(400, "F002", "나 자신에게는 친구 신청을 할 수 없습니다."),
    ALREADY_FRIENDS(409, "F003", "이미 친구입니다."),
    FRIEND_REQUEST_ALREADY_SENT(409, "F004", "이미 친구 신청을 보냈습니다. 상대가 수락할 때까지 기다려 주세요."),
    FRIEND_REQUEST_ALREADY_RECEIVED(409, "F005", "상대가 먼저 친구 신청을 보냈습니다. 받은 신청에서 수락해 주세요."),
    FRIEND_REQUEST_NOT_FOUND(404, "F006", "친구 신청이 없습니다. 이미 처리되었을 수 있습니다."),
    FRIEND_LIMIT_EXCEEDED(400, "F007", "친구는 최대 100명까지 맺을 수 있습니다."),
    NOT_FRIEND(404, "F008", "친구가 아닙니다."),
    FRIEND_CODE_GENERATION_FAILED(500, "F009", "친구 코드를 만들지 못했습니다. 잠시 후 다시 시도해 주세요."),
    NOT_BAND_MATE(403, "F010", "같은 밴드 멤버에게만 코드 없이 신청할 수 있습니다. 친구 코드로 신청해 주세요."),

    /* Notification (알림) */
    NOTIFICATION_NOT_FOUND(404, "N001", "알림이 없습니다. 이미 지워졌을 수 있습니다."),

    /* Character (캐릭터) */
    CHARACTER_ALREADY_EXISTS(409, "C001", "이미 캐릭터를 보유하고 있는 유저입니다."),
    DUPLICATE_NICKNAME(409, "C002", "이미 사용 중인 캐릭터 닉네임입니다."),
    DEFAULT_OUTFIT_NOT_FOUND(500, "C003", "시스템 오류: 초기 기본 의상 아이템이 설정되지 않았습니다."),
    CHARACTER_NOT_FOUND(404, "C004", "존재하지 않는 캐릭터입니다."),
    OUTFIT_NOT_OWNED(400, "C005", "가지고 있지 않은 의상입니다."),
    STAGE_BACKGROUND_LOCKED(400, "C006", "아직 해금하지 않은 무대 배경입니다."),
    HAIR_STYLE_LOCKED(400, "C007", "아직 해금하지 않은 헤어스타일입니다."),
    ACCESSORY_LOCKED(400, "C008", "아직 해금하지 않은 소품입니다."),
    ACCESSORY_SLOT_MISMATCH(400, "C009", "그 자리에 쓸 수 없는 소품입니다."),
    STAGE_EFFECT_LOCKED(400, "C010", "아직 해금하지 않은 무대 효과입니다."),
    ITEM_COLOR_INVALID(400, "C011", "아이템 색이 올바르지 않습니다."),

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
    WORKSPACE_LIMIT_EXCEEDED(400, "S005", "보관함에는 악보를 최대 200개까지 담을 수 있습니다."),
    SHEET_FILE_REQUIRED(400, "S006", "악보 파일을 한 장 이상 올려 주세요."),
    INVALID_SHEET_FILE(400, "S007", "PNG·JPG·WEBP 이미지나 PDF만 올릴 수 있습니다."),
    SHEET_FILE_TOO_LARGE(400, "S008", "악보 파일은 한 장에 10MB까지 올릴 수 있습니다."),
    SHEET_PAGE_LIMIT_EXCEEDED(400, "S009", "악보는 최대 20장까지 올릴 수 있습니다."),
    SHEET_PAGE_NOT_FOUND(404, "S010", "존재하지 않는 악보 장입니다."),
    SHEET_LAST_PAGE(400, "S011", "마지막 한 장은 지울 수 없습니다."),
    SHEET_FILE_STORAGE_FAILED(500, "S012", "악보 파일을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요."),

    /* Band (밴드) */
    BAND_NOT_FOUND(404, "B001", "존재하지 않는 밴드입니다."),
    NOT_BAND_MEMBER(403, "B002", "이 밴드의 멤버만 할 수 있습니다."),
    BAND_LEADER_ONLY(403, "B003", "밴드 리더만 할 수 있습니다."),
    INVALID_INVITE_CODE(404, "B004", "초대 코드가 올바르지 않습니다. 리더에게 코드를 다시 확인해 주세요."),
    ALREADY_BAND_MEMBER(409, "B005", "이미 이 밴드의 멤버입니다."),
    BAND_FULL(400, "B006", "밴드 멤버는 최대 8명까지입니다."),
    BAND_JOIN_LIMIT_EXCEEDED(400, "B007", "밴드는 최대 5개까지 들어갈 수 있습니다."),
    BAND_MEMBER_NOT_FOUND(404, "B008", "이 밴드에 없는 멤버입니다."),
    BAND_LEADER_CANNOT_LEAVE(400, "B009", "리더는 다른 멤버에게 리더를 넘긴 뒤에 나갈 수 있습니다. 혼자 남았다면 밴드를 없애 주세요."),
    CANNOT_REMOVE_SELF(400, "B010", "나 자신은 내보낼 수 없습니다. 밴드 나가기를 이용해 주세요."),
    ALREADY_BAND_LEADER(400, "B011", "이미 리더인 멤버입니다."),
    BAND_PART_CHANGE_DENIED(403, "B012", "본인이나 리더만 파트를 바꿀 수 있습니다."),
    INVITE_CODE_GENERATION_FAILED(500, "B013", "초대 코드를 만들지 못했습니다. 잠시 후 다시 시도해 주세요."),
    INVALID_MEMBER_ORDER(400, "B014", "멤버 순서가 지금 멤버와 맞지 않습니다. 새로고침한 뒤 다시 시도해 주세요."),
    BAND_POST_NOT_FOUND(404, "B015", "게시글이 없습니다."),
    BAND_AUTHOR_ONLY(403, "B016", "작성자만 수정하거나 삭제할 수 있습니다."),
    BAND_COMMENT_NOT_FOUND(404, "B017", "댓글이 없습니다."),
    BAND_REPLY_DEPTH(400, "B018", "대댓글에는 답글을 달 수 없습니다."),
    BAND_FILE_INVALID(400, "B019", "사진·영상·오디오·PDF 파일을 10MB까지 첨부할 수 있습니다."),
    BAND_FILE_NOT_FOUND(404, "B020", "첨부 파일이 없습니다."),
    BAND_FILE_STORAGE_FAILED(500, "B021", "첨부 파일을 저장하지 못했습니다."),
    BAND_FILE_LIMIT(400, "B022", "첨부 파일은 최대 8개까지 올릴 수 있습니다."),
    BAND_FILE_SELECTION_INVALID(400, "B023", "첨부 목록이 변경되었습니다. 글을 다시 열어 주세요."),
    BAND_NOTICE_EXPIRY_INVALID(400, "B024", "공지 종료 시각은 현재보다 이후로 설정해 주세요."),
    SETLIST_NOT_FOUND(404, "B025", "셋리스트가 없습니다."),
    SETLIST_ITEM_NOT_FOUND(404, "B026", "셋리스트에 없는 곡입니다."),
    SETLIST_LIMIT_EXCEEDED(400, "B027", "셋리스트는 밴드마다 최대 50개까지 만들 수 있습니다."),
    SETLIST_ITEM_LIMIT_EXCEEDED(400, "B028", "셋리스트에는 곡을 최대 50곡까지 담을 수 있습니다."),
    INVALID_SETLIST_ORDER(400, "B029", "순서가 지금 목록과 맞지 않습니다. 새로고침한 뒤 다시 시도해 주세요."),
    SETLIST_ASSIGNEE_INVALID(400, "B030", "이 밴드에 없는 멤버는 담당자로 고를 수 없습니다."),
    SETLIST_TITLE_REQUIRED(400, "B031", "셋리스트 이름을 적어 주세요."),
    JAM_NOT_FOUND(404, "B032", "잼을 찾을 수 없습니다."),
    JAM_TRACK_NOT_FOUND(404, "B033", "트랙을 찾을 수 없습니다."),
    JAM_LIMIT_EXCEEDED(400, "B034", "잼은 밴드마다 50개까지 만들 수 있습니다."),
    JAM_TRACK_LIMIT_EXCEEDED(400, "B035", "트랙은 잼마다 8개까지 올릴 수 있습니다."),
    JAM_TRACK_FILE_INVALID(400, "B036", "WebM·OGG·M4A·MP3·WAV 녹음 파일을 30MB까지 올릴 수 있습니다."),
    JAM_PERMISSION_DENIED(403, "B037", "만든 멤버나 리더만 할 수 있습니다."),
    JAM_SONG_INVALID(400, "B038", "이 밴드 셋리스트에 없는 곡입니다.");

    private final int status;
    private final String code;
    private final String message;
}
