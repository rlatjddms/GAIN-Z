package com.project.gainz.global.storage;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 가짜 Supabase Storage(JDK 내장 HTTP 서버)에 붙여 요청 모양과 응답 처리를 확인한다
class SupabaseFileStoreTest {

    private HttpServer server;
    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final Map<String, String> lastHeaders = new ConcurrentHashMap<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/storage/v1/object/", exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath().substring("/storage/v1/object/".length());
            requests.add(method + " " + path);
            exchange.getRequestHeaders().forEach((k, v) -> lastHeaders.put(k.toLowerCase(), v.get(0)));
            int status;
            byte[] body = new byte[0];
            switch (method) {
                case "POST" -> {
                    objects.put(path, exchange.getRequestBody().readAllBytes());
                    status = 200;
                }
                case "GET" -> {
                    body = objects.get(path.replaceFirst("^authenticated/", ""));
                    status = body == null ? 400 : 200;
                    if (body == null) body = "{\"statusCode\":\"404\",\"error\":\"not_found\"}".getBytes();
                }
                case "DELETE" -> status = objects.remove(path) == null ? 404 : 200;
                default -> status = 405;
            }
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            if (body.length > 0) exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private SupabaseFileStore store(String key) {
        return new SupabaseFileStore("http://127.0.0.1:" + server.getAddress().getPort() + "/", "uploads", key);
    }

    @Test
    void 버킷에_올리고_받고_지운다() throws Exception {
        SupabaseFileStore store = store("eyJ.service.role");
        byte[] data = {9, 8, 7, 6};

        store.put("sheets/a.png", new ByteArrayInputStream(data), data.length, "image/png");
        assertThat(lastHeaders).containsEntry("content-type", "image/png")
                .containsEntry("x-upsert", "true")
                .containsEntry("apikey", "eyJ.service.role")
                .containsEntry("authorization", "Bearer eyJ.service.role");

        Resource resource = store.get("sheets/a.png");
        assertThat(resource.getContentAsByteArray()).isEqualTo(data);
        assertThat(resource.contentLength()).isEqualTo(4);

        store.delete("sheets/a.png");
        assertThat(requests).containsExactly(
                "POST uploads/sheets/a.png", "GET authenticated/uploads/sheets/a.png", "DELETE uploads/sheets/a.png");
    }

    @Test
    void 없는_파일은_null이고_지워도_오류가_아니다() throws Exception {
        SupabaseFileStore store = store("eyJ.key");

        assertThat(store.get("band/none")).isNull();
        store.delete("band/none");
    }

    @Test
    void 새_형식_secret_키는_apikey에만_넣는다() throws Exception {
        store("sb_secret_abc").get("profiles/x.png");

        assertThat(lastHeaders).containsEntry("apikey", "sb_secret_abc").doesNotContainKey("authorization");
    }

    // R2로 옮길 때 쓰는 목록 (#262). 폴더 항목(id 없음)은 빼고 "폴더/이름"으로 준다
    @Test
    void 폴더의_파일_목록을_받는다() throws Exception {
        List<String> bodies = new CopyOnWriteArrayList<>();
        server.createContext("/storage/v1/object/list/", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            bodies.add(new String(exchange.getRequestBody().readAllBytes()));
            byte[] body = "[{\"name\":\"sub\",\"id\":null},{\"name\":\"a\",\"id\":\"1\"},{\"name\":\"b\",\"id\":\"2\"}]".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        assertThat(store("eyJ.key").list("band")).containsExactly("band/a", "band/b");
        assertThat(requests).containsExactly("POST /storage/v1/object/list/uploads");
        assertThat(bodies.get(0)).contains("\"prefix\":\"band\"").contains("\"offset\":0");
    }

    @Test
    void 서버_오류는_IOException으로_알린다() {
        server.removeContext("/storage/v1/object/");
        server.createContext("/storage/v1/object/", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        SupabaseFileStore store = store("eyJ.key");

        assertThatThrownBy(() -> store.put("sheets/a.png", new ByteArrayInputStream(new byte[]{1}), 1, "image/png"))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> store.get("sheets/a.png")).isInstanceOf(IOException.class);
    }
}
