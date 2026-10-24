package com.project.gainz.global.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 가짜 Brevo(JDK 내장 HTTP 서버)에 붙여 요청 모양과 실패 처리를 확인한다 (#193)
class BrevoMailClientTest {

    private HttpServer server;
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> apiKey = new AtomicReference<>();
    private final AtomicInteger status = new AtomicInteger(201);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/smtp/email", exchange -> {
            apiKey.set(exchange.getRequestHeaders().getFirst("api-key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = "{\"messageId\":\"x\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private BrevoMailClient client() {
        return new BrevoMailClient("http://127.0.0.1:" + server.getAddress().getPort() + "/v3/smtp/email",
                "secret-key", "gainz@test.invalid", "GAIN-Z", objectMapper);
    }

    @Test
    void 보내는_사람_받는_사람_제목_본문을_API_키와_함께_보낸다() throws Exception {
        client().send("me@test.invalid", "[GAIN-Z] 인증", "<p>안녕</p>");

        assertThat(apiKey.get()).isEqualTo("secret-key");
        JsonNode json = objectMapper.readTree(body.get());
        assertThat(json.at("/sender/email").asText()).isEqualTo("gainz@test.invalid");
        assertThat(json.at("/sender/name").asText()).isEqualTo("GAIN-Z");
        assertThat(json.at("/to/0/email").asText()).isEqualTo("me@test.invalid");
        assertThat(json.at("/subject").asText()).isEqualTo("[GAIN-Z] 인증");
        assertThat(json.at("/htmlContent").asText()).isEqualTo("<p>안녕</p>");
    }

    @Test
    void 실패_응답이면_MailSendException() {
        status.set(401);

        assertThatThrownBy(() -> client().send("me@test.invalid", "제목", "본문")).isInstanceOf(MailSendException.class);
    }
}
