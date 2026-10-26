package com.project.gainz.global.storage;

import com.project.gainz.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 로컬 저장소에서 서버 주소로 올리고 받기 (#262)
class FileLinkControllerTest {

    @TempDir Path directory;
    private LocalFileStore store;
    private FileLinks links;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        store = new LocalFileStore(directory.toString());
        links = new FileLinks(store, "secret");
        mvc = MockMvcBuilders.standaloneSetup(new FileLinkController(links, store))
                .setControllerAdvice(new GlobalExceptionHandler(DataSize.ofMegabytes(50), DataSize.ofMegabytes(100)))
                .build();
    }

    @Test
    void 올리기_주소로_PUT_하고_받기_주소로_받는다() throws Exception {
        byte[] bytes = "%PDF-1.7 악보".getBytes(StandardCharsets.UTF_8);
        mvc.perform(put(links.uploadUrl("tmp/a", "application/pdf", 100)).content(bytes).contentType("application/pdf"))
                .andExpect(status().isOk());
        assertThat(store.get("tmp/a").getContentAsByteArray()).isEqualTo(bytes);

        mvc.perform(get(links.downloadUrl("tmp/a", "악보.pdf", "application/pdf", true)))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes))
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void 한도보다_크거나_받기_주소로_올리면_거절한다() throws Exception {
        mvc.perform(put(links.uploadUrl("tmp/a", "video/mp4", 3)).content(new byte[4]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("G002"));
        mvc.perform(put(links.downloadUrl("tmp/a", "a", "video/mp4", false)).content(new byte[1]))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("G003"));
        assertThat(store.get("tmp/a")).isNull();
    }
}
