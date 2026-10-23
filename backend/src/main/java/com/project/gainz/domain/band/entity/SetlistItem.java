package com.project.gainz.domain.band.entity;

import com.project.gainz.domain.sheet.entity.SheetMusic;
import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
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
@SQLDelete(sql = "UPDATE setlist_item SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class SetlistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setlist_id", nullable = false)
    private Setlist setlist;

    // 합주 메모
    @Column(length = 500)
    private String description;

    /* 보관함 악보가 연결된 경우 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id")
    private SheetMusic sheetMusic;

    /* 곡 제목·아티스트 (악보를 연결해도 화면에 보일 이름으로 저장) */
    private String customTitle;
    private String customArtist;
    private String fileUrl;

    @Column(nullable = false)
    private Integer playOrder;

    // 이 곡을 함께 준비할 멤버 (파트 담당자)
    @ManyToMany
    @JoinTable(name = "setlist_item_assignee",
            joinColumns = @JoinColumn(name = "setlist_item_id"),
            inverseJoinColumns = @JoinColumn(name = "band_member_id"))
    @OrderColumn(name = "assignee_order")
    private List<BandMember> assignees = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "setlist_item_attachment", joinColumns = @JoinColumn(name = "setlist_item_id"))
    @OrderColumn(name = "attachment_order")
    private List<BandPostFile> files = new ArrayList<>();

    public SetlistItem(Setlist setlist, Integer playOrder) {
        this.setlist = setlist;
        this.playOrder = playOrder;
    }

    public void update(String title, String artist, String memo, SheetMusic sheetMusic) {
        this.customTitle = title;
        this.customArtist = artist;
        this.description = memo;
        this.sheetMusic = sheetMusic;
    }

    public void replaceAssignees(List<BandMember> next) {
        assignees.clear();
        assignees.addAll(next);
    }

    public void replaceFiles(List<BandPostFile> next) {
        files.clear();
        files.addAll(next);
    }

    public void changePlayOrder(Integer playOrder) {
        this.playOrder = playOrder;
    }
}
