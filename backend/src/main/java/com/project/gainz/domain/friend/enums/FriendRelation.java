package com.project.gainz.domain.friend.enums;

// 프로필을 보는 나와 그 사람의 관계
public enum FriendRelation {
    ME,         // 내 프로필
    FRIEND,     // 친구
    SENT,       // 내가 신청하고 기다리는 중
    RECEIVED,   // 상대가 나에게 신청함
    NONE
}
