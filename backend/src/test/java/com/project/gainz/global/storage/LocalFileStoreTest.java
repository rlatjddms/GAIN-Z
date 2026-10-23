package com.project.gainz.global.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStoreTest {

    @TempDir Path dir;

    @Test
    void 폴더를_만들어_저장하고_읽고_지운다() throws Exception {
        LocalFileStore store = new LocalFileStore(dir.toString());
        byte[] data = {1, 2, 3};

        store.put("sheets/a.png", new ByteArrayInputStream(data), data.length, "image/png");

        assertThat(Files.readAllBytes(dir.resolve("sheets/a.png"))).isEqualTo(data);
        assertThat(store.get("sheets/a.png").contentLength()).isEqualTo(3);

        store.delete("sheets/a.png");
        assertThat(store.get("sheets/a.png")).isNull();
        store.delete("sheets/a.png");
    }

    @Test
    void 업로드_폴더_밖은_가리킬_수_없다() {
        LocalFileStore store = new LocalFileStore(dir.resolve("uploads").toString());

        assertThatThrownBy(() -> store.get("../secret.txt")).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> store.put("sheets/../../x", new ByteArrayInputStream(new byte[0]), 0, "image/png"))
                .isInstanceOf(IOException.class);
    }
}
