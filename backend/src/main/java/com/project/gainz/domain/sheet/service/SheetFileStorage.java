package com.project.gainz.domain.sheet.service;

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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

// 악보 파일을 서버 디스크에 저장한다 (기본: 사용자 홈/.gainz/uploads/sheets, gainz.upload-dir로 바꿀 수 있음).
// 올린 사람이 붙인 형식은 믿지 않고 파일 앞부분으로 PNG·JPG·WEBP·PDF인지 확인한다. 이름은 UUID로 새로 붙인다
@Slf4j
@Component
public class SheetFileStorage {

    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    public record StoredFile(String storedName, String contentType, String fileName, long size) {
    }

    private final Path baseDir;

    public SheetFileStorage(@Value("${gainz.upload-dir:${user.home}/.gainz/uploads}") String uploadDir) {
        this.baseDir = Path.of(uploadDir, "sheets").toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.SHEET_FILE_REQUIRED);
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException(ErrorCode.SHEET_FILE_TOO_LARGE);
        }
        String contentType;
        try (InputStream in = file.getInputStream()) {
            contentType = detectType(in.readNBytes(12));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SHEET_FILE_STORAGE_FAILED);
        }
        if (contentType == null) {
            throw new BusinessException(ErrorCode.INVALID_SHEET_FILE);
        }
        String storedName = UUID.randomUUID() + extensionOf(contentType);
        try {
            Files.createDirectories(baseDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, baseDir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("악보 파일 저장 실패: {}", baseDir, e);
            throw new BusinessException(ErrorCode.SHEET_FILE_STORAGE_FAILED);
        }
        return new StoredFile(storedName, contentType, displayName(file.getOriginalFilename()), file.getSize());
    }

    public Resource load(String storedName) {
        Path path = resolve(storedName);
        if (!Files.isReadable(path)) {
            throw new BusinessException(ErrorCode.SHEET_PAGE_NOT_FOUND);
        }
        return new FileSystemResource(path);
    }

    // 지우다 실패해도 요청은 그대로 진행한다 (남은 파일은 로그로 확인)
    public void delete(String storedName) {
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException | BusinessException e) {
            log.warn("악보 파일 삭제 실패: {}", storedName);
        }
    }

    private Path resolve(String storedName) {
        Path path = baseDir.resolve(storedName).normalize();
        if (!path.getParent().equals(baseDir)) {
            throw new BusinessException(ErrorCode.SHEET_PAGE_NOT_FOUND);
        }
        return path;
    }

    // 파일 앞부분(매직 넘버)으로 형식 판단. 모르는 형식이면 null (프로필 사진 저장에서도 쓴다)
    public static String detectType(byte[] head) {
        if (startsWith(head, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) return "image/png";
        if (startsWith(head, 0xFF, 0xD8, 0xFF)) return "image/jpeg";
        if (head.length >= 12 && startsWith(head, 'R', 'I', 'F', 'F')
                && new String(Arrays.copyOfRange(head, 8, 12), StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
        if (startsWith(head, '%', 'P', 'D', 'F', '-')) return "application/pdf";
        return null;
    }

    private static boolean startsWith(byte[] head, int... prefix) {
        if (head.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if ((head[i] & 0xFF) != prefix[i]) return false;
        }
        return true;
    }

    private static String extensionOf(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> ".pdf";
        };
    }

    // 경로 부분을 떼고 255자로 자른 표시용 이름
    private static String displayName(String original) {
        if (original == null || original.isBlank()) return "악보";
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        if (name.isEmpty()) return "악보";
        return name.length() > 255 ? name.substring(0, 255) : name;
    }
}
