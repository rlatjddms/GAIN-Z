package com.project.gainz.domain.user.dto.response;

// 메일을 1분 안에 다시 요청했을 때 오류 응답의 data (#193). 이 초가 지나면 다시 보낼 수 있다
public record MailRetryResponse(long retryAfterSeconds) {}
