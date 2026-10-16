package com.project.gainz.domain.band.dto.response;

import com.project.gainz.domain.band.entity.Band;
import com.project.gainz.domain.band.enums.BandStage;
import com.project.gainz.domain.band.enums.Position;

import java.time.LocalDateTime;
import java.util.List;

// 밴드 상세. myPosition은 보는 사람의 자리, stage는 합주실 배경, members는 무대에 선 순서 (왼쪽부터)
public record BandResponse(
        Long id,
        String name,
        String description,
        String inviteCode,
        BandStage stage,
        Position myPosition,
        LocalDateTime createdAt,
        List<BandMemberResponse> members
) {
    public static BandResponse of(Band band, Position myPosition, List<BandMemberResponse> members) {
        return new BandResponse(
                band.getId(),
                band.getName(),
                band.getDescription(),
                band.getInviteCode(),
                band.getStage(),
                myPosition,
                band.getCreatedAt(),
                members
        );
    }
}
