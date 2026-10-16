package com.project.gainz.domain.user.service;

import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileImageStorageTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7', 0, 0, 0, 0};

    @TempDir
    Path tempDir;

    @Test
    void PNG_사진을_저장하고_다시_읽는다() throws Exception {
        ProfileImageStorage storage = new ProfileImageStorage(tempDir.toString());

        ProfileImageStorage.StoredImage stored = storage.store(new MockMultipartFile("file", "me.txt", "text/plain", PNG));

        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.storedName()).endsWith(".png");
        assertThat(Files.exists(tempDir.resolve("profiles").resolve(stored.storedName()))).isTrue();
        assertThat(storage.load(stored.storedName()).contentLength()).isEqualTo(PNG.length);
    }

    @Test
    void PDF는_프로필_사진으로_올릴_수_없다() {
        ProfileImageStorage storage = new ProfileImageStorage(tempDir.toString());

        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "me.png", "image/png", PDF)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PROFILE_IMAGE);
    }

    @Test
    void 빈_파일은_올릴_수_없다() {
        ProfileImageStorage storage = new ProfileImageStorage(tempDir.toString());

        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "me.png", "image/png", new byte[0])))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROFILE_IMAGE_REQUIRED);
    }

    @Test
    void 폴더_밖_경로는_읽지_않는다() {
        ProfileImageStorage storage = new ProfileImageStorage(tempDir.toString());

        assertThatThrownBy(() -> storage.load("../secret.png"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROFILE_IMAGE_NOT_FOUND);
    }
}
