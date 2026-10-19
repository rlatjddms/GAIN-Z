package com.project.gainz.domain.user.enums;

// 프로필 공개 범위. 못 보는 사람에게는 이름·프로필 사진만 보인다
public enum ProfileVisibility {
    PUBLIC,     // 전체 공개
    FRIENDS,    // 친구에게만
    PRIVATE     // 비공개 (나만)
}
