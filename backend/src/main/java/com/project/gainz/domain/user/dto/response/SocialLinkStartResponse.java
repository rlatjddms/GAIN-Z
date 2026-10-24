package com.project.gainz.domain.user.dto.response;

// 소셜 연결 시작 (#189): /oauth2/authorization/{provider}?link= 에 붙이는 5분짜리 한 번용 표
public record SocialLinkStartResponse(String linkToken) {}
