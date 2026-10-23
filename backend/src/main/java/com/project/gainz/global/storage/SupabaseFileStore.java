package com.project.gainz.global.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Supabase Storage(비공개 버킷)에 저장한다 (#178). 운영(Render)처럼 디스크가 재배포 때 지워지는 곳에서 쓴다.
// 브라우저는 지금처럼 백엔드 API로만 파일을 받으므로 권한 검사는 그대로이고, 버킷 키는 서버만 안다.
// 받은 파일은 메모리에 올려 돌려준다 (가장 큰 녹음 30MB). 그래야 Content-Length·Range(오디오 탐색)가 디스크 때와 같이 동작한다
@Component
@ConditionalOnProperty(name = "gainz.storage.type", havingValue = "supabase")
public class SupabaseFileStore implements FileStore {

    private final String objectUrl;
    private final String bucket;
    private final String serviceKey;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public SupabaseFileStore(@Value("${gainz.storage.supabase.url}") String url,
                             @Value("${gainz.storage.supabase.bucket:uploads}") String bucket,
                             @Value("${gainz.storage.supabase.service-key}") String serviceKey) {
        this.objectUrl = url.replaceAll("/+$", "") + "/storage/v1/object/";
        this.bucket = bucket;
        this.serviceKey = serviceKey;
    }

    @Override
    public void put(String key, InputStream content, long size, String contentType) throws IOException {
        HttpRequest request = request(objectUrl + bucket + "/" + key)
                .header("Content-Type", contentType)
                .header("x-upsert", "true")
                .POST(HttpRequest.BodyPublishers.fromPublisher(HttpRequest.BodyPublishers.ofInputStream(() -> content), size))
                .build();
        HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Supabase 업로드 실패 " + response.statusCode() + ": " + response.body());
        }
    }

    @Override
    public Resource get(String key) throws IOException {
        HttpRequest request = request(objectUrl + "authenticated/" + bucket + "/" + key).GET().build();
        HttpResponse<byte[]> response = send(request, HttpResponse.BodyHandlers.ofByteArray());
        int status = response.statusCode();
        if (status == 200) return new ByteArrayResource(response.body());
        // 없는 파일은 404 또는 400(본문에 not_found)으로 온다
        if (status == 404 || status == 400) return null;
        throw new IOException("Supabase 다운로드 실패 " + status);
    }

    @Override
    public void delete(String key) throws IOException {
        HttpRequest request = request(objectUrl + bucket + "/" + key).DELETE().build();
        HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status / 100 != 2 && status != 404 && status != 400) {
            throw new IOException("Supabase 삭제 실패 " + status + ": " + response.body());
        }
    }

    // service_role(JWT) 키는 Authorization에도, 새 형식 secret 키(sb_secret_…)는 apikey에만 넣는다
    private HttpRequest.Builder request(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("apikey", serviceKey);
        if (serviceKey.startsWith("eyJ")) {
            builder.header("Authorization", "Bearer " + serviceKey);
        }
        return builder;
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            return http.send(request, handler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Supabase 요청이 중단됨", e);
        }
    }
}
