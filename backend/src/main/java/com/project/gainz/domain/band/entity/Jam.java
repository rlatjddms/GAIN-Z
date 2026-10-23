package com.project.gainz.domain.band.entity;

import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

// 레이어 잼: 멤버가 각자 녹음한 트랙을 쌓아 만드는 비대면 합주 곡
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE jam SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Jam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_id", nullable = false)
    private Band band;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, length = 60)
    private String title;

    // 메트로놈·카운트인 박자
    @Column(nullable = false)
    private Integer bpm;

    // 연결한 셋리스트 곡. 곡이 지워져도 잼은 남도록 id와 이름만 저장한다
    private Long setlistItemId;
    private String songTitle;

    // 즐겨찾기 (밴드 전체가 같이 봄). null = 예전 잼 = 아님
    private Boolean favorite;

    @OneToMany(mappedBy = "jam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<JamTrack> tracks = new ArrayList<>();

    public Jam(Band band, User creator, String title, Integer bpm) {
        this.band = band;
        this.creator = creator;
        this.title = title;
        this.bpm = bpm;
    }

    public void update(String title, Integer bpm) {
        this.title = title;
        this.bpm = bpm;
    }

    public boolean isFavorite() {
        return Boolean.TRUE.equals(favorite);
    }

    public void changeFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void linkSong(Long setlistItemId, String songTitle) {
        this.setlistItemId = setlistItemId;
        this.songTitle = songTitle;
    }
}
