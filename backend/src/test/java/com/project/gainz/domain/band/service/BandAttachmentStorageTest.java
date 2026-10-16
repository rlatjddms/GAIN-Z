package com.project.gainz.domain.band.service;

import com.project.gainz.global.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class BandAttachmentStorageTest {
    @TempDir Path directory;

    @Test void rejectsHtmlEvenWithImageMime() {
        var storage = new BandAttachmentStorage(directory.toString());
        var file = new MockMultipartFile("file", "photo.png", "image/png", "<html>bad</html>".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> storage.store(file)).isInstanceOf(BusinessException.class);
    }

    @Test void storesPdfWithSafeNameAndCanReadIt() throws Exception {
        var storage = new BandAttachmentStorage(directory.toString());
        byte[] bytes = "%PDF-1.7 sample".getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("file", "../../악보.pdf", "text/plain", bytes);
        var saved = storage.store(file);
        assertThat(saved.name()).isEqualTo("악보.pdf");
        assertThat(saved.type()).isEqualTo("application/pdf");
        assertThat(storage.load(saved.key()).getContentAsByteArray()).isEqualTo(bytes);
        storage.delete(saved.key());
        assertThatThrownBy(() -> storage.load(saved.key())).isInstanceOf(BusinessException.class);
    }

    @Test void rejectsTraversalAndEmptyFiles() {
        var storage = new BandAttachmentStorage(directory.toString());
        assertThatThrownBy(() -> storage.load("../../secret")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", new byte[0]))).isInstanceOf(BusinessException.class);
    }

    @Test void rejectsOversizeFiles() {
        var storage = new BandAttachmentStorage(directory.toString());
        var file = new MockMultipartFile("file", "large.mp4", "video/mp4", new byte[(int) BandAttachmentStorage.MAX_BYTES + 1]);
        assertThatThrownBy(() -> storage.store(file)).isInstanceOf(BusinessException.class);
    }

    @Test void recognizesVideoAndAudioHeaders() {
        assertThat(BandAttachmentStorage.detectType("\u0000\u0000\u0000\u0018ftypisom0000".getBytes(StandardCharsets.ISO_8859_1))).isEqualTo("video/mp4");
        assertThat(BandAttachmentStorage.detectType(new byte[] {0x1a, 0x45, (byte) 0xdf, (byte) 0xa3})).isEqualTo("video/webm");
        assertThat(BandAttachmentStorage.detectType("ID3sample".getBytes(StandardCharsets.UTF_8))).isEqualTo("audio/mpeg");
        assertThat(BandAttachmentStorage.detectType("RIFF0000WAVE".getBytes(StandardCharsets.UTF_8))).isEqualTo("audio/wav");
    }
}
