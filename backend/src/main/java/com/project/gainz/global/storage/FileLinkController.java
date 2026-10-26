package com.project.gainz.global.storage;

import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// FileLinks가 내준 서버 주소로 파일을 올리고 받는다 (#262. R2를 안 쓰는 로컬 개발·옛 Supabase 파일).
// 로그인 토큰 대신 서명한 표로 확인하므로 <video src>처럼 헤더를 못 싣는 곳에서도 쓸 수 있다 (SecurityConfig에서 열어 둠)
@RestController
@RequiredArgsConstructor
public class FileLinkController {

    private final FileLinks links;
    private final FileStore store;

    @PutMapping(FileLinks.SERVER_PATH + "{token}")
    public ResponseEntity<Void> upload(@PathVariable String token, HttpServletRequest request) throws IOException {
        FileLinks.Ticket ticket = links.verify(token, "put");
        long size = request.getContentLengthLong();
        if (size <= 0 || size > ticket.maxBytes()) throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        try (InputStream in = request.getInputStream()) {
            store.put(ticket.key(), new BoundedInputStream(in, size), size, ticket.type());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping(FileLinks.SERVER_PATH + "{token}")
    public ResponseEntity<Resource> download(@PathVariable String token) throws IOException {
        FileLinks.Ticket ticket = links.verify(token, "get");
        Resource resource = store.get(ticket.key());
        if (resource == null) throw new BusinessException(ErrorCode.BAND_FILE_NOT_FOUND);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(ticket.type()))
                .header("Content-Disposition", ContentDisposition.builder(ticket.download() ? "attachment" : "inline")
                        .filename(ticket.name(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "private, max-age=600")
                .body(resource);
    }

    // Content-Length보다 많이 보내도 그만큼만 읽는다
    private static final class BoundedInputStream extends InputStream {
        private final InputStream in;
        private long left;

        private BoundedInputStream(InputStream in, long limit) {
            this.in = in;
            this.left = limit;
        }

        @Override
        public int read() throws IOException {
            if (left <= 0) return -1;
            int b = in.read();
            if (b >= 0) left--;
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (left <= 0) return -1;
            int n = in.read(buffer, offset, (int) Math.min(length, left));
            if (n > 0) left -= n;
            return n;
        }
    }
}
