package com.project.gainz.domain.band.entity;

import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.OrderColumn;
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
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE band_post SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class BandPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_id", nullable = false)
    private Band band;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String fileUrl;

    private String fileName;

    private String fileType;

    @ElementCollection
    @CollectionTable(name = "band_post_attachment", joinColumns = @JoinColumn(name = "post_id"))
    @OrderColumn(name = "attachment_order")
    private List<BandPostFile> files = new ArrayList<>();

    public void replaceFiles(List<BandPostFile> next) {
        files.clear();
        files.addAll(next);
    }

    @Column(nullable = false)
    private Boolean isNotice = false;

    public BandPost(Band band, User author, String title, String content) {
        this.band = band;
        this.author = author;
        update(title, content);
    }

    public void update(String title, String content) {
        this.title = title.trim();
        this.content = content.trim();
    }

    public void changeNotice(boolean notice) { this.isNotice = notice; }

    public void changeFile(String storedName, String name, String type) {
        this.fileUrl = storedName;
        this.fileName = name;
        this.fileType = type;
    }
}
