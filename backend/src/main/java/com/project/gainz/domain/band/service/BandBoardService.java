package com.project.gainz.domain.band.service;

import com.project.gainz.domain.band.dto.request.BandBoardRequest;
import com.project.gainz.domain.band.dto.response.BandBoardResponse.*;
import com.project.gainz.domain.band.entity.BandComment;
import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.entity.BandPost;
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

@Service
@RequiredArgsConstructor
@Transactional
public class BandBoardService {
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
        requireMember(userId, bandId);
        var pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(20, size)));
        var result = notice == null ? posts.findByBandIdOrderByIsNoticeDescCreatedAtDescIdDesc(bandId, pageable)
                : posts.findByBandIdAndIsNoticeOrderByCreatedAtDescIdDesc(bandId, notice, pageable);
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

    public Post update(Long userId, Long bandId, Long postId, BandBoardRequest.Post request) {
        BandPost post = ownPost(userId, bandId, postId);
        post.update(request.title(), request.content());
        return response(post, userId);
    }

    public void delete(Long userId, Long bandId, Long postId) {
        BandPost post = ownPost(userId, bandId, postId);
        post.delete();
        cleanAfterCompletion(post.getFileUrl(), true);
    }

    public Post notice(Long userId, Long bandId, Long postId, boolean notice) {
        if (!requireMember(userId, bandId).isLeader()) throw new BusinessException(ErrorCode.BAND_LEADER_ONLY);
        BandPost post = findPost(bandId, postId);
        post.changeNotice(notice);
        return response(post, userId);
    }

    public Post attach(Long userId, Long bandId, Long postId, MultipartFile file) {
        BandPost post = ownPost(userId, bandId, postId);
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
    public AttachmentFile file(Long userId, Long bandId, Long postId) {
        requireMember(userId, bandId);
        BandPost post = findPost(bandId, postId);
        if (post.getFileUrl() == null) throw new BusinessException(ErrorCode.BAND_FILE_NOT_FOUND);
        return new AttachmentFile(storage.load(post.getFileUrl()), post.getFileName(), post.getFileType());
    }

    @Transactional(readOnly = true)
    public List<Comment> comments(Long userId, Long bandId, Long postId) {
        requireMember(userId, bandId);
        findPost(bandId, postId);
        return comments.findAllByPostIdOrderByCreatedAtAscIdAsc(postId).stream().map(c -> response(c, userId)).toList();
    }

    public Comment addComment(Long userId, Long bandId, Long postId, BandBoardRequest.Comment request) {
        BandMember me = requireMember(userId, bandId);
        BandPost post = findPost(bandId, postId);
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

    private BandPost ownPost(Long userId, Long bandId, Long postId) {
        requireMember(userId, bandId);
        BandPost post = findPost(bandId, postId);
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
        findPost(bandId, postId);
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
        Attachment attachment = post.getFileUrl() == null ? null : new Attachment(post.getFileName(), post.getFileType(),
                "/api/v1/bands/" + post.getBand().getId() + "/posts/" + post.getId() + "/attachment");
        return new Post(post.getId(), post.getTitle(), post.getContent(), Boolean.TRUE.equals(post.getIsNotice()),
                author(post.getAuthor()), post.getCreatedAt(), post.getAuthor().getId().equals(userId),
                comments.countByPostIdAndContentDeletedFalse(post.getId()), attachment);
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
