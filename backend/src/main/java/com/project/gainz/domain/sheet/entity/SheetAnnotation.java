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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 악보 장 위 내 필기 (#167). 원본 파일은 그대로 두고 사용자마다, 장(PDF는 쪽)마다 따로 저장한다
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "page_id", "pdf_page"}))
public class SheetAnnotation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private SheetPage sheetPage;

    @Column(nullable = false)
    private Integer pdfPage;         // PDF 안 쪽 번호 (1부터). 그림 장은 1

    // 필기 JSON { w, h, strokes: [{ t, c, s, p: [x, y, …] }] } (프론트 stores/annotation.js)
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String data;

    @Builder
    public SheetAnnotation(User user, SheetPage sheetPage, Integer pdfPage, String data) {
        this.user = user;
        this.sheetPage = sheetPage;
        this.pdfPage = pdfPage;
        this.data = data;
    }

    public void updateData(String data) {
        this.data = data;
    }
}
