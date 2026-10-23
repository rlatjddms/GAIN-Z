package com.project.gainz.domain.band.service;

import com.project.gainz.domain.band.dto.request.JamRequest;
import com.project.gainz.domain.band.dto.response.JamResponse;
import com.project.gainz.domain.band.entity.Band;
import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.entity.BandPostFile;
import com.project.gainz.domain.band.entity.Jam;
import com.project.gainz.domain.band.entity.JamTrack;
import com.project.gainz.domain.band.entity.Setlist;
import com.project.gainz.domain.band.entity.SetlistItem;
import com.project.gainz.domain.band.enums.Position;
import com.project.gainz.domain.band.repository.BandMemberRepository;
import com.project.gainz.domain.band.repository.JamRepository;
import com.project.gainz.domain.band.repository.JamTrackRepository;
import com.project.gainz.domain.band.repository.SetlistItemRepository;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.notification.service.NotificationService;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.common.enums.InstrumentType;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.domain.notification.enums.NotificationType;
import com.project.gainz.domain.record.enums.RecordMetric;
import com.project.gainz.domain.record.event.RecordCheckEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class JamServiceTest {
    @Mock BandMemberRepository members;
    @Mock JamRepository jams;
    @Mock JamTrackRepository tracks;
    @Mock SetlistItemRepository setlistItems;
    @Mock CharacterRepository characters;
    @Mock BandAttachmentStorage storage;
    @Mock NotificationService notificationService;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks JamService service;

    Band band;
    BandMember me;
    BandMember drummer;
    BandMember leader;
    long nextTrackId = 500;

    @BeforeEach void setUp() {
        band = Band.builder().name("밴드").leader(user(9)).inviteCode("ABCDEFGH").build();
        ReflectionTestUtils.setField(band, "id", 10L);
        me = member(1, Position.MEMBER, InstrumentType.GUITAR);
        drummer = member(2, Position.MEMBER, InstrumentType.DRUM);
        leader = member(9, Position.LEADER, InstrumentType.VOCAL);
        lenient().when(characters.findAllByUserIdIn(any())).thenReturn(List.of(character(me, "기타"), character(drummer, "드럼")));
        lenient().when(tracks.save(any(JamTrack.class))).thenAnswer(i -> {
            JamTrack track = i.getArgument(0);
            ReflectionTestUtils.setField(track, "id", nextTrackId++);
            return track;
        });
    }

    @AfterEach void clearSync() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    @Test void onlyBandMembersCanUseJams() {
        fails(() -> service.list(3L, 10L), ErrorCode.NOT_BAND_MEMBER);
        fails(() -> service.addTrack(3L, 10L, 50L, new JamRequest.Track(0, null, 1000), audio()), ErrorCode.NOT_BAND_MEMBER);
        verifyNoInteractions(jams, storage);
    }

    @Test void anyMemberCanFavoriteAndListShowsFavoriteAndWhoCanDelete() {
        Jam mine = jam(50L, me);
        Jam drummers = jam(51L, drummer);
        when(jams.findAllByBandIdOrderByIdDesc(10L)).thenReturn(List.of(drummers, mine));

        // 드럼 멤버가 내 잼을 즐겨찾기해도 된다
        assertThat(service.setFavorite(2L, 10L, 50L, true).favorite()).isTrue();
        assertThat(mine.isFavorite()).isTrue();

        // 나에게는 내 잼만 지울 수 있게 보이고, 리더에게는 모두
        assertThat(service.list(1L, 10L)).extracting(JamResponse.Summary::id, JamResponse.Summary::favorite, JamResponse.Summary::canManage)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(51L, false, false), org.assertj.core.groups.Tuple.tuple(50L, true, true));
        assertThat(service.list(9L, 10L)).extracting(JamResponse.Summary::canManage).containsExactly(true, true);

        fails(() -> service.setFavorite(3L, 10L, 50L, true), ErrorCode.NOT_BAND_MEMBER);
    }

    @Test void createsJamLinkedToSetlistSongOfSameBand() {
        SetlistItem song = song(31L, band, "Hysteria");
        when(setlistItems.findById(31L)).thenReturn(Optional.of(song));
        when(jams.save(any(Jam.class))).thenAnswer(i -> { Jam saved = i.getArgument(0); ReflectionTestUtils.setField(saved, "id", 50L); return saved; });
        when(members.findAllByBandIdOrderByIdAsc(10L)).thenReturn(List.of(me, drummer, leader));

        JamResponse.Jam result = service.create(1L, 10L, new JamRequest.Save("  합주 ", 120, 31L));

        assertThat(result.id()).isEqualTo(50L);
        assertThat(result.title()).isEqualTo("합주");
        assertThat(result.bpm()).isEqualTo(120);
        assertThat(result.song()).isEqualTo(new JamResponse.Song(31L, "Hysteria"));
        assertThat(result.creatorName()).isEqualTo("기타");
        assertThat(result.canManage()).isTrue();
        assertThat(result.tracks()).isEmpty();
        // 멤버 모두에게 새 잼 알림 (만든 사람은 notifyAll에서 빠진다)
        verify(notificationService).notifyAll(eq(List.of(me.getUser(), drummer.getUser(), leader.getUser())), eq(1L),
                eq(NotificationType.JAM_NEW), eq("밴드"), anyString(), eq("/bands/10/jams?jam=50"));
    }

    @Test void rejectsSongOfOtherBandAndTooManyJams() {
        Band other = Band.builder().name("다른 밴드").leader(user(7)).inviteCode("IJKLMNOP").build();
        ReflectionTestUtils.setField(other, "id", 11L);
        when(setlistItems.findById(31L)).thenReturn(Optional.of(song(31L, other, "남의 곡")));
        fails(() -> service.create(1L, 10L, new JamRequest.Save("합주", 120, 31L)), ErrorCode.JAM_SONG_INVALID);

        when(jams.countByBandId(10L)).thenReturn((long) JamService.MAX_JAMS);
        fails(() -> service.create(1L, 10L, new JamRequest.Save("합주", 120, null)), ErrorCode.JAM_LIMIT_EXCEEDED);
        verify(jams, never()).save(any());
    }

    @Test void listShowsTrackCountAndCreator() {
        Jam jam = jam(50L, me);
        track(jam, drummer);
        track(jam, me);
        when(jams.findAllByBandIdOrderByIdDesc(10L)).thenReturn(List.of(jam));

        var result = service.list(2L, 10L);

        assertThat(result).singleElement().satisfies(summary -> {
            assertThat(summary.trackCount()).isEqualTo(2);
            assertThat(summary.creatorName()).isEqualTo("기타");
            assertThat(summary.song()).isNull();
        });
    }

    @Test void onlyCreatorOrLeaderCanEditOrDeleteJamAndFilesAreRemovedAfterCommit() {
        Jam jam = jam(50L, me);
        JamTrack first = track(jam, drummer);
        JamTrack second = track(jam, me);

        fails(() -> service.update(2L, 10L, 50L, new JamRequest.Save("바꿈", 90, null)), ErrorCode.JAM_PERMISSION_DENIED);
        fails(() -> service.delete(2L, 10L, 50L), ErrorCode.JAM_PERMISSION_DENIED);

        assertThat(service.update(9L, 10L, 50L, new JamRequest.Save("바꿈", 90, null)).title()).isEqualTo("바꿈");
        assertThat(service.get(2L, 10L, 50L).canManage()).isFalse();

        TransactionSynchronizationManager.initSynchronization();
        service.delete(1L, 10L, 50L);
        verify(jams).delete(jam);
        verify(storage, never()).delete(anyString());
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(storage).delete(first.getFile().getKey());
        verify(storage).delete(second.getFile().getKey());
    }

    @Test void uploadedTrackUsesMembersPartAndDefaultVolume() {
        Jam jam = jam(50L, drummer);
        var file = audio();
        when(storage.storeAudio(file)).thenReturn(new BandAttachmentStorage.StoredFile("key-new", "take.webm", "audio/webm"));
        TransactionSynchronizationManager.initSynchronization();

        JamResponse.Track result = service.addTrack(1L, 10L, 50L, new JamRequest.Track(-2170, null, 3200), file);

        assertThat(result.part()).isEqualTo(InstrumentType.GUITAR);
        assertThat(result.nickname()).isEqualTo("기타");
        assertThat(result.volume()).isEqualTo(100);
        assertThat(result.offsetMs()).isEqualTo(-2170);
        assertThat(result.durationMs()).isEqualTo(3200);
        assertThat(result.type()).isEqualTo("audio/webm");
        assertThat(result.canDelete()).isTrue();
        assertThat(result.url()).isEqualTo("/api/v1/bands/10/jams/50/tracks/" + result.id() + "/file");
        assertThat(jam.getTracks()).hasSize(1);
        verify(eventPublisher).publishEvent(RecordCheckEvent.of(1L, RecordMetric.JAM_TRACKS));
        // 잼 만든 사람에게 트랙 알림
        verify(notificationService).notifyAll(eq(List.of(drummer.getUser())), eq(1L), eq(NotificationType.JAM_TRACK),
                eq("밴드"), anyString(), eq("/bands/10/jams?jam=50"));

        // DB 저장이 롤백되면 올린 파일을 지운다
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(storage).delete("key-new");
    }

    @Test void trackLimitIsCheckedBeforeStoringFile() {
        Jam jam = jam(50L, me);
        for (int i = 0; i < JamService.MAX_TRACKS; i++) track(jam, me);
        fails(() -> service.addTrack(1L, 10L, 50L, new JamRequest.Track(0, 100, 1000), audio()), ErrorCode.JAM_TRACK_LIMIT_EXCEEDED);
        verifyNoInteractions(storage);
    }

    @Test void anyMemberCanChangeMixButOnlyUploaderOrLeaderCanDelete() {
        Jam jam = jam(50L, me);
        JamTrack track = track(jam, me);

        JamResponse.Track changed = service.updateTrack(2L, 10L, 50L, track.getId(), new JamRequest.TrackUpdate(80, null));
        assertThat(changed.volume()).isEqualTo(80);
        assertThat(changed.offsetMs()).isZero();
        assertThat(changed.canDelete()).isFalse();
        service.updateTrack(2L, 10L, 50L, track.getId(), new JamRequest.TrackUpdate(null, 120));
        assertThat(track.getVolume()).isEqualTo(80);
        assertThat(track.getOffsetMs()).isEqualTo(120);

        fails(() -> service.deleteTrack(2L, 10L, 50L, track.getId()), ErrorCode.JAM_PERMISSION_DENIED);
        TransactionSynchronizationManager.initSynchronization();
        service.deleteTrack(9L, 10L, 50L, track.getId());
        assertThat(jam.getTracks()).isEmpty();
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(storage).delete(track.getFile().getKey());
    }

    @Test void missingJamOrTrackIsNotFound() {
        Jam jam = jam(50L, me);
        fails(() -> service.get(1L, 10L, 99L), ErrorCode.JAM_NOT_FOUND);
        fails(() -> service.file(1L, 10L, 50L, 999L), ErrorCode.JAM_TRACK_NOT_FOUND);
        assertThat(jam.getTracks()).isEmpty();
    }

    @Test void trackFileIsServedFromStorage() {
        Jam jam = jam(50L, me);
        JamTrack track = track(jam, drummer);
        var resource = new ByteArrayResource(new byte[] {1, 2});
        when(storage.load(track.getFile().getKey())).thenReturn(resource);

        var file = service.file(1L, 10L, 50L, track.getId());

        assertThat(file.resource()).isSameAs(resource);
        assertThat(file.type()).isEqualTo("audio/webm");
    }

    private MockMultipartFile audio() {
        return new MockMultipartFile("file", "take.webm", "audio/webm", new byte[] {0x1a, 0x45, (byte) 0xdf, (byte) 0xa3});
    }

    private Jam jam(long id, BandMember creator) {
        Jam jam = new Jam(band, creator.getUser(), "합주", 120);
        ReflectionTestUtils.setField(jam, "id", id);
        lenient().when(jams.findByIdAndBandId(id, 10L)).thenReturn(Optional.of(jam));
        return jam;
    }

    private JamTrack track(Jam jam, BandMember uploader) {
        long id = nextTrackId++;
        JamTrack track = JamTrack.builder().jam(jam).uploader(uploader.getUser()).part(uploader.getPart())
                .file(new BandPostFile("key-" + id, "take.webm", "audio/webm")).volume(100).offsetMs(0).durationMs(1000).build();
        ReflectionTestUtils.setField(track, "id", id);
        jam.getTracks().add(track);
        return track;
    }

    private SetlistItem song(long id, Band owner, String title) {
        SetlistItem item = new SetlistItem(new Setlist(owner, "셋리스트", 0), 0);
        ReflectionTestUtils.setField(item, "id", id);
        item.update(title, null, null, null);
        return item;
    }

    private Character character(BandMember member, String nickname) {
        return Character.builder().user(member.getUser()).nickname(nickname).instrumentType(member.getPart()).build();
    }

    private BandMember member(long userId, Position position, InstrumentType part) {
        BandMember member = BandMember.builder().band(band).user(user(userId)).position(position).part(part).build();
        ReflectionTestUtils.setField(member, "id", userId * 100);
        lenient().when(members.findByBandIdAndUserId(10L, userId)).thenReturn(Optional.of(member));
        return member;
    }

    private User user(long id) {
        User user = User.builder().name("멤버").email(id + "@example.com").build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void fails(Runnable work, ErrorCode code) {
        assertThatThrownBy(work::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }
}
