package com.project.gainz.domain.band.service;

import com.project.gainz.domain.band.dto.request.BandBoardRequest;
import com.project.gainz.domain.band.dto.response.BandBoardResponse.*;
import com.project.gainz.domain.band.entity.BandComment;
import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.entity.BandPost;
import com.project.gainz.domain.band.entity.BandPostFile;
import com.project.gainz.domain.band.repository.BandCommentRepository;
import com.project.gainz.domain.band.repository.BandMemberRepository;
import com.project.gainz.domain.band.repository.BandPostRepository;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.user.dto.response.MyInfoResponse;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class BandBoardService {
    public static final int MAX_ATTACHMENTS = 8;
    private final BandMemberRepository members;
    private final BandPostRepository posts;
    private final BandCommentRepository comments;
    private final CharacterRepository characters;
    private final BandAttachmentStorage storage;

    @Transactional(readOnly = true)
    public PostPage list(Long userId, Long bandId, int page, int size) {
        return list(userId, bandId, page, size, null);
    }

    @Transactional(readOnly = true)
    public PostPage list(Long userId, Long bandId, int page, int size, Boolean notice) {
        return list(userId, bandId, page, size, notice, false);
    }

    @Transactional(readOnly = true)
    public PostPage list(Long userId, Long bandId, int page, int size, Boolean notice, boolean mine) {
        requireMember(userId, bandId);
        var pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(20, size)));
        var result = posts.findVisible(bandId, userId, mine, notice, Instant.now(), pageable);
        return new PostPage(result.getContent().stream().map(p -> response(p, userId)).toList(),
                result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Post get(Long userId, Long bandId, Long postId) {
        requireMember(userId, bandId);
        return response(findPost(bandId, postId), userId);
    }

    public Post create(Long userId, Long bandId, BandBoardRequest.Post request) {
        BandMember me = requireMember(userId, bandId);
        BandPost post = posts.save(new BandPost(me.getBand(), me.getUser(), request.title(), request.content()));
        return response(post, userId);
    }

    // 본문·공지·첨부 변경을 한 트랜잭션으로 저장한다. 파일 오류가 나면 글도 롤백한다.
    public Post save(Long userId, Long bandId, Long postId, BandBoardRequest.Save request, List<MultipartFile> uploads) {
        BandMember me = requireMember(userId, bandId);
        BandPost post = postId == null ? new BandPost(me.getBand(), me.getUser(), request.title(), request.content())
                : lockedPost(bandId, postId);
        if (!post.getAuthor().getId().equals(userId)) throw new BusinessException(ErrorCode.BAND_AUTHOR_ONLY);
        boolean changesNotice = request.notice() != null && (request.notice() != post.isNoticeActive(Instant.now())
                || (request.notice() && !Objects.equals(request.noticeExpiresAt(), post.getNoticeExpiresAt())));
        if (!me.isLeader() && (changesNotice || (request.notice() == null && request.noticeExpiresAt() != null))) {
            throw new BusinessException(ErrorCode.BAND_LEADER_ONLY);
        }
        if (request.notice() != null) validateNoticeExpiry(request.notice(), request.noticeExpiresAt());
        List<String> keep = request.keepAttachmentIds() == null ? List.of() : request.keepAttachmentIds();
        List<MultipartFile> added = uploads == null ? List.of() : uploads;
        var existing = new HashSet<String>();
        post.getFiles().forEach(f -> existing.add(f.getKey()));
        if (post.getFileUrl() != null) existing.add("legacy");
        if (new HashSet<>(keep).size() != keep.size() || !existing.containsAll(keep)) {
            throw new BusinessException(ErrorCode.BAND_FILE_SELECTION_INVALID);
        }
        if (keep.size() + added.size() > MAX_ATTACHMENTS) throw new BusinessException(ErrorCode.BAND_FILE_LIMIT);
        var next = new ArrayList<BandPostFile>();
        for (BandPostFile file : post.getFiles()) {
            if (keep.contains(file.getKey())) next.add(file);
            else cleanAfterCompletion(file.getKey(), true);
        }
        for (MultipartFile file : added) {
            var stored = storage.store(file);
            cleanAfterCompletion(stored.key(), false);
            next.add(new BandPostFile(stored.key(), stored.name(), stored.type()));
        }
        if (post.getFileUrl() != null && !keep.contains("legacy")) {
            cleanAfterCompletion(post.getFileUrl(), true);
            post.changeFile(null, null, null);
        }
        post.replaceFiles(next);
        post.update(request.title(), request.content());
        if (request.notice() != null) post.changeNotice(request.notice(), request.noticeExpiresAt());
        if (postId == null) post = posts.save(post);
        return response(post, userId);
    }

    public Post update(Long userId, Long bandId, Long postId, BandBoardRequest.Post request) {
        BandPost post = ownPost(userId, bandId, postId);
        post.update(request.title(), request.content());
        return response(post, userId);
    }

    public void delete(Long userId, Long bandId, Long postId) {
        BandPost post = ownPost(userId, bandId, postId);
        post.delete();
        cleanAfterCompletion(post.getFileUrl(), true);
        post.getFiles().forEach(f -> cleanAfterCompletion(f.getKey(), true));
    }

    public Post notice(Long userId, Long bandId, Long postId, boolean notice) {
        return notice(userId, bandId, postId, notice, null);
    }

    public Post notice(Long userId, Long bandId, Long postId, boolean notice, Instant expiresAt) {
        if (!requireMember(userId, bandId).isLeader()) throw new BusinessException(ErrorCode.BAND_LEADER_ONLY);
        BandPost post = lockedPost(bandId, postId);
        validateNoticeExpiry(notice, expiresAt);
        post.changeNotice(notice, expiresAt);
        return response(post, userId);
    }

    private void validateNoticeExpiry(boolean notice, Instant expiresAt) {
        if (notice && expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.BAND_NOTICE_EXPIRY_INVALID);
        }
    }

    public Post attach(Long userId, Long bandId, Long postId, MultipartFile file) {
        BandPost post = ownPost(userId, bandId, postId);
        if (post.getFileUrl() == null && post.getFiles().size() >= MAX_ATTACHMENTS) throw new BusinessException(ErrorCode.BAND_FILE_LIMIT);
        var stored = storage.store(file);
        cleanAfterCompletion(stored.key(), false);
        cleanAfterCompletion(post.getFileUrl(), true);
        post.changeFile(stored.key(), stored.name(), stored.type());
        return response(post, userId);
    }

    public Post removeAttachment(Long userId, Long bandId, Long postId) {
        BandPost post = ownPost(userId, bandId, postId);
        cleanAfterCompletion(post.getFileUrl(), true);
        post.changeFile(null, null, null);
        return response(post, userId);
    }

    public record AttachmentFile(Resource resource, String name, String type) {}

    @Transactional(readOnly = true)
    public List<AttachmentFile> allFiles(Long userId, Long bandId, Long postId) {
        requireMember(userId, bandId);
        BandPost post = findPost(bandId, postId);
        var result = new ArrayList<AttachmentFile>();
        if (post.getFileUrl() != null) result.add(new AttachmentFile(storage.load(post.getFileUrl()), post.getFileName(), post.getFileType()));
        post.getFiles().forEach(f -> result.add(new AttachmentFile(storage.load(f.getKey()), f.getName(), f.getType())));
        if (result.isEmpty()) throw new BusinessException(ErrorCode.BAND_FILE_NOT_FOUND);
        return result;
    }

    @Transactional(readOnly = true)
    public AttachmentFile file(Long userId, Long bandId, Long postId) {
        requireMember(userId, bandId);
        BandPost post = findPost(bandId, postId);
        if (post.getFileUrl() == null) throw new BusinessException(ErrorCode.BAND_FILE_NOT_FOUND);
        return new AttachmentFile(storage.load(post.getFileUrl()), post.getFileName(), post.getFileType());
    }

    @Transactional(readOnly = true)
    public AttachmentFile file(Long userId, Long bandId, Long postId, String attachmentId) {
        requireMember(userId, bandId);
        BandPost post = findPost(bandId, postId);
        BandPostFile file = post.getFiles().stream().filter(f -> f.getKey().equals(attachmentId)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAND_FILE_NOT_FOUND));
        return new AttachmentFile(storage.load(file.getKey()), file.getName(), file.getType());
    }

    @Transactional(readOnly = true)
    public List<Comment> comments(Long userId, Long bandId, Long postId) {
        requireMember(userId, bandId);
        findPost(bandId, postId);
        return comments.findAllByPostIdOrderByCreatedAtAscIdAsc(postId).stream().map(c -> response(c, userId)).toList();
    }

    public Comment addComment(Long userId, Long bandId, Long postId, BandBoardRequest.Comment request) {
        BandMember me = requireMember(userId, bandId);
        BandPost post = lockedPost(bandId, postId);
        BandComment parent = request.parentId() == null ? null : findComment(postId, request.parentId());
        if (parent != null && parent.getParent() != null) throw new BusinessException(ErrorCode.BAND_REPLY_DEPTH);
        return response(comments.save(new BandComment(post, me.getUser(), request.content(), parent)), userId);
    }

    public Comment updateComment(Long userId, Long bandId, Long postId, Long commentId, BandBoardRequest.Comment request) {
        BandComment comment = ownComment(userId, bandId, postId, commentId);
        comment.update(request.content());
        return response(comment, userId);
    }

    public void deleteComment(Long userId, Long bandId, Long postId, Long commentId) {
        BandComment comment = ownComment(userId, bandId, postId, commentId);
        if (comments.existsByParentId(commentId)) comment.eraseContent();
        else comment.delete();
    }

    private BandMember requireMember(Long userId, Long bandId) {
        return members.findByBandIdAndUserId(bandId, userId)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()) && !Boolean.TRUE.equals(m.getBand().getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_BAND_MEMBER));
    }

    private BandPost findPost(Long bandId, Long postId) {
        return posts.findByIdAndBandId(postId, bandId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.BAND_POST_NOT_FOUND));
    }

    private BandPost lockedPost(Long bandId, Long postId) {
        return posts.findForUpdate(postId, bandId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.BAND_POST_NOT_FOUND));
    }

    private BandPost ownPost(Long userId, Long bandId, Long postId) {
        requireMember(userId, bandId);
        BandPost post = lockedPost(bandId, postId);
        if (!post.getAuthor().getId().equals(userId)) throw new BusinessException(ErrorCode.BAND_AUTHOR_ONLY);
        return post;
    }

    private BandComment findComment(Long postId, Long commentId) {
        return comments.findByIdAndPostId(commentId, postId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.BAND_COMMENT_NOT_FOUND));
    }

    private BandComment ownComment(Long userId, Long bandId, Long postId, Long commentId) {
        requireMember(userId, bandId);
        lockedPost(bandId, postId);
        BandComment comment = findComment(postId, commentId);
        if (!comment.getAuthor().getId().equals(userId)) throw new BusinessException(ErrorCode.BAND_AUTHOR_ONLY);
        if (comment.isContentDeleted()) throw new BusinessException(ErrorCode.BAND_COMMENT_NOT_FOUND);
        return comment;
    }

    private Author author(User user) {
        String nickname = characters.findByUserId(user.getId()).map(c -> c.getNickname()).orElse("멤버");
        return new Author(user.getId(), nickname, MyInfoResponse.from(user).profileImageUrl());
    }

    private Post response(BandPost post, Long userId) {
        String path = "/api/v1/bands/" + post.getBand().getId() + "/posts/" + post.getId();
        var attachments = new ArrayList<Attachment>();
        if (post.getFileUrl() != null) attachments.add(new Attachment("legacy", post.getFileName(), post.getFileType(), path + "/attachment"));
        post.getFiles().forEach(f -> attachments.add(new Attachment(f.getKey(), f.getName(), f.getType(), path + "/attachments/" + f.getKey())));
        return new Post(post.getId(), post.getTitle(), post.getContent(), post.isNoticeActive(Instant.now()),
                author(post.getAuthor()), post.getCreatedAt(), post.getAuthor().getId().equals(userId),
                comments.countByPostIdAndContentDeletedFalse(post.getId()), attachments.isEmpty() ? null : attachments.get(0), attachments, post.getNoticeExpiresAt());
    }

    private Comment response(BandComment comment, Long userId) {
        boolean deleted = comment.isContentDeleted();
        return new Comment(comment.getId(), comment.getParent() == null ? null : comment.getParent().getId(),
                comment.getContent(), deleted ? null : author(comment.getAuthor()), comment.getCreatedAt(),
                !deleted && comment.getAuthor().getId().equals(userId), deleted);
    }

    // DB가 롤백되면 새 파일만, 커밋되면 교체 전 파일만 정리한다
    private void cleanAfterCompletion(String key, boolean onCommit) {
        if (key == null) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if ((status == STATUS_COMMITTED) == onCommit) storage.delete(key);
            }
        });
    }
}
