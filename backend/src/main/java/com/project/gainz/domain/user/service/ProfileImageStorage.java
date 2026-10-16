package com.project.gainz.domain.user.service;

import com.project.gainz.domain.sheet.service.SheetFileStorage;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

// 프로필 사진 파일 저장 (업로드 폴더/profiles). 이미지(PNG·JPG·WEBP)만, 5MB까지
@Slf4j
@Component
public class ProfileImageStorage {

    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    public record StoredImage(String storedName, String contentType) {
    }

    private final Path baseDir;

    public ProfileImageStorage(@Value("${gainz.upload-dir:${user.home}/.gainz/uploads}") String uploadDir) {
        this.baseDir = Path.of(uploadDir, "profiles").toAbsolutePath().normalize();
    }

    public StoredImage store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_REQUIRED);
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_TOO_LARGE);
        }
        // 확장자·Content-Type 대신 파일 앞부분으로 형식을 판단한다
        String contentType;
        try (InputStream in = file.getInputStream()) {
            contentType = SheetFileStorage.detectType(in.readNBytes(12));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_STORAGE_FAILED);
        }
        if (contentType == null || !IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_PROFILE_IMAGE);
        }
        String storedName = UUID.randomUUID() + switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            default -> ".webp";
        };
        try {
            Files.createDirectories(baseDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, baseDir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("프로필 사진 저장 실패: {}", baseDir, e);
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_STORAGE_FAILED);
        }
        return new StoredImage(storedName, contentType);
    }

    public Resource load(String storedName) {
        Path path = resolve(storedName);
        if (!Files.isReadable(path)) {
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_NOT_FOUND);
        }
        return new FileSystemResource(path);
    }

    // 지우다 실패해도 요청은 그대로 진행한다 (남은 파일은 로그로 확인)
    public void delete(String storedName) {
        if (storedName == null) return;
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException | BusinessException e) {
            log.warn("프로필 사진 삭제 실패: {}", storedName);
        }
    }

    private Path resolve(String storedName) {
        Path path = baseDir.resolve(storedName).normalize();
        if (!path.getParent().equals(baseDir)) {
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_NOT_FOUND);
        }
        return path;
    }
}
