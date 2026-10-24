package com.project.gainz.global.mail;

// 메일 보내기 (#193). gainz.mail.type 으로 고른다: log(기본, 로그에 찍기 — 로컬 개발) / brevo(Brevo HTTP API — 운영)
// Render 무료 서버는 SMTP 포트를 막아서 SMTP가 아니라 HTTP API로 보낸다
public interface MailClient {

    // 실패하면 MailSendException
    void send(String to, String subject, String html);
}
