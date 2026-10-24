package com.project.gainz.domain.user.dto.response;

// 인증 코드가 맞으면 주는 인증 표 (#193). 가입 요청의 emailVerificationToken으로 30분 안에 한 번 쓴다
public record SignupCodeVerifyResponse(String emailVerificationToken) {}
