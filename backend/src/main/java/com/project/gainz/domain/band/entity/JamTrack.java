package com.project.gainz.domain.band.entity;

import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.common.entity.BaseEntity;
import com.project.gainz.global.common.enums.InstrumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

// 잼에 쌓인 녹음 한 겹. 볼륨·시작 위치는 밴드가 함께 듣는 믹스 설정
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE jam_track SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class JamTrack extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jam_id", nullable = false)
    private Jam jam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    // 올릴 때 밴드에서 맡은 파트
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InstrumentType part;

    @Embedded
    private BandPostFile file;

    // 0~150 (%)
    @Column(nullable = false)
    private Integer volume;

    // 녹음 파일 0초가 놓이는 타임라인 위치(ms). 음수면 앞부분을 잘라 맞춘다
    @Column(nullable = false)
    private Integer offsetMs;

    private Integer durationMs;

    @Builder
    public JamTrack(Jam jam, User uploader, InstrumentType part, BandPostFile file, Integer volume, Integer offsetMs, Integer durationMs) {
        this.jam = jam;
        this.uploader = uploader;
        this.part = part;
        this.file = file;
        this.volume = volume;
        this.offsetMs = offsetMs;
        this.durationMs = durationMs;
    }

    public void changeVolume(Integer volume) {
        this.volume = volume;
    }

    public void changeOffset(Integer offsetMs) {
        this.offsetMs = offsetMs;
    }
}
