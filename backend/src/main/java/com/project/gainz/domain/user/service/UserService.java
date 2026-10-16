package com.project.gainz.domain.user.service;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.user.dto.request.PasswordChangeRequest;
import com.project.gainz.domain.user.dto.request.UpdateNameRequest;
import com.project.gainz.domain.user.dto.request.WithdrawRequest;
import com.project.gainz.domain.user.dto.response.MyInfoResponse;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.RefreshTokenRepository;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 내 계정 (마이페이지): 조회, 이름·비밀번호 바꾸기, 회원 탈퇴
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(Long userId) {
        User user = findUser(userId);
        return MyInfoResponse.of(user, characterRepository.findByUserId(userId).orElse(null));
    }

    public MyInfoResponse updateName(Long userId, UpdateNameRequest request) {
        User user = findUser(userId);
        user.update(request.name().trim());
        return MyInfoResponse.of(user, characterRepository.findByUserId(userId).orElse(null));
    }

    // 이메일 가입자만. 지금 비밀번호를 확인하고, 같은 비밀번호로는 바꾸지 않는다
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = findUser(userId);
        if (!user.isLocal()) {
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

    // 회원 탈퇴: 리프레시 토큰을 지우고 유저·캐릭터를 소프트 삭제한다.
    // 연습 기록·루틴·보관함 등은 지우지 않고 남기지만, 유저가 삭제돼 다시 볼 수 없다
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = findUser(userId);
        if (user.isLocal()) {
            String password = request == null ? null : request.password();
            if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
                throw new BusinessException(ErrorCode.INVALID_PASSWORD);
            }
        }

        // 리프레시 토큰은 이메일로 저장돼 있어서, 이메일을 비우기 전에 지운다
        refreshTokenRepository.deleteById(user.getEmail());
        characterRepository.findByUserId(userId).ifPresent(Character::delete);
        user.withdraw();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
