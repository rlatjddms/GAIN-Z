package com.project.gainz.domain.user.service;

import com.project.gainz.domain.sheet.service.SheetFileStorage;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.global.storage.FileStore;
import com.project.gainz.global.storage.StorageQuota;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

// 프로필 사진 파일 저장 (FileStore의 profiles/, #178). 이미지(PNG·JPG·WEBP)만, 5MB까지
@Slf4j
@Component
public class ProfileImageStorage {

    // 휴대폰 원본 사진 (#262에서 5MB → 10MB. 화면은 올리기 전에 줄여서 보낸다)
    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    public record StoredImage(String storedName, String contentType) {
    }

    private static final String FOLDER = "profiles/";

    private final FileStore fileStore;
    private final StorageQuota quota;

    public ProfileImageStorage(FileStore fileStore, StorageQuota quota) {
        this.fileStore = fileStore;
        this.quota = quota;
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
        quota.check(file.getSize());
        try (InputStream in = file.getInputStream()) {
            fileStore.put(FOLDER + storedName, in, file.getSize(), contentType);
            quota.added(file.getSize());
        } catch (IOException e) {
            log.error("프로필 사진 저장 실패: {}", storedName, e);
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_STORAGE_FAILED);
        }
        return new StoredImage(storedName, contentType);
    }

    public Resource load(String storedName) {
        Resource resource;
        try {
            resource = fileStore.get(key(storedName));
        } catch (IOException e) {
            log.error("프로필 사진 읽기 실패: {}", storedName, e);
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_STORAGE_FAILED);
        }
        if (resource == null) {
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_NOT_FOUND);
        }
        return resource;
    }

    // 지우다 실패해도 요청은 그대로 진행한다 (남은 파일은 로그로 확인)
    public void delete(String storedName) {
        if (storedName == null) return;
        try {
            fileStore.delete(key(storedName));
        } catch (IOException | BusinessException e) {
            log.warn("프로필 사진 삭제 실패: {}", storedName);
        }
    }

    // 저장할 때 붙인 이름(UUID.확장자)만 받는다. 다른 폴더를 가리키는 이름은 막는다
    private static String key(String storedName) {
        if (storedName == null || !storedName.matches("[A-Za-z0-9-]+\\.[a-z]+")) {
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_NOT_FOUND);
        }
        return FOLDER + storedName;
    }
}
