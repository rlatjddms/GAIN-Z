package com.project.gainz.global.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 메일을 보내지 않고 로그에 남긴다 (로컬 개발용). 인증·재설정 링크를 로그에서 복사해 쓴다
@Slf4j
@Component
@ConditionalOnProperty(name = "gainz.mail.type", havingValue = "log", matchIfMissing = true)
public class LogMailClient implements MailClient {

    @Override
    public void send(String to, String subject, String html) {
        log.info("[메일 대신 로그] 받는 사람: {} / 제목: {}\n{}", to, subject, html);
    }
}
