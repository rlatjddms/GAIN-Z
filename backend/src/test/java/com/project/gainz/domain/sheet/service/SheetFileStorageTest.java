package com.project.gainz.domain.sheet.service;

import com.project.gainz.domain.sheet.service.SheetFileStorage.StoredFile;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.global.storage.LocalFileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SheetFileStorageTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    private static final byte[] PDF = "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);

    @TempDir Path dir;

    private SheetFileStorage storage() {
        return new SheetFileStorage(new LocalFileStore(dir.toString()));
    }

    @Test
    void 파일_앞부분으로_형식을_알아낸다() {
        assertThat(SheetFileStorage.detectType(PNG)).isEqualTo("image/png");
        assertThat(SheetFileStorage.detectType(JPG)).isEqualTo("image/jpeg");
        assertThat(SheetFileStorage.detectType(WEBP)).isEqualTo("image/webp");
        assertThat(SheetFileStorage.detectType(PDF)).isEqualTo("application/pdf");
        assertThat(SheetFileStorage.detectType("<html>".getBytes(StandardCharsets.US_ASCII))).isNull();
        assertThat(SheetFileStorage.detectType(new byte[0])).isNull();
    }

    @Test
    void 저장하면_새_이름으로_저장되고_다시_읽고_지울_수_있다() throws Exception {
        SheetFileStorage storage = storage();

        StoredFile saved = storage.store(new MockMultipartFile("files", "C:\\악보\\밤편지 1.png", "application/octet-stream", PNG));

        assertThat(saved.contentType()).isEqualTo("image/png");
        assertThat(saved.storedName()).endsWith(".png").doesNotContain("밤편지");
        assertThat(saved.fileName()).isEqualTo("밤편지 1.png");
        Path file = dir.resolve("sheets").resolve(saved.storedName());
        assertThat(Files.readAllBytes(file)).isEqualTo(PNG);
        assertThat(storage.load(saved.storedName()).contentLength()).isEqualTo(PNG.length);

        storage.delete(saved.storedName());
        assertThat(file).doesNotExist();
    }

    @Test
    void 형식을_속인_파일은_받지_않는다() {
        MockMultipartFile fake = new MockMultipartFile("files", "악보.png", "image/png", "<script>".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> storage().store(fake))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SHEET_FILE);
    }

    @Test
    void 빈_파일과_10MB가_넘는_파일은_받지_않는다() {
        assertThatThrownBy(() -> storage().store(new MockMultipartFile("files", "a.png", "image/png", new byte[0])))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_FILE_REQUIRED);

        byte[] big = new byte[(int) SheetFileStorage.MAX_FILE_BYTES + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        assertThatThrownBy(() -> storage().store(new MockMultipartFile("files", "big.png", "image/png", big)))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_FILE_TOO_LARGE);
    }

    @Test
    void 저장소_밖_경로로는_읽을_수_없다() {
        assertThatThrownBy(() -> storage().load("../../secret.yml"))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_PAGE_NOT_FOUND);
    }
}
