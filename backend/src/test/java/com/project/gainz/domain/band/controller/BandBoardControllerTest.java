package com.project.gainz.domain.band.controller;

import com.project.gainz.domain.band.dto.response.BandBoardResponse.CommentPage;
import com.project.gainz.domain.band.dto.response.BandBoardResponse.PostPage;
import com.project.gainz.domain.band.service.BandBoardService;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.global.error.GlobalExceptionHandler;
import com.project.gainz.global.security.principal.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// HTTP 계약·검증·파일 응답 확인. 인증 필터와 DB를 포함한 통합 테스트는 아니다
class BandBoardControllerTest {
    private BandBoardService service;
    private MockMvc mvc;

    @BeforeEach void setup() {
        service = mock(BandBoardService.class);
        User user = User.builder().name("테스트").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        UserPrincipal principal = UserPrincipal.create(user);
        mvc = MockMvcBuilders.standaloneSetup(new BandBoardController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType() == UserPrincipal.class;
                    }
                    @Override public Object resolveArgument(MethodParameter p, ModelAndViewContainer m,
                                                            NativeWebRequest r, WebDataBinderFactory b) { return principal; }
                }).build();
    }

    @Test void listBindsPaginationAndNoticeFilter() throws Exception {
        when(service.list(1L, 10L, 2, 6, true)).thenReturn(new PostPage(List.of(), 2, 3, 15));
        mvc.perform(get("/api/v1/bands/10/posts?page=2&size=6&notice=true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.number").value(2));
        verify(service).list(1L, 10L, 2, 6, true);
    }

    @Test void noticeDeadlineParsesOffsetAndRejectsMalformedTime() throws Exception {
        mvc.perform(patch("/api/v1/bands/10/posts/20/notice").contentType(MediaType.APPLICATION_JSON)
                .content("{\"notice\":true,\"noticeExpiresAt\":\"2030-01-02T19:30:00+09:00\"}"))
                .andExpect(status().isOk());
        verify(service).notice(1L, 10L, 20L, true, java.time.Instant.parse("2030-01-02T10:30:00Z"));
        mvc.perform(patch("/api/v1/bands/10/posts/20/notice").contentType(MediaType.APPLICATION_JSON)
                .content("{\"notice\":true,\"noticeExpiresAt\":\"잘못된 시간\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void mineFilterUsesAuthenticatedUser() throws Exception {
        when(service.list(1L, 10L, 1, 7, null, true)).thenReturn(new PostPage(List.of(), 1, 2, 9));
        mvc.perform(get("/api/v1/bands/10/posts?mine=true&page=1&size=7&userId=999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(9));
        verify(service).list(1L, 10L, 1, 7, null, true);
    }

    @Test void searchParamsAreBoundAsKeywordAndIsoDates() throws Exception {
        when(service.list(1L, 10L, 0, 12, null, false, "합주", java.time.LocalDate.of(2029, 10, 1), java.time.LocalDate.of(2029, 10, 7)))
                .thenReturn(new PostPage(List.of(), 0, 1, 3));
        mvc.perform(get("/api/v1/bands/10/posts?q=합주&from=2029-10-01&to=2029-10-07"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(3));
        verify(service).list(1L, 10L, 0, 12, null, false, "합주", java.time.LocalDate.of(2029, 10, 1), java.time.LocalDate.of(2029, 10, 7));
        mvc.perform(get("/api/v1/bands/10/posts?from=10월1일")).andExpect(status().isBadRequest());
    }

    @Test void commentsWithPageParamReturnPage() throws Exception {
        when(service.commentPage(1L, 10L, 20L, 2, 20)).thenReturn(new CommentPage(List.of(), 2, 3, 45, 50));
        mvc.perform(get("/api/v1/bands/10/posts/20/comments?page=2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.commentCount").value(50));
        when(service.comments(1L, 10L, 20L)).thenReturn(List.of());
        mvc.perform(get("/api/v1/bands/10/posts/20/comments")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }

    @Test void archiveStreamsAllFilesWithSafeDistinctNames() throws Exception {
        when(service.allFiles(1L, 10L, 20L)).thenReturn(List.of(
                new BandBoardService.AttachmentFile(new ByteArrayResource(new byte[] {1, 2}), "../사진.png", "image/png"),
                new BandBoardService.AttachmentFile(new ByteArrayResource(new byte[] {3}), "../사진.png", "image/png")));
        var response = mvc.perform(get("/api/v1/bands/10/posts/20/attachments.zip")).andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andExpect(header().string("Cache-Control", "private, no-store")).andReturn().getResponse();
        try (var zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(response.getContentAsByteArray()))) {
            org.assertj.core.api.Assertions.assertThat(zip.getNextEntry().getName()).isEqualTo("1_.._사진.png");
            org.assertj.core.api.Assertions.assertThat(zip.readAllBytes()).containsExactly(1, 2);
            org.assertj.core.api.Assertions.assertThat(zip.getNextEntry().getName()).isEqualTo("2_.._사진.png");
            org.assertj.core.api.Assertions.assertThat(zip.readAllBytes()).containsExactly(3);
            org.assertj.core.api.Assertions.assertThat(zip.getNextEntry()).isNull();
        }
    }

    @Test void emptyAndTooLongPostAreRejectedBeforeService() throws Exception {
        mvc.perform(post("/api/v1/bands/10/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"content\":\"본문\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/bands/10/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "가".repeat(101) + "\",\"content\":\"본문\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void oversizedCommentIsRejected() throws Exception {
        mvc.perform(post("/api/v1/bands/10/posts/20/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "가".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void permissionFailureUsesBusinessErrorEnvelope() throws Exception {
        when(service.get(1L, 10L, 20L)).thenThrow(new BusinessException(ErrorCode.NOT_BAND_MEMBER));
        mvc.perform(get("/api/v1/bands/10/posts/20"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.success").value(false));
    }

    @Test void multipartPutBindsOneFile() throws Exception {
        var file = new MockMultipartFile("file", "연습.pdf", "application/pdf", "%PDF-1.7".getBytes());
        mvc.perform(multipart("/api/v1/bands/10/posts/20/attachment").file(file).with(request -> {
            request.setMethod("PUT"); return request;
        })).andExpect(status().isOk());
        verify(service).attach(eq(1L), eq(10L), eq(20L), any());
    }

    @Test void multipartSaveBindsNoticeAndMultipleFiles() throws Exception {
        var post = new MockMultipartFile("post", "", "application/json", "{\"title\":\"합주\",\"content\":\"본문\",\"notice\":true,\"keepAttachmentIds\":[]}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var one = new MockMultipartFile("files", "사진.png", "image/png", new byte[] {1});
        var two = new MockMultipartFile("files", "영상.webm", "video/webm", new byte[] {2});
        mvc.perform(multipart("/api/v1/bands/10/posts").file(post).file(one).file(two)).andExpect(status().isOk());
        verify(service).save(eq(1L), eq(10L), isNull(), argThat(p -> p.notice() && p.title().equals("합주")), argThat(files -> files.size() == 2));
    }

    @Test void attachmentResponseIsPrivateAndUsesStoredContentType() throws Exception {
        when(service.file(1L, 10L, 20L)).thenReturn(new BandBoardService.AttachmentFile(
                new ByteArrayResource(new byte[] {1, 2, 3}), "연습.mp4", "video/mp4"));
        mvc.perform(get("/api/v1/bands/10/posts/20/attachment?download=true"))
                .andExpect(status().isOk()).andExpect(content().contentType("video/mp4"))
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")));
    }
}
