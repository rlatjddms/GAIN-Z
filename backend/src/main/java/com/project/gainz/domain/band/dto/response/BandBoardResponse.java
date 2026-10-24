package com.project.gainz.domain.band.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.time.Instant;

public final class BandBoardResponse {
    private BandBoardResponse() {}

    public record Author(Long userId, String nickname, String profileImageUrl) {}
    public record Attachment(String id, String name, String type, String url) {}
    public record Post(Long id, String title, String content, boolean notice, Author author,
                       LocalDateTime createdAt, boolean mine, long commentCount, Attachment attachment,
                       List<Attachment> attachments, Instant noticeExpiresAt) {}
    public record PostPage(List<Post> content, int number, int totalPages, long totalElements) {}
    // 댓글 한 쪽 (#188): 최근 원댓글부터 쪽을 나누고, 쪽 안은 오래된 순(원댓글 뒤에 그 답글).
    // totalElements는 원댓글 수, commentCount는 지워지지 않은 댓글(답글 포함) 수
    public record CommentPage(List<Comment> content, int number, int totalPages, long totalElements, long commentCount) {}
    public record Comment(Long id, Long parentId, String content, Author author,
                          LocalDateTime createdAt, boolean mine, boolean deleted) {}
}
