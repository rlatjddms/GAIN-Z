package com.project.gainz.global.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// 서버 디스크에 저장한다 (기본: 사용자 홈/.gainz/uploads, gainz.upload-dir로 바꿀 수 있음). 로컬 개발·docker compose용
@Component
@ConditionalOnProperty(name = "gainz.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStore implements FileStore {

    private final Path baseDir;

    public LocalFileStore(@Value("${gainz.upload-dir:${user.home}/.gainz/uploads}") String uploadDir) {
        this.baseDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public void put(String key, InputStream content, long size, String contentType) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        try {
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(target);
            throw e;
        }
    }

    @Override
    public Resource get(String key) throws IOException {
        Path path = resolve(key);
        return Files.isRegularFile(path) && Files.isReadable(path) ? new FileSystemResource(path) : null;
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    // 업로드 폴더 밖을 가리키는 key는 막는다
    private Path resolve(String key) throws IOException {
        Path path = baseDir.resolve(key).normalize();
        if (!path.startsWith(baseDir) || path.equals(baseDir)) {
            throw new IOException("잘못된 파일 경로: " + key);
        }
        return path;
    }
}
