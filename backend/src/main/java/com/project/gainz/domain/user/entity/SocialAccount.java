package com.project.gainz.domain.user.entity;

import com.project.gainz.domain.user.enums.SocialProvider;
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

// 계정에 연결한 소셜 로그인 (#189). 한 계정에 소셜 종류마다 하나, 한 소셜 계정은 한 GAIN-Z 계정에만.
// providerId(소셜 고유 ID)가 null이면 이 표가 생기기 전에 가입한 소셜 계정 → 다음 소셜 로그인 때 채운다
@Entity
@Getter
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_social_account_provider_id", columnNames = {"provider", "provider_id"}),
        @UniqueConstraint(name = "uk_social_account_user_provider", columnNames = {"user_id", "provider"}),
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private SocialProvider provider;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    SocialAccount(User user, SocialProvider provider, String providerId) {
        this.user = user;
        this.provider = provider;
        this.providerId = providerId;
    }

    public void fillProviderId(String providerId) {
        this.providerId = providerId;
    }
}
