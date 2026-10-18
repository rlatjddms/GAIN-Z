package com.project.gainz.domain.band.service;

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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
public class BandAttachmentStorage {
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    // 레이어 잼 녹음 트랙 (Opus·AAC 약 25분)
    public static final long AUDIO_MAX_BYTES = 30L * 1024 * 1024;
    private final Path baseDir;

    public record StoredFile(String key, String name, String type) {}

    public BandAttachmentStorage(@Value("${gainz.upload-dir:${user.home}/.gainz/uploads}") String dir) {
        baseDir = Path.of(dir, "band").toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file) {
        return save(file, MAX_BYTES, BandAttachmentStorage::detectType, ErrorCode.BAND_FILE_INVALID);
    }

    // 녹음 트랙은 오디오 형식만 받는다
    public StoredFile storeAudio(MultipartFile file) {
        return save(file, AUDIO_MAX_BYTES, BandAttachmentStorage::detectAudioType, ErrorCode.JAM_TRACK_FILE_INVALID);
    }

    private StoredFile save(MultipartFile file, long maxBytes, Function<byte[], String> detector, ErrorCode invalid) {
        if (file == null || file.isEmpty() || file.getSize() > maxBytes) {
            throw new BusinessException(invalid);
        }
        try (var input = file.getInputStream()) {
            byte[] head = input.readNBytes(16);
            String type = detector.apply(head);
            if (type == null) throw new BusinessException(invalid);
            String key = UUID.randomUUID().toString();
            Files.createDirectories(baseDir);
            Path target = resolve(key);
            try (var stream = file.getInputStream()) {
                Files.copy(stream, target);
            } catch (IOException e) {
                Files.deleteIfExists(target);
                throw e;
            }
            String name = file.getOriginalFilename() == null ? "첨부파일" : file.getOriginalFilename();
            name = name.replace('\\', '/');
            name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
            if (name.isBlank()) name = "첨부파일";
            return new StoredFile(key, name.substring(0, Math.min(name.length(), 200)), type);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BAND_FILE_STORAGE_FAILED);
        }
    }

    // 브라우저에 실행 가능한 HTML/SVG를 내보내지 않도록 실제 파일 헤더로 형식을 제한한다
    static String detectType(byte[] head) {
        String type = SheetFileStorage.detectType(head);
        if (type != null) return type;
        String text = new String(head, StandardCharsets.ISO_8859_1);
        if (head.length >= 12 && text.startsWith("RIFF") && text.substring(8, 12).equals("WAVE")) return "audio/wav";
        if (head.length >= 12 && text.substring(4, 8).equals("ftyp")) return "video/mp4";
        if (head.length >= 4 && head[0] == 0x1a && head[1] == 0x45 && head[2] == (byte) 0xdf && head[3] == (byte) 0xa3) return "video/webm";
        if (text.startsWith("ID3") || (head.length > 1 && head[0] == (byte) 0xff && (head[1] & 0xe0) == 0xe0)) return "audio/mpeg";
        return null;
    }

    // 브라우저 녹음(webm·ogg·mp4)과 일반 음원(mp3·wav). WebM·MP4는 영상과 머리가 같아서 오디오로 표시한다
    static String detectAudioType(byte[] head) {
        String type = detectType(head);
        if (type == null) {
            String text = new String(head, StandardCharsets.ISO_8859_1);
            return text.startsWith("OggS") ? "audio/ogg" : null;
        }
        return switch (type) {
            case "audio/wav", "audio/mpeg" -> type;
            case "video/webm" -> "audio/webm";
            case "video/mp4" -> "audio/mp4";
            default -> null;
        };
    }

    public Resource load(String key) {
        Path path = resolve(key);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) throw new BusinessException(ErrorCode.BAND_FILE_NOT_FOUND);
        return new FileSystemResource(path);
    }

    public void delete(String key) {
        if (key == null) return;
        try { Files.deleteIfExists(resolve(key)); }
        catch (IOException | BusinessException e) { log.warn("밴드 첨부 파일 정리 실패: {}", key); }
    }

    private Path resolve(String key) {
        if (key == null || !key.matches("[a-f0-9-]{36}")) throw new BusinessException(ErrorCode.BAND_FILE_NOT_FOUND);
        Path path = baseDir.resolve(key).normalize();
        if (!path.getParent().equals(baseDir)) throw new BusinessException(ErrorCode.BAND_FILE_NOT_FOUND);
        return path;
    }
}
