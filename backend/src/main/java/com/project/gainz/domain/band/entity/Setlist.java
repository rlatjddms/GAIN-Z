package com.project.gainz.domain.band.entity;

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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE setlist SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Setlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_id")
    private Band band;

    @Column(nullable = false)
    private String title;

    private String description;

    // 밴드 멤버가 함께 쓰는 즐겨찾기 표시 (예전 행은 비어 있어서 null이면 꺼진 것으로 본다)
    private Boolean favorite = false;

    // 목록에 보이는 순서 (작을수록 위)
    private Integer listOrder;

    @OneToMany(mappedBy = "setlist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("playOrder ASC, id ASC")
    private List<SetlistItem> items = new ArrayList<>();

    public Setlist(Band band, String title, Integer listOrder) {
        this.band = band;
        this.title = title;
        this.listOrder = listOrder;
    }

    public void rename(String title) {
        this.title = title;
    }

    public void changeFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void changeListOrder(Integer listOrder) {
        this.listOrder = listOrder;
    }

    public boolean isFavorite() {
        return Boolean.TRUE.equals(favorite);
    }
}
