package com.project.gainz.domain.user.entity;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.user.enums.ProfileVisibility;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE user SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class User extends BaseEntity {

    public static final String WITHDRAWN_NAME = "탈퇴한 사용자";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    // 이메일 인증 여부 (#193). 이 칸이 생기기 전에 가입한 계정은 null → 인증된 것으로 본다
    private Boolean emailVerified;

    // 처음 가입한 방법. 로그인할 수 있는 방법은 password(있으면 이메일)와 socialAccounts (#189)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider provider;

    // 연결한 소셜 로그인 (#189)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Character character;

    // 프로필 사진 (업로드 폴더/profiles 안의 파일 이름). 없으면 기본 프로필
    private String profileImage;

    @Column(length = 20)
    private String profileImageType;

    // 요즘 듣는 음악 (제목·아티스트는 함께 있거나 함께 없다, 링크는 선택)
    @Column(length = 100)
    private String profileMusicTitle;

    @Column(length = 100)
    private String profileMusicArtist;

    @Column(length = 500)
    private String profileMusicUrl;

    // 곡 검색으로 골랐을 때만 (Apple 커버·30초 미리듣기 주소, #187)
    @Column(length = 500)
    private String profileMusicArtworkUrl;

    @Column(length = 500)
    private String profileMusicPreviewUrl;

    // 프로필 공개 범위. 컬럼이 나중에 생겨서 기존 사용자는 null → 전체 공개로 본다
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProfileVisibility profileVisibility;

    // 친구 코드 (처음 친구 화면을 열 때 만든다)
    @Column(unique = true, length = 8)
    private String friendCode;

    @Builder
    public User(String email, String password, SocialProvider provider, UserRole role, Boolean emailVerified) {
        this.email = email;
        this.password = password;
        this.provider = provider;
        this.role = role;
        this.emailVerified = emailVerified;
    }

    public boolean isEmailVerified() {
        return !Boolean.FALSE.equals(emailVerified);
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    // 화면에 보이는 이름은 캐릭터 닉네임 (#227). 캐릭터가 없을 때 쓰는 이름: 탈퇴 회원은 "탈퇴한 사용자", 아니면 이메일 앞부분
    public String nameWithoutCharacter() {
        return Boolean.TRUE.equals(getIsDeleted()) ? WITHDRAWN_NAME : email.split("@")[0];
    }

    public boolean hasPassword() {
        return password != null;
    }

    /* ---------- 소셜 연결 (#189) ---------- */

    public Optional<SocialAccount> findSocialAccount(SocialProvider provider) {
        return socialAccounts.stream().filter(a -> a.getProvider() == provider).findFirst();
    }

    public List<SocialProvider> linkedProviders() {
        return socialAccounts.stream().map(SocialAccount::getProvider).sorted().toList();
    }

    public void addSocialAccount(SocialProvider provider, String providerId) {
        socialAccounts.add(new SocialAccount(this, provider, providerId));
    }

    public void removeSocialAccount(SocialProvider provider) {
        socialAccounts.removeIf(a -> a.getProvider() == provider);
    }

    // 비밀번호(이메일 로그인) + 연결된 소셜 수
    public int loginMethodCount() {
        return (hasPassword() ? 1 : 0) + socialAccounts.size();
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

    public void changeProfileMusic(String title, String artist, String url) {
        changeProfileMusic(title, artist, url, null, null);
    }

    public void changeProfileMusic(String title, String artist, String url, String artworkUrl, String previewUrl) {
        this.profileMusicTitle = title;
        this.profileMusicArtist = artist;
        this.profileMusicUrl = url;
        this.profileMusicArtworkUrl = artworkUrl;
        this.profileMusicPreviewUrl = previewUrl;
    }

    public void clearProfileMusic() {
        changeProfileMusic(null, null, null, null, null);
    }

    public boolean hasProfileMusic() {
        return profileMusicTitle != null;
    }

    public ProfileVisibility getProfileVisibility() {
        return profileVisibility != null ? profileVisibility : ProfileVisibility.PUBLIC;
    }

    public void changeProfileVisibility(ProfileVisibility visibility) {
        this.profileVisibility = visibility;
    }

    public boolean hasProfileImage() {
        return profileImage != null;
    }

    // 회원 탈퇴: 같은 이메일로 다시 가입할 수 있게 이메일을 비우고(유니크 제약) 개인정보를 지운 뒤 소프트 삭제
    public void withdraw() {
        this.email = "withdrawn_" + id + "_" + System.currentTimeMillis() + "@gainz.invalid";
        this.password = null;
        this.profileImage = null;
        this.profileImageType = null;
        this.friendCode = null;
        // 같은 소셜 계정으로 다시 가입할 수 있게 연결을 지운다
        socialAccounts.clear();
        clearProfileMusic();
        delete();
    }
}
