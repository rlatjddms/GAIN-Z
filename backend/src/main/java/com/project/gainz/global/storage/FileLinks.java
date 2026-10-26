package com.project.gainz.global.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

// 브라우저가 파일을 바로 올리고 받는 몇 분짜리 주소 (#262). 권한 검사는 주소를 내주기 전에 부르는 쪽이 한다.
// R2는 저장소가 서명한 주소(presign)를 그대로 주고, 못 만드는 저장소(local·supabase)는 서버 주소 /api/v1/files/{표}를 준다.
// 표는 jwt.secret으로 서명해서 바꿀 수 없고, 서버 주소는 "/api/…"로 시작하니 프론트가 API 주소를 앞에 붙인다
@Component
public class FileLinks {

    public static final String SERVER_PATH = "/api/v1/files/";
    static final Duration UPLOAD_TTL = Duration.ofMinutes(30);
    static final Duration DOWNLOAD_TTL = Duration.ofMinutes(10);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    // op: put(올리기) / get(받기). maxBytes는 올리기만, name·download는 받기만 쓴다
    public record Ticket(String op, String key, String type, long maxBytes, String name, boolean download, long exp) {}

    private final FileStore store;
    private final byte[] secret;
    private final Clock clock;

    // 생성자가 둘이라 Spring이 쓸 것을 표시한다 (#310)
    @Autowired
    public FileLinks(FileStore store, @Value("${jwt.secret}") String secret) {
        this(store, secret, Clock.systemUTC());
    }

    FileLinks(FileStore store, String secret, Clock clock) {
        this.store = store;
        this.secret = ("file-link:" + secret).getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    // 브라우저가 Content-Type을 이 값으로 PUT 해야 한다
    public String uploadUrl(String key, String contentType, long maxBytes) {
        String url = store.presignPut(key, contentType, UPLOAD_TTL);
        return url != null ? url : serverUrl(new Ticket("put", key, contentType, maxBytes, null, false, expiry(UPLOAD_TTL)));
    }

    public String downloadUrl(String key, String name, String contentType, boolean download) {
        String url = store.presignGet(key, name, contentType, download, DOWNLOAD_TTL);
        return url != null ? url : serverUrl(new Ticket("get", key, contentType, 0, name, download, expiry(DOWNLOAD_TTL)));
    }

    // 서버 주소의 표를 확인한다. 고쳤거나 시간이 지났거나 다른 용도면 FILE_LINK_INVALID
    public Ticket verify(String token, String op) {
        int dot = token == null ? -1 : token.indexOf('.');
        if (dot <= 0) throw new BusinessException(ErrorCode.FILE_LINK_INVALID);
        String payload = token.substring(0, dot);
        byte[] given;
        try {
            given = Base64.getUrlDecoder().decode(token.substring(dot + 1));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.FILE_LINK_INVALID);
        }
        if (!MessageDigest.isEqual(sign(payload), given)) throw new BusinessException(ErrorCode.FILE_LINK_INVALID);
        Ticket ticket;
        try {
            ticket = JSON.readValue(Base64.getUrlDecoder().decode(payload), Ticket.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_LINK_INVALID);
        }
        if (!op.equals(ticket.op()) || ticket.exp() < clock.millis()) throw new BusinessException(ErrorCode.FILE_LINK_INVALID);
        return ticket;
    }

    private String serverUrl(Ticket ticket) {
        String payload;
        try {
            payload = ENCODER.encodeToString(JSON.writeValueAsBytes(ticket));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return SERVER_PATH + payload + "." + ENCODER.encodeToString(sign(payload));
    }

    private long expiry(Duration ttl) {
        return clock.millis() + ttl.toMillis();
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
