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

    @Builder
    public UserWorkspace(User user, SheetMusic sheetMusic) {
        this.user = user;
        this.sheetMusic = sheetMusic;
        this.isFavorite = false;
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
}
