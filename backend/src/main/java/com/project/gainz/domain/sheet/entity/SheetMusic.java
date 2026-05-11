package com.project.gainz.domain.sheet.entity;

import com.project.gainz.global.common.enums.InstrumentType;
import com.project.gainz.domain.sheet.enums.SheetType;
import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SheetMusic extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    @Column(nullable = false)
    private String originalAudioUrl;

    @Column(nullable = false)
    private Integer bpm;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InstrumentType instrumentType;

    @ElementCollection(targetClass = SheetType.class)
    @CollectionTable(name = "sheet_music_types", joinColumns = @JoinColumn(name = "sheet_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "sheet_type", nullable = false)
    private List<SheetType> sheetTypes = new ArrayList<>();
}
