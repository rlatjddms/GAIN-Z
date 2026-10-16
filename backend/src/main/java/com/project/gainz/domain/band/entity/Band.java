package com.project.gainz.domain.band.entity;

import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE band SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Band extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String inviteCode;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private User leader;

    private String description;

    private String profileImageUrl;

    @OneToMany(mappedBy = "band", cascade = CascadeType.ALL)
    private List<BandMember> members = new ArrayList<>();

    @Column(nullable = false)
    private Boolean isRecruiting = false;

    /* 모집 공고 */
    private String recruitmentTitle;
    private String recruitmentContent;
    private String recruitmentImageUrl;

    @Builder
    public Band(String name, String description, User leader, String inviteCode) {
        this.name = name;
        this.description = description;
        this.leader = leader;
        this.inviteCode = inviteCode;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void changeInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public void changeLeader(User leader) {
        this.leader = leader;
    }

    // 밴드 없애기: 초대 코드는 유니크라서 지운 밴드의 코드가 새 코드와 겹치지 않게 비운 뒤 소프트 삭제
    public void disband() {
        this.inviteCode = "disbanded_" + id;
        delete();
    }
}
