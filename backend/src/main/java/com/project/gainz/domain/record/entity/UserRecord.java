package com.project.gainz.domain.record.entity;

import com.project.gainz.domain.record.enums.RecordType;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 얻은 레코드 한 장. createdAt이 얻은 때
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_record", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "record_type"}))
public class UserRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // MariaDB enum 타입으로 만들면 레코드를 추가할 때마다 컬럼을 고쳐야 해서 varchar로 둔다
    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 40, columnDefinition = "varchar(40)")
    private RecordType recordType;

    public UserRecord(User user, RecordType recordType) {
        this.user = user;
        this.recordType = recordType;
    }
}
