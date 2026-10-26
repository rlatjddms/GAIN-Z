package com.project.gainz.domain.band.service;

import com.project.gainz.domain.band.dto.request.SetlistRequest;
import com.project.gainz.domain.band.entity.Band;
import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.entity.BandPostFile;
import com.project.gainz.domain.band.entity.Setlist;
import com.project.gainz.domain.band.entity.SetlistItem;
import com.project.gainz.domain.band.enums.BandUploadKind;
import com.project.gainz.domain.band.enums.Position;
import com.project.gainz.domain.band.repository.BandMemberRepository;
import com.project.gainz.domain.band.repository.SetlistRepository;
import com.project.gainz.domain.notification.service.NotificationService;
import com.project.gainz.domain.sheet.entity.SheetMusic;
import com.project.gainz.domain.sheet.entity.UserWorkspace;
import com.project.gainz.domain.sheet.repository.SheetMusicRepository;
import com.project.gainz.domain.sheet.repository.UserWorkspaceRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.domain.notification.enums.NotificationType;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;

@ExtendWith(MockitoExtension.class)
class SetlistServiceTest {
    @Mock BandMemberRepository members;
    @Mock SetlistRepository setlists;
    @Mock SheetMusicRepository sheets;
    @Mock UserWorkspaceRepository workspaces;
    @Mock BandAttachmentStorage storage;
    @Mock NotificationService notificationService;
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
        // 새로 담당이 된 멤버에게 (지정한 본인은 notifyAll에서 빠진다)
        verify(notificationService).notifyAll(eq(List.of(drummer.getUser(), me.getUser())), eq(1L),
                eq(NotificationType.SETLIST_ASSIGN), eq("밴드"), anyString(), startsWith("/bands/10/setlists?item="));
    }

    @Test void alreadyAssignedMembersAreNotNotifiedAgain() {
        Setlist setlist = setlist(30);
        SetlistItem existing = item(setlist, 40, 0);
        existing.replaceAssignees(List.of(drummer));
        service.saveItem(1L, 10L, 30L, 40L, item(null, List.of(200L)), null);
        verify(notificationService).notifyAll(eq(List.of()), eq(1L), eq(NotificationType.SETLIST_ASSIGN), eq("밴드"),
                anyString(), eq("/bands/10/setlists?item=40"));
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
        when(storage.store(upload, BandUploadKind.SETLIST_FILE)).thenReturn(new BandAttachmentStorage.StoredFile("new", "new.png", "image/png"));
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
        User user = User.builder().email(id + "@example.com").build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void fails(Runnable work, ErrorCode code) {
        assertThatThrownBy(work::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }
}
