package com.project.gainz.domain.sheet.entity;

import com.project.gainz.domain.user.entity.User;
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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// 내 악보 보관함에 담긴 악보 한 장과 나만의 설정 (메모·바꾼 키·즐겨찾기)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserWorkspace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    private SheetMusic sheetMusic;

    // 내가 바꿔서 보는 키. null이면 원래 키
    @Column(length = 4)
    private String customKey;

    @Column(columnDefinition = "TEXT")
    private String personalMemo;

    @Column(nullable = false)
    private Boolean isFavorite = false;

    // 키 바꿔 치기 표에 내가 추가한 코드 (#192). 띄어쓰기로 이어 저장 (코드에는 띄어쓰기가 없다)
    @Column(length = 400)
    private String customChords;

    @Builder
    public UserWorkspace(User user, SheetMusic sheetMusic) {
        this.user = user;
        this.sheetMusic = sheetMusic;
        this.isFavorite = false;
    }

    // 내 악보로 복사했을 때 이 항목이 가리키는 악보를 사본으로 바꾼다 (#191)
    public void changeSheet(SheetMusic sheetMusic) {
        this.sheetMusic = sheetMusic;
    }

    // 원래 키와 같으면 따로 저장하지 않는다
    public void changeCustomKey(String key) {
        this.customKey = (key == null || key.equals(sheetMusic.getOriginalKey())) ? null : key;
    }

    public void changePersonalMemo(String memo) {
        this.personalMemo = (memo == null || memo.isBlank()) ? null : memo;
    }

    public void changeFavorite(boolean favorite) {
        this.isFavorite = favorite;
    }

    // 겹치는 코드는 하나만, 순서는 보낸 대로. 비우면 null
    public void changeCustomChords(List<String> chords) {
        List<String> unique = chords.stream().map(String::trim).filter(c -> !c.isEmpty()).distinct().toList();
        this.customChords = unique.isEmpty() ? null : String.join(" ", unique);
    }

    public List<String> getCustomChordList() {
        return customChords == null ? List.of() : List.of(customChords.split(" "));
    }
}
