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
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.notification.enums.NotificationType;
import com.project.gainz.domain.notification.service.NotificationService;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.common.enums.InstrumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// 커뮤니티 알림: 댓글·답글·멘션은 한 사람에게 하나만, 공지는 새로 켜질 때만
@ExtendWith(MockitoExtension.class)
class BandBoardNotificationTest {

    @Mock BandMemberRepository members;
    @Mock BandPostRepository posts;
    @Mock BandCommentRepository comments;
    @Mock CharacterRepository characters;
    @Mock BandAttachmentStorage storage;
    @Mock NotificationService notificationService;
    @InjectMocks BandBoardService service;

    Band band;
    User guitar;   // 1: 글쓴이, 리더 "기타왕"
    User drum;     // 2: "드럼짱"
    User vocal;    // 3: "보컬"
    BandPost post;

    @BeforeEach void setUp() {
        guitar = user(1, "김기타");
        drum = user(2, "박드럼");
        vocal = user(3, "이보컬");
        band = Band.builder().name("락앤롤").leader(guitar).inviteCode("ABCDEFGH").build();
        ReflectionTestUtils.setField(band, "id", 10L);
        List<BandMember> all = List.of(member(guitar, Position.LEADER), member(drum, Position.MEMBER), member(vocal, Position.MEMBER));
        all.forEach(m -> lenient().when(members.findByBandIdAndUserId(10L, m.getUser().getId())).thenReturn(Optional.of(m)));
        lenient().when(members.findAllByBandIdOrderByIdAsc(10L)).thenReturn(all);
        lenient().when(characters.findAllByUserIdIn(any())).thenReturn(List.of(
                character(guitar, "기타왕"), character(drum, "드럼짱"), character(vocal, "보컬")));

        post = new BandPost(band, guitar, "합주 일정", "토요일 2시");
        ReflectionTestUtils.setField(post, "id", 20L);
        lenient().when(posts.findForUpdate(20L, 10L)).thenReturn(Optional.of(post));
        lenient().when(comments.save(any(BandComment.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(posts.save(any(BandPost.class))).thenAnswer(i -> {
            BandPost saved = i.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 21L);
            return saved;
        });
    }

    /* ---------- 댓글 ---------- */

    @Test void commentOnMyPostNotifiesPostAuthor() {
        service.addComment(2L, 10L, 20L, new BandBoardRequest.Comment("좋아요", null));

        assertThat(sent()).containsExactly(Map.entry(NotificationType.COMMENT, List.of(1L)));
        assertThat(lastContent()).isEqualTo("드럼짱 님이 내 글에 댓글을 남겼어요: 좋아요");
        assertThat(lastUrl()).isEqualTo("/home?band=10&post=20");
    }

    @Test void myOwnCommentNotifiesNobody() {
        service.addComment(1L, 10L, 20L, new BandBoardRequest.Comment("내가 단 댓글", null));

        // 받는 사람 목록에 나만 있어도 notifyAll이 나를 뺀다 (여기서는 넘긴 목록만 확인)
        assertThat(sent()).containsExactly(Map.entry(NotificationType.COMMENT, List.of(1L)));
        assertThat(lastActor()).isEqualTo(1L);
    }

    @Test void replyNotifiesParentAuthorAndPostAuthorOnce() {
        BandComment parent = new BandComment(post, drum, "몇 시예요?", null);
        ReflectionTestUtils.setField(parent, "id", 30L);
        when(comments.findByIdAndPostId(30L, 20L)).thenReturn(Optional.of(parent));

        service.addComment(3L, 10L, 20L, new BandBoardRequest.Comment("2시래요", 30L));

        assertThat(sent()).containsExactly(
                Map.entry(NotificationType.REPLY, List.of(2L)),
                Map.entry(NotificationType.COMMENT, List.of(1L)));
    }

    @Test void mentionWinsOverReplyAndComment() {
        BandComment parent = new BandComment(post, drum, "몇 시예요?", null);
        ReflectionTestUtils.setField(parent, "id", 30L);
        when(comments.findByIdAndPostId(30L, 20L)).thenReturn(Optional.of(parent));

        // 글쓴이(기타왕)와 댓글 단 사람(드럼짱)을 둘 다 부르면 멘션 하나씩만
        service.addComment(3L, 10L, 20L, new BandBoardRequest.Comment("@기타왕 @드럼짱 2시 맞죠?", 30L));

        assertThat(sent()).containsExactly(Map.entry(NotificationType.MENTION, List.of(1L, 2L)));
    }

    /* ---------- 글·공지 ---------- */

    @Test void newPostMentionsAndNewNoticeGoToDifferentPeople() {
        var request = new BandBoardRequest.Save("공지", "@드럼짱 스틱 챙겨 오세요", true, List.of(), null);

        service.save(1L, 10L, null, request, List.of());

        // 불린 드럼짱은 멘션, 나머지는 공지 (글쓴이 본인은 notifyAll에서 빠진다)
        assertThat(sent()).containsExactly(
                Map.entry(NotificationType.MENTION, List.of(2L)),
                Map.entry(NotificationType.NOTICE, List.of(1L, 3L)));
    }

    @Test void editingNotifiesOnlyNewlyMentionedMembers() {
        post.update("합주 일정", "@드럼짱 토요일 2시");
        var request = new BandBoardRequest.Save("합주 일정", "@드럼짱 @보컬 토요일 3시로 바꿔요", null, List.of(), null);

        service.save(1L, 10L, 20L, request, List.of());

        assertThat(sent()).containsExactly(Map.entry(NotificationType.MENTION, List.of(3L)));
    }

    @Test void turningOnNoticeNotifiesOnceButKeepingItOnDoesNot() {
        service.notice(1L, 10L, 20L, true, null);
        assertThat(sent()).containsExactly(Map.entry(NotificationType.NOTICE, List.of(1L, 2L, 3L)));

        Mockito.clearInvocations(notificationService);
        service.notice(1L, 10L, 20L, true, Instant.now().plusSeconds(3600));
        assertThat(sent()).isEmpty();
    }

    /* ---------- 준비 ---------- */

    // notifyAll로 넘긴 (종류 → 받는 사람 id) 중 받는 사람이 있는 것만, 부른 순서대로
    private Map<NotificationType, List<Long>> sent() {
        Map<NotificationType, List<Long>> result = new LinkedHashMap<>();
        for (Invocation call : notifyAllCalls()) {
            Collection<User> receivers = call.getArgument(0);
            if (receivers.isEmpty()) continue;
            result.put(call.getArgument(2), receivers.stream().map(User::getId).toList());
        }
        return result;
    }

    private List<Invocation> notifyAllCalls() {
        return Mockito.mockingDetails(notificationService).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("notifyAll"))
                .filter(i -> !((Collection<?>) i.getArgument(0)).isEmpty())
                .toList();
    }

    private Invocation last() {
        List<Invocation> calls = notifyAllCalls();
        return calls.get(calls.size() - 1);
    }

    private Long lastActor() {
        return last().getArgument(1);
    }

    private String lastContent() {
        return last().getArgument(4);
    }

    private String lastUrl() {
        return last().getArgument(5);
    }

    private User user(long id, String name) {
        User user = User.builder().email(id + "@example.com").build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private BandMember member(User user, Position position) {
        BandMember member = BandMember.builder().band(band).user(user).position(position).part(InstrumentType.GUITAR).build();
        ReflectionTestUtils.setField(member, "id", user.getId() * 100);
        return member;
    }

    private Character character(User user, String nickname) {
        return Character.builder().user(user).nickname(nickname).instrumentType(InstrumentType.GUITAR)
                .instrumentColor("#000000").gender(Gender.values()[0]).hairStyle(HairStyle.values()[0])
                .hairColor("#000000").currentOutfit(mock(OutfitItem.class)).build();
    }
}
