package com.project.gainz.domain.band.controller;

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
