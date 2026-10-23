package com.project.gainz.domain.band.entity;

import com.project.gainz.domain.band.enums.Position;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.common.enums.InstrumentType;
import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE band_member SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class BandMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_id", nullable = false)
    private Band band;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Position position;

    // 이 밴드에서 맡은 파트 (캐릭터 기본 악기와 따로)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InstrumentType part;

    // 합주실 무대에 선 순서 (작을수록 왼쪽). 예전 멤버는 비어 있어서 들어온 순서로 본다
    private Integer stageOrder;

    @Builder
    public BandMember(Band band, User user, Position position, InstrumentType part, Integer stageOrder) {
        this.band = band;
        this.user = user;
        this.position = position;
        this.part = part;
        this.stageOrder = stageOrder;
    }

    public boolean isLeader() {
        return position == Position.LEADER;
    }

    public void changePart(InstrumentType part) {
        this.part = part;
    }

    public void changePosition(Position position) {
        this.position = position;
    }

    public void changeStageOrder(Integer stageOrder) {
        this.stageOrder = stageOrder;
    }
}
