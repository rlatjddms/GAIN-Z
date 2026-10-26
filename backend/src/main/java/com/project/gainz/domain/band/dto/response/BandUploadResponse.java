package com.project.gainz.domain.band.dto.response;

public final class BandUploadResponse {
    private BandUploadResponse() {}

    // 브라우저는 url에 파일을 PUT 하고(Content-Type은 contentType 그대로), 저장할 때 uploadId를 보낸다.
    // url이 "/api/"로 시작하면 서버 주소라 API 주소를 앞에 붙인다
    public record Started(String uploadId, String url, String contentType) {}

    // 몇 분 동안 쓸 수 있는 파일 주소 (<img>·<video>·<audio> src나 fetch에 바로 쓴다). 위와 같이 "/api/"면 API 주소를 붙인다
    public record Link(String url) {}
}
