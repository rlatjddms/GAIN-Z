package com.project.gainz.global.error;

import com.project.gainz.global.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 올릴 수 있는 크기 (application.yml spring.servlet.multipart). 종류별로 더 작은 한도는 각 저장소가 따로 알린다
    private final DataSize maxFileSize;
    private final DataSize maxRequestSize;

    // 테스트에서 직접 만들 때는 기본 한도로
    public GlobalExceptionHandler() {
        this(DataSize.ofMegabytes(30), DataSize.ofMegabytes(100));
    }

    @Autowired
    public GlobalExceptionHandler(
            @Value("${spring.servlet.multipart.max-file-size:30MB}") DataSize maxFileSize,
            @Value("${spring.servlet.multipart.max-request-size:100MB}") DataSize maxRequestSize) {
        this.maxFileSize = maxFileSize;
        this.maxRequestSize = maxRequestSize;
    }

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode, e.getData()));
    }

    // 올린 파일이 multipart 크기 제한(application.yml)을 넘음. 악보·첨부·잼 음원·프로필 사진 어디서든 같은 코드와 실제 한도로 알린다 (#280)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    protected ResponseEntity<ApiResponse<?>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        ErrorCode code = ErrorCode.FILE_TOO_LARGE;
        String message = String.format("파일이 너무 커요. 한 파일에 %dMB, 한 번에 %dMB까지 올릴 수 있어요.",
                maxFileSize.toMegabytes(), maxRequestSize.toMegabytes());
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiResponse.fail(code.getStatus(), code.getCode(), message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.getStatus(), ErrorCode.INVALID_INPUT.getCode(), errorMessage));
    }
}
