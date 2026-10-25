package com.project.gainz.domain.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.gainz.domain.user.dto.response.SongSearchResponse;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// 프로필 뮤직 곡 검색을 대신 해 준다 (#228).
// 아이폰 브라우저가 iTunes 검색을 부르면 음악 앱 주소(musics://)로 돌려보내져서 결과를 못 받는다. 서버는 아이폰이 아니라 정상으로 받는다.
// iTunes는 같은 IP에서 분당 약 20번까지라, 프론트는 브라우저로 먼저 찾고 안 될 때만 여기로 오고, 같은 검색어는 10분 동안 기억한다
@Service
public class SongSearchService {

    static final int LIMIT = 12;
    static final int TERM_MAX = 60;
    static final Duration CACHE_TTL = Duration.ofMinutes(10);
    static final int CACHE_MAX = 200;
    // ProfileMusicRequest와 같은 길이
    static final int TITLE_MAX = 100;
    static final int ARTIST_MAX = 100;
    static final int URL_MAX = 500;

    private final URI searchUrl;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private record Cached(List<SongSearchResponse> songs, Instant until) {
    }

    // 오래 안 쓴 검색어부터 지운다
    private final Map<String, Cached> cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Cached> eldest) {
            return size() > CACHE_MAX;
        }
    });

    // 생성자가 둘이라 스프링이 쓸 것을 정해 준다 (아래는 테스트용: 가짜 서버 주소·시계)
    @Autowired
    public SongSearchService(@Value("${gainz.music.search-url:https://itunes.apple.com/search}") String searchUrl,
                             ObjectMapper objectMapper) {
        this(URI.create(searchUrl), objectMapper, Clock.systemUTC());
    }

    SongSearchService(URI searchUrl, ObjectMapper objectMapper, Clock clock) {
        this.searchUrl = searchUrl;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public List<SongSearchResponse> search(String rawTerm) {
        String term = rawTerm == null ? "" : rawTerm.strip();
        if (term.isEmpty()) return List.of();
        if (term.length() > TERM_MAX) term = term.substring(0, TERM_MAX);
        String key = term.toLowerCase(Locale.ROOT);
        Instant now = clock.instant();
        Cached cached = cache.get(key);
        if (cached != null && cached.until().isAfter(now)) return cached.songs();

        List<SongSearchResponse> songs = parse(fetch(term));
        cache.put(key, new Cached(songs, now.plus(CACHE_TTL)));
        return songs;
    }

    // 한국 스토어는 음악 결과가 없어서 미국 스토어를 한국어로 찾는다 (프론트와 같은 조건)
    private String fetch(String term) {
        String query = "term=" + URLEncoder.encode(term, StandardCharsets.UTF_8)
                + "&country=US&lang=ko_kr&media=music&entity=song&limit=" + LIMIT;
        HttpRequest request = HttpRequest.newBuilder(URI.create(searchUrl + "?" + query))
                .timeout(Duration.ofSeconds(8))
                .header("accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new BusinessException(ErrorCode.SONG_SEARCH_FAILED);
            return response.body();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SONG_SEARCH_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SONG_SEARCH_FAILED);
        }
    }

    // iTunes 응답 → 제목·가수가 있는 곡만, 100px 커버는 화면용 200px로, 너무 긴 값은 자르거나 비운다
    List<SongSearchResponse> parse(String body) {
        try {
            List<SongSearchResponse> songs = new ArrayList<>();
            for (JsonNode r : objectMapper.readTree(body).path("results")) {
                String title = r.path("trackName").asText("");
                String artist = r.path("artistName").asText("");
                if (title.isBlank() || artist.isBlank()) continue;
                String artwork = textOrNull(r, "artworkUrl100");
                String url = textOrNull(r, "trackViewUrl");
                songs.add(new SongSearchResponse(
                        r.path("trackId").isNumber() ? r.path("trackId").asLong() : null,
                        cut(title, TITLE_MAX),
                        cut(artist, ARTIST_MAX),
                        r.path("collectionName").asText(""),
                        artwork == null ? null : artwork.replace("100x100bb", "200x200bb"),
                        textOrNull(r, "previewUrl"),
                        url != null && url.length() <= URL_MAX ? url : null));
            }
            return songs;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SONG_SEARCH_FAILED);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    private static String cut(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
