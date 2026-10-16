package com.project.gainz.domain.band.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public final class BandBoardResponse {
    private BandBoardResponse() {}

    public record Author(Long userId, String nickname, String profileImageUrl) {}
    public record Attachment(String name, String type, String url) {}
    public record Post(Long id, String title, String content, boolean notice, Author author,
                       LocalDateTime createdAt, boolean mine, long commentCount, Attachment attachment) {}
    public record PostPage(List<Post> content, int number, int totalPages, long totalElements) {}
    public record Comment(Long id, Long parentId, String content, Author author,
                          LocalDateTime createdAt, boolean mine, boolean deleted) {}
}
