package com.project.gainz.global.storage;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

// 올린 파일을 실제로 보관하는 곳 (#178). gainz.storage.type 으로 고른다:
// local(기본, 디스크) / r2(Cloudflare R2, 운영 #262) / supabase(Supabase Storage, #262 전 운영)
// key는 "sheets/uuid.png"처럼 폴더/이름 형태. 형식 검사·크기 제한·key 검증은 부르는 쪽(…Storage)이 맡는다
public interface FileStore {

    void put(String key, InputStream content, long size, String contentType) throws IOException;

    // 없으면 null
    Resource get(String key) throws IOException;

    // 없는 파일을 지워도 오류가 아니다
    void delete(String key) throws IOException;

    // 파일 크기. 없으면 -1
    default long size(String key) throws IOException {
        Resource resource = get(key);
        return resource == null ? -1 : resource.contentLength();
    }

    // 형식을 알아보려고 앞부분만 읽는다. 없으면 null
    default byte[] head(String key, int bytes) throws IOException {
        Resource resource = get(key);
        if (resource == null) return null;
        try (InputStream in = resource.getInputStream()) {
            return in.readNBytes(bytes);
        }
    }

    // 브라우저가 바로 올린 임시 파일(tmp/…)을 제자리로 옮긴다
    default void move(String from, String to, String contentType) throws IOException {
        Resource resource = get(from);
        if (resource == null) throw new IOException("옮길 파일이 없음: " + from);
        try (InputStream in = resource.getInputStream()) {
            put(to, in, resource.contentLength(), contentType);
        }
        delete(from);
    }

    // 브라우저가 서버를 거치지 않고 저장소에 바로 올리는 주소 (PUT). 못 만들면 null → 서버가 받는 주소(FileLinks)를 쓴다
    default String presignPut(String key, String contentType, Duration ttl) {
        return null;
    }

    // 브라우저가 저장소에서 바로 받는 주소 (GET). 못 만들면 null → 서버가 보내 주는 주소(FileLinks)를 쓴다
    default String presignGet(String key, String name, String contentType, boolean download, Duration ttl) {
        return null;
    }
}
