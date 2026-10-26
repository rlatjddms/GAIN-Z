package com.project.gainz.domain.sheet.service;

import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.global.storage.FileStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

// 악보 파일을 FileStore의 sheets/ 아래에 저장한다 (로컬 디스크 또는 Supabase Storage, #178).
// 올린 사람이 붙인 형식은 믿지 않고 파일 앞부분으로 PNG·JPG·WEBP·PDF인지 확인한다. 이름은 UUID로 새로 붙인다
@Slf4j
@Component
public class SheetFileStorage {

    // 스캔한 PDF 여러 쪽 (#262에서 10MB → 30MB)
    public static final long MAX_FILE_BYTES = 30L * 1024 * 1024;

    public record StoredFile(String storedName, String contentType, String fileName, long size) {
    }

    private static final String FOLDER = "sheets/";

    private final FileStore fileStore;

    public SheetFileStorage(FileStore fileStore) {
        this.fileStore = fileStore;
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
        try (InputStream in = file.getInputStream()) {
            fileStore.put(FOLDER + storedName, in, file.getSize(), contentType);
        } catch (IOException e) {
            log.error("악보 파일 저장 실패: {}", storedName, e);
            throw new BusinessException(ErrorCode.SHEET_FILE_STORAGE_FAILED);
        }
        return new StoredFile(storedName, contentType, displayName(file.getOriginalFilename()), file.getSize());
    }

    // 저장된 파일을 새 이름으로 하나 더 저장한다 (내 악보로 복사, #191) → 새 이름
    public String copy(String storedName) {
        String copyName = UUID.randomUUID() + storedName.substring(storedName.lastIndexOf('.'));
        try {
            Resource source = fileStore.get(key(storedName));
            if (source == null) {
                throw new BusinessException(ErrorCode.SHEET_PAGE_NOT_FOUND);
            }
            try (InputStream in = source.getInputStream()) {
                fileStore.put(FOLDER + copyName, in, source.contentLength(), contentTypeOf(copyName));
            }
        } catch (IOException e) {
            log.error("악보 파일 복사 실패: {}", storedName, e);
            throw new BusinessException(ErrorCode.SHEET_FILE_STORAGE_FAILED);
        }
        return copyName;
    }

    public Resource load(String storedName) {
        Resource resource;
        try {
            resource = fileStore.get(key(storedName));
        } catch (IOException e) {
            log.error("악보 파일 읽기 실패: {}", storedName, e);
            throw new BusinessException(ErrorCode.SHEET_FILE_STORAGE_FAILED);
        }
        if (resource == null) {
            throw new BusinessException(ErrorCode.SHEET_PAGE_NOT_FOUND);
        }
        return resource;
    }

    // 지우다 실패해도 요청은 그대로 진행한다 (남은 파일은 로그로 확인)
    public void delete(String storedName) {
        try {
            fileStore.delete(key(storedName));
        } catch (IOException | BusinessException e) {
            log.warn("악보 파일 삭제 실패: {}", storedName);
        }
    }

    // 저장할 때 붙인 이름(UUID.확장자)만 받는다. 다른 폴더를 가리키는 이름은 막는다
    private static String key(String storedName) {
        if (storedName == null || !storedName.matches("[A-Za-z0-9-]+\\.[a-z]+")) {
            throw new BusinessException(ErrorCode.SHEET_PAGE_NOT_FOUND);
        }
        return FOLDER + storedName;
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

    private static String contentTypeOf(String storedName) {
        String lower = storedName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/pdf";
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
