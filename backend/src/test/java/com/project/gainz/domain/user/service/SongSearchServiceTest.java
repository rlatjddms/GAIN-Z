package com.project.gainz.domain.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.gainz.domain.user.dto.response.SongSearchResponse;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 가짜 iTunes(JDK 내장 HTTP 서버)에 붙여 검색 조건·결과 정리·기억·실패 처리를 확인한다 (#228)
class SongSearchServiceTest {

    private static final String BODY = """
            {"resultCount":3,"results":[
              {"trackId":11,"trackName":"Hysteria","artistName":"Muse","collectionName":"Absolution",
               "artworkUrl100":"https://img/100x100bb.jpg","previewUrl":"https://preview/1.m4a","trackViewUrl":"https://music/1"},
              {"trackId":12,"trackName":"","artistName":"이름 없는 곡"},
              {"trackId":13,"trackName":"Starlight","artistName":"Muse"}
            ]}""";

    private HttpServer server;
    private final AtomicReference<String> query = new AtomicReference<>();
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-25T00:00:00Z"));
    private SongSearchService service;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/search", exchange -> {
            calls.incrementAndGet();
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] out = BODY.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        Clock clock = new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        };
        service = new SongSearchService(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/search"), new ObjectMapper(), clock);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void 미국_스토어를_한국어로_찾고_제목_가수가_있는_곡만_돌려준다() {
        List<SongSearchResponse> songs = service.search("  히스테리아 muse ");

        assertThat(query.get()).contains("term=%ED%9E%88%EC%8A%A4%ED%85%8C%EB%A6%AC%EC%95%84+muse", "country=US", "lang=ko_kr", "entity=song", "limit=12");
        assertThat(songs).extracting(SongSearchResponse::title).containsExactly("Hysteria", "Starlight");
        assertThat(songs.get(0)).isEqualTo(new SongSearchResponse(11L, "Hysteria", "Muse", "Absolution",
                "https://img/200x200bb.jpg", "https://preview/1.m4a", "https://music/1"));
        // 커버·미리듣기·링크가 없으면 null
        assertThat(songs.get(1).artworkUrl()).isNull();
        assertThat(songs.get(1).previewUrl()).isNull();
        assertThat(songs.get(1).url()).isNull();
    }

    @Test
    void 같은_검색어는_10분_동안_다시_묻지_않는다() {
        service.search("Muse");
        service.search("muse ");
        assertThat(calls.get()).isEqualTo(1);

        now.set(now.get().plus(SongSearchService.CACHE_TTL).plusSeconds(1));
        service.search("muse");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void 빈_검색어는_묻지_않는다() {
        assertThat(service.search("   ")).isEmpty();
        assertThat(service.search(null)).isEmpty();
        assertThat(calls.get()).isZero();
    }

    @Test
    void iTunes가_실패하면_곡_검색_실패로_알린다() {
        status.set(503);

        assertThatThrownBy(() -> service.search("muse"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SONG_SEARCH_FAILED);
    }

    @Test
    void 너무_긴_제목은_자르고_긴_링크는_비운다() {
        String body = "{\"results\":[{\"trackId\":1,\"trackName\":\"" + "가".repeat(150) + "\",\"artistName\":\"A\",\"trackViewUrl\":\"https://x/" + "a".repeat(600) + "\"}]}";

        SongSearchResponse song = service.parse(body).get(0);

        assertThat(song.title()).hasSize(SongSearchService.TITLE_MAX);
        assertThat(song.url()).isNull();
    }
}
