package com.project.gainz.domain.sheet.entity;

import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TabData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    private SheetMusic sheetMusic;

    @Column(nullable = false)
    private Integer measureNumber;

    // 마디 하나의 박자·코드·음 (JSON). 포맷은 docs/note-data-format.md
    @Column(columnDefinition = "TEXT", nullable = false)
    private String noteData;

    // 이 마디의 운지 안내 문구 (원래 키 기준)

    private String recommendedFingering;

    // 다른 악보로 그대로 옮긴 사본 (내 악보로 복사, #191)
    public TabData copyTo(SheetMusic target) {
        TabData copy = new TabData();
        copy.sheetMusic = target;
        copy.measureNumber = measureNumber;
        copy.noteData = noteData;
        copy.recommendedFingering = recommendedFingering;
        return copy;
    }
}
