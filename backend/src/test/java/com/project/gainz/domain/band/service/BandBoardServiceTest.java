package com.project.gainz.domain.band.service;

import com.project.gainz.domain.band.dto.request.BandBoardRequest;
import com.project.gainz.domain.band.entity.Band;
import com.project.gainz.domain.band.entity.BandComment;
import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.entity.BandPost;
import com.project.gainz.domain.band.enums.Position;
import com.project.gainz.domain.band.repository.BandCommentRepository;
import com.project.gainz.domain.band.repository.BandMemberRepository;
import com.project.gainz.domain.band.repository.BandPostRepository;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BandBoardServiceTest {
    @Mock BandMemberRepository members;
    @Mock BandPostRepository posts;
    @Mock BandCommentRepository comments;
    @Mock CharacterRepository characters;
    @Mock BandAttachmentStorage storage;
    @InjectMocks BandBoardService service;

    private User user(long id) {
        User user = User.builder().name("멤버").email(id + "@example.com").build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private BandMember member(long id, Position position) {
        Band band = Band.builder().name("밴드").leader(user(1)).inviteCode("ABCDEFGH").build();
        ReflectionTestUtils.setField(band, "id", 10L);
        BandMember member = BandMember.builder().band(band).user(user(id)).position(position).build();
        when(members.findByBandIdAndUserId(10L, id)).thenReturn(Optional.of(member));
        return member;
    }

    private BandPost post(BandMember member) {
        BandPost post = new BandPost(member.getBand(), member.getUser(), "제목", "본문");
        ReflectionTestUtils.setField(post, "id", 20L);
        when(posts.findByIdAndBandId(20L, 10L)).thenReturn(Optional.of(post));
        return post;
    }

    private BandComment comment(BandPost post, long id, BandComment parent) {
        BandComment comment = new BandComment(post, post.getAuthor(), "댓글", parent);
        ReflectionTestUtils.setField(comment, "id", id);
        when(comments.findByIdAndPostId(id, 20L)).thenReturn(Optional.of(comment));
        return comment;
    }

    private void fails(Runnable work, ErrorCode code) {
        assertThatThrownBy(work::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }

    @AfterEach void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    @Test void nonMemberCannotReadOrWrite() {
        fails(() -> service.list(9L, 10L, 0, 12), ErrorCode.NOT_BAND_MEMBER);
        fails(() -> service.create(9L, 10L, new BandBoardRequest.Post("제목", "내용")), ErrorCode.NOT_BAND_MEMBER);
        verifyNoInteractions(posts, storage);
    }

    @Test void departedMemberCannotRead() {
        member(1, Position.MEMBER).delete();
        fails(() -> service.get(1L, 10L, 20L), ErrorCode.NOT_BAND_MEMBER);
    }

    @Test void listBoundsPageSizeAndUsesNoticeFirstQuery() {
        member(1, Position.MEMBER);
        when(posts.findByBandIdOrderByIsNoticeDescCreatedAtDescIdDesc(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        service.list(1L, 10L, -5, 100);
        verify(posts).findByBandIdOrderByIsNoticeDescCreatedAtDescIdDesc(eq(10L), argThat((Pageable p) -> p.getPageSize() == 20 && p.getPageNumber() == 0));
    }

    @Test void createUsesMemberIdentityAndTrimsText() {
        member(1, Position.MEMBER);
        when(posts.save(any())).thenAnswer(invocation -> {
            BandPost saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 20L);
            return saved;
        });
        var response = service.create(1L, 10L, new BandBoardRequest.Post(" 제목 ", " 본문 "));
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.author().userId()).isEqualTo(1L);
        assertThat(response.mine()).isTrue();
        assertThat(response.notice()).isFalse();
    }

    @Test void leaderCannotEditSomeoneElsesPost() {
        BandMember leader = member(1, Position.LEADER);
        BandPost post = post(leader);
        ReflectionTestUtils.setField(post, "author", user(2));
        fails(() -> service.update(1L, 10L, 20L, new BandBoardRequest.Post("변경", "내용")), ErrorCode.BAND_AUTHOR_ONLY);
        fails(() -> service.delete(1L, 10L, 20L), ErrorCode.BAND_AUTHOR_ONLY);
    }

    @Test void memberCannotPinNotice() {
        member(1, Position.MEMBER);
        fails(() -> service.notice(1L, 10L, 20L, true), ErrorCode.BAND_LEADER_ONLY);
    }

    @Test void leaderCanPinAndUnpinOtherAuthorsPost() {
        BandPost post = post(member(1, Position.LEADER));
        ReflectionTestUtils.setField(post, "author", user(2));
        assertThat(service.notice(1L, 10L, 20L, true).notice()).isTrue();
        assertThat(service.notice(1L, 10L, 20L, false).notice()).isFalse();
    }

    @Test void postInDifferentBandIsNotVisible() {
        member(1, Position.MEMBER);
        fails(() -> service.get(1L, 10L, 999L), ErrorCode.BAND_POST_NOT_FOUND);
    }

    @Test void commentWithRepliesLeavesAnonymousTombstone() {
        BandPost post = post(member(1, Position.MEMBER));
        BandComment root = comment(post, 30, null);
        when(comments.existsByParentId(30L)).thenReturn(true);
        service.deleteComment(1L, 10L, 20L, 30L);
        assertThat(root.isContentDeleted()).isTrue();
        assertThat(root.getIsDeleted()).isFalse();
        when(comments.findAllByPostIdOrderByCreatedAtAscIdAsc(20L)).thenReturn(List.of(root));
        var response = service.comments(1L, 10L, 20L).get(0);
        assertThat(response.author()).isNull();
        assertThat(response.mine()).isFalse();
        assertThat(response.content()).isEqualTo("삭제된 댓글");
    }

    @Test void leafCommentIsSoftDeleted() {
        BandPost post = post(member(1, Position.MEMBER));
        BandComment leaf = comment(post, 30, null);
        service.deleteComment(1L, 10L, 20L, 30L);
        assertThat(leaf.getIsDeleted()).isTrue();
    }

    @Test void replyToReplyIsRejected() {
        BandPost post = post(member(1, Position.MEMBER));
        BandComment root = new BandComment(post, post.getAuthor(), "원댓글", null);
        comment(post, 31, root);
        fails(() -> service.addComment(1L, 10L, 20L, new BandBoardRequest.Comment("답글", 31L)), ErrorCode.BAND_REPLY_DEPTH);
    }

    @Test void commentFromAnotherPostIsRejected() {
        post(member(1, Position.MEMBER));
        fails(() -> service.addComment(1L, 10L, 20L, new BandBoardRequest.Comment("답글", 999L)), ErrorCode.BAND_COMMENT_NOT_FOUND);
    }

    @Test void cannotEditAnotherAuthorsComment() {
        BandPost post = post(member(1, Position.MEMBER));
        BandComment comment = comment(post, 30, null);
        ReflectionTestUtils.setField(comment, "author", user(2));
        fails(() -> service.updateComment(1L, 10L, 20L, 30L, new BandBoardRequest.Comment("수정", null)), ErrorCode.BAND_AUTHOR_ONLY);
    }

    @Test void attachmentDownloadRequiresMembershipBeforeStorageAccess() {
        fails(() -> service.file(9L, 10L, 20L), ErrorCode.NOT_BAND_MEMBER);
        verifyNoInteractions(storage);
    }

    @Test void replacementCleansOldFileOnlyAfterCommit() {
        BandPost post = post(member(1, Position.MEMBER));
        post.changeFile("old", "old.png", "image/png");
        var file = new MockMultipartFile("file", "new.png", "image/png", new byte[] {1});
        when(storage.store(file)).thenReturn(new BandAttachmentStorage.StoredFile("new", "new.png", "image/png"));
        TransactionSynchronizationManager.initSynchronization();
        service.attach(1L, 10L, 20L, file);
        verify(storage, never()).delete(anyString());
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(storage).delete("old");
        verify(storage, never()).delete("new");
    }

    @Test void attachmentRollbackCleansNewFileOnly() {
        BandPost post = post(member(1, Position.MEMBER));
        post.changeFile("old", "old.png", "image/png");
        var file = new MockMultipartFile("file", "new.png", "image/png", new byte[] {1});
        when(storage.store(file)).thenReturn(new BandAttachmentStorage.StoredFile("new", "new.png", "image/png"));
        TransactionSynchronizationManager.initSynchronization();
        service.attach(1L, 10L, 20L, file);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(storage).delete("new");
        verify(storage, never()).delete("old");
    }
}
