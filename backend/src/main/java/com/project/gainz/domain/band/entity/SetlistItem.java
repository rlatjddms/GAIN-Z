package com.project.gainz.domain.band.entity;

import com.project.gainz.domain.sheet.entity.SheetMusic;
import com.project.gainz.global.common.BaseEntity;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE setlist_item SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class SetlistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setlist_id", nullable = false)
    private Setlist setlist;

    private String description;

    /* AI 추출 악보 데이터가 연결된 경우 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id")
    private SheetMusic sheetMusic;

    /* sheetMusic이 없는 경우 직접 입력 */
    private String customTitle;
    private String customArtist;
    private String fileUrl;

    @Column(nullable = false)
    private Integer playOrder;
}
