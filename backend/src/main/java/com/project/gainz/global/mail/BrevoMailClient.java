package com.project.gainz.global.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

// Brevo 거래 메일 API로 보낸다 (#193). 무료 하루 300통, 보내는 사람 이메일을 Brevo에서 미리 인증해 둬야 한다
@Component
@ConditionalOnProperty(name = "gainz.mail.type", havingValue = "brevo")
public class BrevoMailClient implements MailClient {

    private final URI endpoint;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public BrevoMailClient(@Value("${gainz.mail.brevo.url:https://api.brevo.com/v3/smtp/email}") String url,
                           @Value("${gainz.mail.brevo.api-key}") String apiKey,
                           @Value("${gainz.mail.from}") String fromEmail,
                           @Value("${gainz.mail.from-name:GAIN-Z}") String fromName,
                           ObjectMapper objectMapper) {
        this.endpoint = URI.create(url);
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.objectMapper = objectMapper;
    }

    @Override
    public void send(String to, String subject, String html) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "sender", Map.of("name", fromName, "email", fromEmail),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", html));
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(20))
                    .header("api-key", apiKey)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new MailSendException("Brevo 메일 전송 실패 " + response.statusCode() + ": " + response.body(), null);
            }
        } catch (IOException e) {
            throw new MailSendException("Brevo 메일 전송 실패", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MailSendException("Brevo 메일 전송이 중단됨", e);
        }
    }
}
