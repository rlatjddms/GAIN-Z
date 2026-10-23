package com.project.gainz.domain.user.dto.request;

// 회원 탈퇴. 이메일 가입자는 비밀번호로 한 번 더 확인하고, 소셜 가입자는 비워 둔다
public record WithdrawRequest(
        String password
) {
}
