package com.project.gainz.domain.band.service;

import com.project.gainz.domain.band.dto.response.BandUploadResponse;
import com.project.gainz.domain.band.entity.BandUpload;
import com.project.gainz.domain.band.enums.BandUploadKind;
import com.project.gainz.domain.band.repository.BandUploadRepository;
import com.project.gainz.domain.sheet.service.SheetFileStorage;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.global.storage.FileLinks;
import com.project.gainz.global.storage.FileStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;

// 밴드 게시판·셋리스트 첨부와 잼 녹음 트랙을 FileStore의 band/ 아래에 저장한다 (#178).
// 큰 파일은 브라우저가 서버를 거치지 않고 tmp/에 바로 올린 뒤(start) 저장할 때 가져간다(claim) (#262)
@Slf4j
@Component
public class BandAttachmentStorage {
    // 첨부: 휴대폰 영상 몇 분 (#262에서 10MB → 200MB)
    public static final long MAX_BYTES = 200L * 1024 * 1024;
    // 레이어 잼 녹음 트랙: WAV 약 5분·Opus는 훨씬 길게 (#262에서 30MB → 50MB)
    public static final long AUDIO_MAX_BYTES = 50L * 1024 * 1024;
    private static final String FOLDER = "band/";
    private static final String TMP_FOLDER = "tmp/";
    private final FileStore fileStore;
    private final FileLinks links;
    private final BandUploadRepository uploads;

    public record StoredFile(String key, String name, String type) {}

    public BandAttachmentStorage(FileStore fileStore, FileLinks links, BandUploadRepository uploads) {
        this.fileStore = fileStore;
        this.links = links;
        this.uploads = uploads;
    }

    // 바로 올릴 주소를 준다. 밴드 멤버인지는 부르는 쪽(BandUploadService)이 확인한다
    public BandUploadResponse.Started start(Long userId, Long bandId, BandUploadKind kind, String name, long size, String contentType) {
        if (size <= 0 || size > maxBytes(kind)) throw new BusinessException(invalid(kind));
        String type = contentType != null && contentType.matches("[\\w.+-]+/[\\w.+-]+") ? contentType : "application/octet-stream";
        String id = UUID.randomUUID().toString();
        uploads.save(new BandUpload(id, userId, bandId, kind, cleanName(name)));
        return new BandUploadResponse.Started(id, links.uploadUrl(TMP_FOLDER + id, type, maxBytes(kind)), type);
    }

    // 바로 올린 파일을 확인하고(크기·실제 형식) band/로 옮긴다. 본인이 이 밴드에 올린 것만 가져갈 수 있다
    public StoredFile claim(Long userId, Long bandId, String uploadId, BandUploadKind kind) {
        BandUpload upload = uploadId == null ? null : uploads.findById(uploadId)
                .filter(u -> u.getUserId().equals(userId) && u.getBandId().equals(bandId) && u.getKind() == kind)
                .orElse(null);
        if (upload == null) throw new BusinessException(ErrorCode.BAND_UPLOAD_NOT_FOUND);
        String tmp = TMP_FOLDER + upload.getId();
        try {
            long size = fileStore.size(tmp);
            if (size <= 0) throw new BusinessException(ErrorCode.BAND_UPLOAD_NOT_FOUND);
            byte[] head = fileStore.head(tmp, 16);
            String type = size > maxBytes(kind) || head == null ? null : detector(kind).apply(head);
            if (type == null) {
                fileStore.delete(tmp);
                uploads.deleteById(upload.getId());
                throw new BusinessException(invalid(kind));
            }
            String key = UUID.randomUUID().toString();
            fileStore.move(tmp, path(key), type);
            uploads.deleteById(upload.getId());
            return new StoredFile(key, upload.getName(), type);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BAND_FILE_STORAGE_FAILED);
        }
    }

    // 몇 분 동안 쓸 수 있는 받기 주소. 권한 검사는 부르는 쪽(각 서비스의 file)이 한다
    public String link(String key, String name, String type, boolean download) {
        return links.downloadUrl(path(key), name, type, download);
    }

    private static long maxBytes(BandUploadKind kind) {
        return kind == BandUploadKind.JAM_TRACK ? AUDIO_MAX_BYTES : MAX_BYTES;
    }

    private static ErrorCode invalid(BandUploadKind kind) {
        return kind == BandUploadKind.JAM_TRACK ? ErrorCode.JAM_TRACK_FILE_INVALID : ErrorCode.BAND_FILE_INVALID;
    }

    private static Function<byte[], String> detector(BandUploadKind kind) {
        return kind == BandUploadKind.JAM_TRACK ? BandAttachmentStorage::detectAudioType : BandAttachmentStorage::detectType;
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
            try (var stream = file.getInputStream()) {
                fileStore.put(path(key), stream, file.getSize(), type);
            }
            return new StoredFile(key, cleanName(file.getOriginalFilename()), type);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BAND_FILE_STORAGE_FAILED);
        }
    }

    // 경로·제어 문자를 빼고 200자까지
    static String cleanName(String original) {
        String name = original == null ? "첨부파일" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        if (name.isBlank()) name = "첨부파일";
        return name.substring(0, Math.min(name.length(), 200));
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
        Resource resource;
        try { resource = fileStore.get(path(key)); }
        catch (IOException e) { throw new BusinessException(ErrorCode.BAND_FILE_STORAGE_FAILED); }
        if (resource == null) throw new BusinessException(ErrorCode.BAND_FILE_NOT_FOUND);
        return resource;
    }

    public void delete(String key) {
        if (key == null) return;
        try { fileStore.delete(path(key)); }
        catch (IOException | BusinessException e) { log.warn("밴드 첨부 파일 정리 실패: {}", key); }
    }

    private static String path(String key) {
        if (key == null || !key.matches("[a-f0-9-]{36}")) throw new BusinessException(ErrorCode.BAND_FILE_NOT_FOUND);
        return FOLDER + key;
    }
}
