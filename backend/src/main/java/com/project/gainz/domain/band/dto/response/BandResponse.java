package com.project.gainz.domain.band.dto.response;

import com.project.gainz.domain.band.entity.Band;
import com.project.gainz.domain.band.enums.Position;

import java.time.LocalDateTime;
import java.util.List;

// 밴드 상세. myPosition은 보는 사람의 자리, members는 리더 먼저 그다음 들어온 순서
public record BandResponse(
        Long id,
        String name,
        String description,
        String inviteCode,
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
                myPosition,
                band.getCreatedAt(),
                members
        );
    }
}
