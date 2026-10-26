package com.project.gainz.global.storage;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

// R2 서명 주소 모양과, R2에 아직 없는 옛 파일을 Supabase에서 읽는지 (#262).
// 가짜 서버(JDK 내장 HTTP 서버) 하나가 R2(/gainz/…)와 Supabase(/storage/v1/object/…)를 함께 흉내 낸다
class R2FileStoreTest {

    private HttpServer server;
    private final Map<String, byte[]> legacyObjects = new ConcurrentHashMap<>();
    private final List<String> requests = new CopyOnWriteArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void 서명_주소에_버킷_key_기한_받을_때_이름과_형식이_들어간다() {
        R2FileStore store = new R2FileStore("acc", "gainz", "id", "key", "", "", "uploads", "");

        String put = store.presignPut("tmp/a", "video/mp4", Duration.ofMinutes(30));
        String get = URLDecoder.decode(store.presignGet("band/b", "공연.mp4", "video/mp4", true, Duration.ofMinutes(10)), StandardCharsets.UTF_8);

        assertThat(put).startsWith("https://acc.r2.cloudflarestorage.com/gainz/tmp/a?")
                .contains("X-Amz-Expires=1800").contains("X-Amz-Signature=");
        assertThat(get).startsWith("https://acc.r2.cloudflarestorage.com/gainz/band/b?")
                .contains("X-Amz-Expires=600")
                .contains("response-content-type=video/mp4")
                .contains("response-content-disposition=attachment");
    }

    @Test
    void R2에_없는_옛_파일은_Supabase에서_읽고_지울_때는_둘_다_지운다() throws IOException {
        startServer();
        legacyObjects.put("sheets/old.png", new byte[] {1, 2, 3});
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        R2FileStore store = new R2FileStore("", "gainz", "id", "key", url, url, "uploads", "sb_secret_test");

        assertThat(store.get("sheets/old.png").getContentAsByteArray()).containsExactly(1, 2, 3);
        assertThat(store.size("sheets/old.png")).isEqualTo(3);
        assertThat(store.head("sheets/old.png", 2)).containsExactly(1, 2);
        // 옛 파일은 서명 주소 대신 서버가 보내 준다
        assertThat(store.presignGet("sheets/old.png", "a.png", "image/png", false, Duration.ofMinutes(10))).isNull();
        assertThat(store.get("sheets/none.png")).isNull();
        assertThat(store.size("sheets/none.png")).isEqualTo(-1);

        store.delete("sheets/old.png");
        assertThat(requests).contains("DELETE /gainz/sheets/old.png", "DELETE /storage/v1/object/uploads/sheets/old.png");
        assertThat(legacyObjects).isEmpty();
    }

    @Test
    void Supabase_정보가_없으면_옛_파일을_찾지_않는다() {
        R2FileStore store = new R2FileStore("acc", "gainz", "id", "key", "", "https://x.supabase.co", "uploads", "");
        assertThat(store.legacy()).isNull();
    }

    private void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // R2: 아무 파일도 없다
        server.createContext("/gainz/", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(exchange.getRequestMethod().equals("DELETE") ? 204 : 404, -1);
            exchange.close();
        });
        server.createContext("/storage/v1/object/", exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            requests.add(method + " " + path);
            String key = path.replaceFirst("^/storage/v1/object/(authenticated/)?uploads/", "");
            byte[] body = new byte[0];
            int status;
            if (method.equals("GET")) {
                byte[] found = legacyObjects.get(key);
                status = found == null ? 400 : 200;
                body = found == null ? "{\"error\":\"not_found\"}".getBytes() : found;
            } else if (method.equals("DELETE")) {
                status = legacyObjects.remove(key) == null ? 404 : 200;
            } else {
                status = 405;
            }
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            if (body.length > 0) exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }
}
