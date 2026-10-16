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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 올린 악보 파일 한 장 (이미지 또는 PDF). 파일은 서버 디스크에 storedName으로 저장된다
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SheetPage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    private SheetMusic sheetMusic;

    @Column(nullable = false)
    private Integer pageNumber;      // 1부터

    @Column(nullable = false, length = 60)
    private String storedName;       // 저장소 안 파일 이름 (UUID.확장자)

    @Column(nullable = false, length = 40)
    private String contentType;      // 파일 앞부분으로 확인한 실제 형식

    @Column(nullable = false)
    private String fileName;         // 올린 사람이 고른 파일 이름 (표시용)

    @Column(nullable = false)
    private Long size;

    @Builder
    public SheetPage(SheetMusic sheetMusic, Integer pageNumber, String storedName, String contentType, String fileName, Long size) {
        this.sheetMusic = sheetMusic;
        this.pageNumber = pageNumber;
        this.storedName = storedName;
        this.contentType = contentType;
        this.fileName = fileName;
        this.size = size;
    }

    public void changePageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public boolean isImage() {
        return contentType.startsWith("image/");
    }
}
