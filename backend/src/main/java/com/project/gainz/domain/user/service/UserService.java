package com.project.gainz.domain.user.service;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.band.service.BandService;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.friend.service.FriendService;
import com.project.gainz.domain.notification.service.NotificationService;
import com.project.gainz.domain.user.dto.request.PasswordChangeRequest;
import com.project.gainz.domain.user.dto.request.PasswordCreateRequest;
import com.project.gainz.domain.user.dto.request.ProfileMusicRequest;
import com.project.gainz.domain.user.dto.request.ProfileVisibilityRequest;
import com.project.gainz.domain.user.dto.request.UpdateNameRequest;
import com.project.gainz.domain.user.dto.request.WithdrawRequest;
import com.project.gainz.domain.user.dto.response.MyInfoResponse;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.RefreshTokenRepository;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.domain.record.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

// 내 계정 (마이페이지): 조회, 이름·프로필 사진·비밀번호 바꾸기, 회원 탈퇴
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileImageStorage profileImageStorage;
    private final BandService bandService;
    private final FriendService friendService;
    private final NotificationService notificationService;
    private final RecordService recordService;

    public record ProfileImageFile(Resource resource, String contentType) {
    }

    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(Long userId) {
        return MyInfoResponse.from(findUser(userId));
    }

    public MyInfoResponse updateName(Long userId, UpdateNameRequest request) {
        User user = findUser(userId);
        user.update(request.name().trim());
        return MyInfoResponse.from(user);
    }

    // 요즘 듣는 음악 설정 (링크·커버·미리듣기는 비우면 null)
    public MyInfoResponse updateProfileMusic(Long userId, ProfileMusicRequest request) {
        User user = findUser(userId);
        user.changeProfileMusic(request.title().trim(), request.artist().trim(),
                blankToNull(request.url()), blankToNull(request.artworkUrl()), blankToNull(request.previewUrl()));
        return MyInfoResponse.from(user);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public MyInfoResponse deleteProfileMusic(Long userId) {
        User user = findUser(userId);
        user.clearProfileMusic();
        return MyInfoResponse.from(user);
    }

    public MyInfoResponse changeProfileVisibility(Long userId, ProfileVisibilityRequest request) {
        User user = findUser(userId);
        user.changeProfileVisibility(request.visibility());
        return MyInfoResponse.from(user);
    }

    // 사진 바꾸기: 새 파일을 저장하고, 저장이 끝나면(커밋 뒤) 예전 파일을 지운다
    public MyInfoResponse changeProfileImage(Long userId, MultipartFile file) {
        User user = findUser(userId);
        ProfileImageStorage.StoredImage stored = profileImageStorage.store(file);
        String old = user.getProfileImage();
        user.changeProfileImage(stored.storedName(), stored.contentType());
        deleteFileAfterCommit(old);
        return MyInfoResponse.from(user);
    }

    public MyInfoResponse deleteProfileImage(Long userId) {
        User user = findUser(userId);
        String old = user.getProfileImage();
        user.changeProfileImage(null, null);
        deleteFileAfterCommit(old);
        return MyInfoResponse.from(user);
    }

    // 프로필 사진 파일 (로그인한 사람이면 누구 사진이든 볼 수 있다: 밴드 멤버 목록 등)
    @Transactional(readOnly = true)
    public ProfileImageFile getProfileImage(Long userId) {
        User user = findUser(userId);
        if (!user.hasProfileImage()) {
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_NOT_FOUND);
        }
        return new ProfileImageFile(profileImageStorage.load(user.getProfileImage()), user.getProfileImageType());
    }

    // 비밀번호가 있는 계정만. 지금 비밀번호를 확인하고, 같은 비밀번호로는 바꾸지 않는다
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = findUser(userId);
        if (!user.hasPassword()) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_PASSWORD);
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.SAME_PASSWORD);
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    // 비밀번호가 없는 계정(소셜로만 가입)이 처음 만들기 (#189). 만들면 가입한 이메일로도 로그인할 수 있다
    public void createPassword(Long userId, PasswordCreateRequest request) {
        User user = findUser(userId);
        if (user.hasPassword()) {
            throw new BusinessException(ErrorCode.PASSWORD_ALREADY_SET);
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    // 회원 탈퇴: 리프레시 토큰과 프로필 사진을 지우고 유저·캐릭터를 소프트 삭제한다.
    // 연습 기록·루틴·보관함 등은 지우지 않고 남기지만, 유저가 삭제돼 다시 볼 수 없다
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = findUser(userId);
        // 비밀번호가 있으면 확인 (소셜로만 로그인하는 계정은 확인 없이)
        if (user.hasPassword()) {
            String password = request == null ? null : request.password();
            if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
                throw new BusinessException(ErrorCode.INVALID_PASSWORD);
            }
        }

        // 밴드 먼저 (#215): 리더를 넘겨야 할 밴드가 있으면 여기서 멈추고, 리더에게 보내는 알림에 캐릭터 닉네임을 쓴다
        bandService.leaveAllBands(userId);
        // 리프레시 토큰은 이메일로 저장돼 있어서, 이메일을 비우기 전에 지운다
        refreshTokenRepository.deleteById(user.getEmail());
        characterRepository.findByUserId(userId).ifPresent(Character::delete);
        friendService.removeAll(userId);
        notificationService.removeAllOf(userId);
        recordService.removeAllOf(userId);
        deleteFileAfterCommit(user.getProfileImage());
        user.withdraw();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // DB 저장이 실패하면 파일은 그대로 둔다 (트랜잭션 밖에서 부르면 바로 지움)
    private void deleteFileAfterCommit(String storedName) {
        if (storedName == null) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            profileImageStorage.delete(storedName);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                profileImageStorage.delete(storedName);
            }
        });
    }
}
