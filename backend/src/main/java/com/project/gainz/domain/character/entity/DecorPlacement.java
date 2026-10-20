package com.project.gainz.domain.character.entity;

import com.project.gainz.domain.character.enums.DecorItem;

// 무대에 올린 방 꾸미기 소품 하나와 자리 (x = 무대 도트 기준 그림 왼쪽 끝, 바닥 줄에 놓임)
public record DecorPlacement(DecorItem key, int x) {
}
