package com.project.gainz.domain.band.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BandPostFile {
    @Column(name = "stored_key", nullable = false)
    private String key;
    @Column(name = "file_name", nullable = false)
    private String name;
    @Column(name = "content_type", nullable = false)
    private String type;

    public BandPostFile(String key, String name, String type) {
        this.key = key;
        this.name = name;
        this.type = type;
    }
}
