package com.project.gainz.global.error;

import com.project.gainz.global.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

// 크기 초과 (#280): 악보 코드(S008)가 아니라 공통 코드, 문구는 실제 설정 한도로
class GlobalExceptionHandlerTest {

    @Test void tooLargeUploadUsesCommonCodeAndConfiguredLimits() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(DataSize.ofMegabytes(50), DataSize.ofMegabytes(200));

        ResponseEntity<ApiResponse<?>> response = handler.handleMaxUploadSizeExceededException(new MaxUploadSizeExceededException(-1));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().code()).isEqualTo("G002");
        assertThat(response.getBody().message()).isEqualTo("파일이 너무 커요. 한 파일에 50MB, 한 번에 200MB까지 올릴 수 있어요.");
    }

    @Test void defaultLimitsMatchApplicationYml() {
        ResponseEntity<ApiResponse<?>> response = new GlobalExceptionHandler()
                .handleMaxUploadSizeExceededException(new MaxUploadSizeExceededException(-1));

        assertThat(response.getBody().message()).contains("30MB").contains("100MB");
    }
}
