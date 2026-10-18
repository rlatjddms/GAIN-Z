package com.project.gainz.domain.band.service;

import com.project.gainz.domain.band.dto.request.SetlistRequest;
import com.project.gainz.domain.band.entity.Band;
import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.entity.BandPostFile;
import com.project.gainz.domain.band.entity.Setlist;
import com.project.gainz.domain.band.entity.SetlistItem;
import com.project.gainz.domain.band.enums.Position;
import com.project.gainz.domain.band.repository.BandMemberRepository;
import com.project.gainz.domain.band.repository.SetlistRepository;
import com.project.gainz.domain.practice.dto.response.RoutineDailySummaryResponse;
import com.project.gainz.domain.practice.dto.response.UserDailyPracticeTotal;
import com.project.gainz.domain.practice.repository.PracticeLogRepository;
import com.project.gainz.domain.practice.service.RoutineService;
import com.project.gainz.domain.sheet.entity.SheetMusic;
import com.project.gainz.domain.sheet.entity.UserWorkspace;
import com.project.gainz.domain.sheet.repository.SheetMusicRepository;
import com.project.gainz.domain.sheet.repository.UserWorkspaceRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetlistServiceTest {
    @Mock BandMemberRepository members;
    @Mock SetlistRepository setlists;
    @Mock SheetMusicRepository sheets;
    @Mock UserWorkspaceRepository workspaces;
    @Mock PracticeLogRepository practiceLogs;
    @Mock RoutineService routineService;
    @Mock BandAttachmentStorage storage;
    @InjectMocks SetlistService service;

    Band band;
    BandMember me;
    BandMember drummer;

    @BeforeEach void setUp() {
        band = Band.builder().name("밴드").leader(user(1)).inviteCode("ABCDEFGH").build();
        ReflectionTestUtils.setField(band, "id", 10L);
        me = member(100, 1);
        drummer = member(200, 2);
        lenient().when(members.findByBandIdAndUserId(10L, 1L)).thenReturn(Optional.of(me));
        lenient().when(members.findAllByBandIdOrderByIdAsc(10L)).thenReturn(List.of(me, drummer));
    }

    @AfterEach void clearSync() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    @Test void onlyBandMembersCanUseSetlists() {
        fails(() -> service.list(9L, 10L), ErrorCode.NOT_BAND_MEMBER);
        verifyNoInteractions(setlists);
    }

    @Test void createPutsNewSetlistAtTheEndAndRespectsLimit() {
        when(setlists.countByBandId(10L)).thenReturn(3L);
        when(setlists.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.create(1L, 10L, new SetlistRequest.Create("  정기 합주  "));
        assertThat(result.title()).isEqualTo("정기 합주");
        assertThat(result.favorite()).isFalse();
        when(setlists.countByBandId(10L)).thenReturn((long) SetlistService.MAX_SETLISTS);
        fails(() -> service.create(1L, 10L, new SetlistRequest.Create("하나 더")), ErrorCode.SETLIST_LIMIT_EXCEEDED);
    }

    @Test void updateChangesOnlySentFieldsAndRejectsBlankTitle() {
        Setlist setlist = setlist(30);
        var result = service.update(1L, 10L, 30L, new SetlistRequest.Update(null, true));
        assertThat(result.title()).isEqualTo("셋리스트");
        assertThat(result.favorite()).isTrue();
        fails(() -> service.update(1L, 10L, 30L, new SetlistRequest.Update(" ", null)), ErrorCode.SETLIST_TITLE_REQUIRED);
        assertThat(setlist.getTitle()).isEqualTo("셋리스트");
    }

    @Test void reorderNeedsEveryCurrentSetlistExactlyOnce() {
        Setlist a = setlist(30), b = setlist(31);
        when(setlists.findAllByBandIdOrderByListOrderAscIdAsc(10L)).thenReturn(List.of(a, b));
        var result = service.reorder(1L, 10L, new SetlistRequest.Order(List.of(31L, 30L)));
        assertThat(result).extracting(r -> r.id()).containsExactly(31L, 30L);
        assertThat(b.getListOrder()).isZero();
        assertThat(a.getListOrder()).isEqualTo(1);
        fails(() -> service.reorder(1L, 10L, new SetlistRequest.Order(List.of(31L))), ErrorCode.INVALID_SETLIST_ORDER);
        fails(() -> service.reorder(1L, 10L, new SetlistRequest.Order(List.of(31L, 31L))), ErrorCode.INVALID_SETLIST_ORDER);
    }

    @Test void addItemSavesTitleMemoAssigneesAndLinkedSheetInWorkspace() {
        Setlist setlist = setlist(30);
        SheetMusic sheet = sheet(7, null);
        when(sheets.findById(7L)).thenReturn(Optional.of(sheet));
        when(workspaces.existsByUserIdAndSheetMusicId(1L, 7L)).thenReturn(true);
        UserWorkspace mine = UserWorkspace.builder().user(user(1)).sheetMusic(sheet).build();
        ReflectionTestUtils.setField(mine, "id", 70L);
        when(workspaces.findAllByUserIdAndSheetIds(eq(1L), anyCollection())).thenReturn(List.of(mine));
        var result = service.saveItem(1L, 10L, 30L, null,
                new SetlistRequest.Item(" Hysteria ", " ", "후렴 반복", 7L, List.of(200L, 100L, 200L), List.of()), null);
        var item = result.items().get(0);
        assertThat(item.title()).isEqualTo("Hysteria");
        assertThat(item.artist()).isNull();
        assertThat(item.memo()).isEqualTo("후렴 반복");
        assertThat(item.memberIds()).containsExactly(200L, 100L);
        assertThat(item.sheet().sheetId()).isEqualTo(7L);
        assertThat(item.sheet().workspaceItemId()).isEqualTo(70L);
        assertThat(setlist.getItems().get(0).getPlayOrder()).isZero();
    }

    @Test void cannotLinkSheetTheEditorCannotSee() {
        setlist(30);
        SheetMusic other = sheet(8, user(5));
        when(sheets.findById(8L)).thenReturn(Optional.of(other));
        fails(() -> service.saveItem(1L, 10L, 30L, null, item(8L, List.of()), null), ErrorCode.SHEET_NOT_FOUND);
    }

    @Test void keepsAlreadyLinkedSheetEvenWhenEditorDoesNotOwnIt() {
        Setlist setlist = setlist(30);
        SetlistItem existing = item(setlist, 40, 0);
        SheetMusic other = sheet(8, user(5));
        existing.update("곡", null, null, other);
        var result = service.saveItem(1L, 10L, 30L, 40L, item(8L, List.of()), null);
        assertThat(result.items().get(0).sheet().sheetId()).isEqualTo(8L);
        assertThat(result.items().get(0).sheet().workspaceItemId()).isNull();
        verify(sheets, never()).findById(any());
    }

    @Test void assigneesMustBelongToTheBand() {
        setlist(30);
        fails(() -> service.saveItem(1L, 10L, 30L, null, item(null, List.of(999L)), null), ErrorCode.SETLIST_ASSIGNEE_INVALID);
    }

    @Test void itemLimitIsChecked() {
        Setlist setlist = setlist(30);
        for (int i = 0; i < SetlistService.MAX_ITEMS; i++) item(setlist, 40 + i, i);
        fails(() -> service.saveItem(1L, 10L, 30L, null, item(null, List.of()), null), ErrorCode.SETLIST_ITEM_LIMIT_EXCEEDED);
    }

    @Test void replacingAttachmentsCleansRemovedFileAfterCommitAndNewFileOnRollback() {
        Setlist setlist = setlist(30);
        SetlistItem existing = item(setlist, 40, 0);
        existing.replaceFiles(List.of(new BandPostFile("keep", "keep.pdf", "application/pdf"), new BandPostFile("drop", "drop.pdf", "application/pdf")));
        var upload = new MockMultipartFile("files", "new.png", "image/png", new byte[] {1});
        when(storage.store(upload)).thenReturn(new BandAttachmentStorage.StoredFile("new", "new.png", "image/png"));
        TransactionSynchronizationManager.initSynchronization();
        var result = service.saveItem(1L, 10L, 30L, 40L, new SetlistRequest.Item("곡", null, null, null, List.of(), List.of("keep")), List.of(upload));
        assertThat(result.items().get(0).attachments()).extracting(a -> a.id()).containsExactly("keep", "new");
        assertThat(result.items().get(0).attachments().get(1).url()).isEqualTo("/api/v1/bands/10/setlists/30/items/40/attachments/new");
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(storage).delete("drop");
        verify(storage, never()).delete("keep");
        verify(storage, never()).delete("new");
    }

    @Test void unknownKeptAttachmentOrTooManyFilesAreRejected() {
        Setlist setlist = setlist(30);
        item(setlist, 40, 0);
        fails(() -> service.saveItem(1L, 10L, 30L, 40L, new SetlistRequest.Item("곡", null, null, null, null, List.of("ghost")), null), ErrorCode.BAND_FILE_SELECTION_INVALID);
        var uploads = java.util.stream.IntStream.range(0, 9).mapToObj(i -> new MockMultipartFile("files", i + ".png", "image/png", new byte[] {1})).toList();
        fails(() -> service.saveItem(1L, 10L, 30L, 40L, new SetlistRequest.Item("곡", null, null, null, null, null), List.copyOf(uploads)), ErrorCode.BAND_FILE_LIMIT);
        verifyNoInteractions(storage);
    }

    @Test void reorderItemsAndDeleteItem() {
        Setlist setlist = setlist(30);
        item(setlist, 40, 0);
        item(setlist, 41, 1);
        var result = service.reorderItems(1L, 10L, 30L, new SetlistRequest.Order(List.of(41L, 40L)));
        assertThat(result.items()).extracting(i -> i.id()).containsExactly(41L, 40L);
        fails(() -> service.reorderItems(1L, 10L, 30L, new SetlistRequest.Order(List.of(41L))), ErrorCode.INVALID_SETLIST_ORDER);
        assertThat(service.deleteItem(1L, 10L, 30L, 41L).items()).extracting(i -> i.id()).containsExactly(40L);
        fails(() -> service.deleteItem(1L, 10L, 30L, 41L), ErrorCode.SETLIST_ITEM_NOT_FOUND);
    }

    @Test void setlistFromAnotherBandIsNotFound() {
        when(setlists.findByIdAndBandId(99L, 10L)).thenReturn(Optional.empty());
        fails(() -> service.delete(1L, 10L, 99L), ErrorCode.SETLIST_NOT_FOUND);
    }

    @Test void weeklyPracticeSumsFromMondayToTodayForEveryMember() {
        LocalDate wednesday = LocalDate.of(2026, 10, 14);
        LocalDate monday = LocalDate.of(2026, 10, 12);
        // 직접 수정으로 0이 된 날은 연습한 날로 세지 않는다
        when(practiceLogs.findDailyTotalsByUsers(List.of(1L, 2L), monday, wednesday)).thenReturn(List.of(
                new UserDailyPracticeTotal(1L, monday, 3600L), new UserDailyPracticeTotal(1L, monday.plusDays(1), 0L),
                new UserDailyPracticeTotal(1L, wednesday, 1800L)));
        when(routineService.getDailySummary(1L, monday, wednesday)).thenReturn(List.of(
                new RoutineDailySummaryResponse(monday, 2, 1), new RoutineDailySummaryResponse(wednesday, 2, 2)));
        when(routineService.getDailySummary(2L, monday, wednesday)).thenReturn(List.of());
        var week = service.practice(1L, 10L, wednesday);
        assertThat(week.from()).isEqualTo(monday);
        assertThat(week.to()).isEqualTo(LocalDate.of(2026, 10, 18));
        var mine = week.members().get(0);
        assertThat(mine.memberId()).isEqualTo(100L);
        assertThat(mine.practiceSeconds()).isEqualTo(5400L);
        assertThat(mine.practiceDays()).isEqualTo(2L);
        assertThat(mine.routineTotal()).isEqualTo(4);
        assertThat(mine.routineCompleted()).isEqualTo(3);
        var other = week.members().get(1);
        assertThat(other.practiceSeconds()).isZero();
        assertThat(other.routineTotal()).isZero();
    }

    private SetlistRequest.Item item(Long sheetId, List<Long> memberIds) {
        return new SetlistRequest.Item("곡", null, null, sheetId, memberIds, List.of());
    }

    private Setlist setlist(long id) {
        Setlist setlist = new Setlist(band, "셋리스트", 0);
        ReflectionTestUtils.setField(setlist, "id", id);
        lenient().when(setlists.findByIdAndBandId(id, 10L)).thenReturn(Optional.of(setlist));
        lenient().when(setlists.saveAndFlush(setlist)).thenReturn(setlist);
        return setlist;
    }

    private SetlistItem item(Setlist setlist, long id, int order) {
        SetlistItem item = new SetlistItem(setlist, order);
        ReflectionTestUtils.setField(item, "id", id);
        item.update("곡" + id, null, null, null);
        setlist.getItems().add(item);
        return item;
    }

    private SheetMusic sheet(long id, User owner) {
        SheetMusic sheet = mock(SheetMusic.class);
        lenient().when(sheet.getId()).thenReturn(id);
        lenient().when(sheet.getTitle()).thenReturn("Hysteria");
        lenient().when(sheet.getArtist()).thenReturn("Muse");
        lenient().when(sheet.isShared()).thenReturn(false);
        lenient().when(sheet.isOwnedBy(anyLong())).thenAnswer(i -> owner != null && owner.getId().equals(i.getArgument(0)));
        return sheet;
    }

    private BandMember member(long id, long userId) {
        BandMember member = BandMember.builder().band(band).user(user(userId)).position(Position.MEMBER).build();
        ReflectionTestUtils.setField(member, "id", id);
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
