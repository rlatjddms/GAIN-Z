package com.project.gainz.global.storage;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

// 올린 파일을 실제로 보관하는 곳 (#178). gainz.storage.type 으로 고른다: local(기본, 디스크) / supabase(Supabase Storage)
// key는 "sheets/uuid.png"처럼 폴더/이름 형태. 형식 검사·크기 제한·key 검증은 부르는 쪽(…Storage)이 맡는다
public interface FileStore {

    void put(String key, InputStream content, long size, String contentType) throws IOException;

    // 없으면 null
    Resource get(String key) throws IOException;

    // 없는 파일을 지워도 오류가 아니다
    void delete(String key) throws IOException;
}
