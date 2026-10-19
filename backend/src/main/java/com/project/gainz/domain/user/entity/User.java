package com.project.gainz.domain.user.entity;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE user SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Character character;

    // 프로필 사진 (업로드 폴더/profiles 안의 파일 이름). 없으면 기본 프로필
    private String profileImage;

    @Column(length = 20)
    private String profileImageType;

    // 친구 코드 (처음 친구 화면을 열 때 만든다)
    @Column(unique = true, length = 8)
    private String friendCode;

    @Builder
    public User(String email, String password, String name, SocialProvider provider, UserRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.provider = provider;
        this.role = role;
    }

    public User update(String name) {
        this.name = name;
        return this;
    }

    public boolean isLocal() {
        return provider == SocialProvider.LOCAL;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeProfileImage(String storedName, String contentType) {
        this.profileImage = storedName;
        this.profileImageType = contentType;
    }

    public void changeFriendCode(String friendCode) {
        this.friendCode = friendCode;
    }

    public boolean hasProfileImage() {
        return profileImage != null;
    }

    // 회원 탈퇴: 같은 이메일로 다시 가입할 수 있게 이메일을 비우고(유니크 제약) 개인정보를 지운 뒤 소프트 삭제
    public void withdraw() {
        this.email = "withdrawn_" + id + "_" + System.currentTimeMillis() + "@gainz.invalid";
        this.password = null;
        this.name = "탈퇴한 사용자";
        this.profileImage = null;
        this.profileImageType = null;
        this.friendCode = null;
        delete();
    }
}
