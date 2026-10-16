package com.project.gainz.domain.sheet.entity;

import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.common.enums.InstrumentType;
import com.project.gainz.domain.sheet.enums.SheetType;
import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// 악보 한 곡. 올린 악보 파일(SheetPage), AI가 만든 마디별 악보(TabData), 예전에 직접 쓴 코드 악보(content)를 담는다.
// owner가 없으면 모두가 볼 수 있는 공용 악보, 있으면 그 사람이 만든 악보 (만든 사람만 고칠 수 있다)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SheetMusic extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    // 원곡 음원 (AI 악보만 있음)
    private String originalAudioUrl;

    private Integer bpm;

    // 원래 키 (C, F#m, Bb …). 키 변조의 기준
    @Column(length = 4)
    private String originalKey;

    // 예전에 직접 쓴 코드 악보 (지금은 파일로 올린다): 가사 사이에 [C] [Am7] 처럼 코드를 넣는 형식
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InstrumentType instrumentType;

    @ElementCollection(targetClass = SheetType.class)
    @CollectionTable(name = "sheet_music_types", joinColumns = @JoinColumn(name = "sheet_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "sheet_type", nullable = false)
    private List<SheetType> sheetTypes = new ArrayList<>();

    @Builder
    public SheetMusic(User owner, String title, String artist, String originalAudioUrl, Integer bpm,
                      String originalKey, String content, InstrumentType instrumentType, List<SheetType> sheetTypes) {
        this.owner = owner;
        this.title = title;
        this.artist = artist;
        this.originalAudioUrl = originalAudioUrl;
        this.bpm = bpm;
        this.originalKey = originalKey;
        this.content = content;
        this.instrumentType = instrumentType;
        if (sheetTypes != null) {
            this.sheetTypes.addAll(sheetTypes);
        }
    }

    public void updateInfo(String title, String artist, String originalKey, Integer bpm,
                           InstrumentType instrumentType, SheetType sheetType) {
        this.title = title;
        this.artist = artist;
        this.originalKey = originalKey;
        this.bpm = bpm;
        this.instrumentType = instrumentType;
        this.sheetTypes.clear();
        this.sheetTypes.add(sheetType);
    }

    public boolean isShared() {
        return owner == null;
    }

    public boolean isOwnedBy(Long userId) {
        return owner != null && Objects.equals(owner.getId(), userId);
    }
}
