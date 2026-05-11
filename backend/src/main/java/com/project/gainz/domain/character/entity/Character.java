package com.project.gainz.domain.character.entity;

import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.global.common.enums.InstrumentType;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "characters")
@SQLDelete(sql = "UPDATE characters SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Character extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InstrumentType instrumentType;

    @Column(nullable = false, length = 7)
    private String instrumentColor;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private HairStyle hairStyle;

    @Column(nullable = false, length = 7)
    private String hairColor;

    @Column(nullable = false)
    private Integer level = 1;

    @Column(nullable = false)
    private Long exp = 0L;

    @Column(nullable = false)
    private Integer currentStreak = 0;

    @Column(nullable = false)
    private Integer maxStreak = 0;

    @Column(nullable = false)
    private Long totalPracticeTime = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_outfit_id", nullable = false)
    private OutfitItem currentOutfit;
}
