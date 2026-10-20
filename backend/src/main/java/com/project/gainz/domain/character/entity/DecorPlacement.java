package com.project.gainz.domain.character.entity;

import com.project.gainz.domain.character.enums.DecorItem;

// 무대에 올린 방 꾸미기 소품 하나와 자리 (무대 도트 기준)
//   x = 그림 왼쪽 끝, y = 그림 바닥 줄 (null이면 그 무대의 바닥 줄)
public record DecorPlacement(DecorItem key, int x, Integer y) {

    public DecorPlacement(DecorItem key, int x) {
        this(key, x, null);
    }
}
