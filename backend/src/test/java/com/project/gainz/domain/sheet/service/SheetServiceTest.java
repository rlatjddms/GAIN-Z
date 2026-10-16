package com.project.gainz.domain.sheet.service;

import com.project.gainz.domain.sheet.dto.request.SheetRequest;
import com.project.gainz.domain.sheet.dto.request.WorkspaceUpdateRequest;
import com.project.gainz.domain.sheet.dto.response.SheetDetailResponse;
import com.project.gainz.domain.sheet.dto.response.WorkspaceDetailResponse;
import com.project.gainz.domain.sheet.dto.response.WorkspaceItemResponse;
import com.project.gainz.domain.sheet.entity.SheetMusic;
import com.project.gainz.domain.sheet.entity.SheetPage;
import com.project.gainz.domain.sheet.entity.UserWorkspace;
import com.project.gainz.domain.sheet.enums.SheetType;
import com.project.gainz.domain.sheet.repository.SheetMusicRepository;
import com.project.gainz.domain.sheet.repository.SheetPageRepository;
import com.project.gainz.domain.sheet.repository.TabDataRepository;
import com.project.gainz.domain.sheet.repository.UserWorkspaceRepository;
import com.project.gainz.domain.sheet.service.SheetFileStorage.StoredFile;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.common.enums.InstrumentType;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SheetServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_ID = 2L;

    @Mock private SheetMusicRepository sheetMusicRepository;
    @Mock private SheetPageRepository sheetPageRepository;
    @Mock private TabDataRepository tabDataRepository;
    @Mock private UserWorkspaceRepository userWorkspaceRepository;
    @Mock private UserRepository userRepository;
    @Mock private SheetFileStorage fileStorage;

    @InjectMocks private SheetService sheetService;

    private User user(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private SheetMusic sheet(Long id, User owner, String title) {
        SheetMusic sheet = SheetMusic.builder().owner(owner).title(title).artist("").originalKey("C")
                .instrumentType(InstrumentType.GUITAR).sheetTypes(List.of(SheetType.SCORE)).build();
        ReflectionTestUtils.setField(sheet, "id", id);
        return sheet;
    }

    private SheetPage page(Long id, SheetMusic sheet, int number, String contentType) {
        SheetPage page = SheetPage.builder().sheetMusic(sheet).pageNumber(number).storedName("f" + id)
                .contentType(contentType).fileName("p" + number).size(100L).build();
        ReflectionTestUtils.setField(page, "id", id);
        return page;
    }

    private UserWorkspace item(Long id, SheetMusic sheet) {
        UserWorkspace item = UserWorkspace.builder().user(mock(User.class)).sheetMusic(sheet).build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private SheetRequest req(String title) {
        return new SheetRequest(title, " 아이유 ", "G", 92, InstrumentType.GUITAR, SheetType.TAB);
    }

    private MultipartFile file(String name) {
        return new MockMultipartFile("files", name, "image/png", new byte[]{1});
    }

    private List<MultipartFile> files(int n) {
        return IntStream.rangeClosed(1, n).mapToObj(i -> file("p" + i + ".png")).toList();
    }

    private void storeAnything() {
        when(fileStorage.store(any())).thenAnswer(inv -> {
            MultipartFile f = inv.getArgument(0);
            return new StoredFile("s-" + f.getOriginalFilename(), "image/png", f.getOriginalFilename(), f.getSize());
        });
    }

    /* ---------- 올리기 ---------- */

    @Test
    void 악보를_올리면_고른_순서대로_장이_되고_보관함에_담긴다() {
        User me = mock(User.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(me));
        when(sheetMusicRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sheetPageRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(userWorkspaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        storeAnything();

        WorkspaceDetailResponse result = sheetService.uploadSheet(USER_ID, req("  밤편지 "), List.of(file("a.png"), file("b.png")));

        ArgumentCaptor<SheetMusic> captor = ArgumentCaptor.forClass(SheetMusic.class);
        verify(sheetMusicRepository).save(captor.capture());
        SheetMusic saved = captor.getValue();
        assertThat(saved.getOwner()).isSameAs(me);
        assertThat(saved.getTitle()).isEqualTo("밤편지");
        assertThat(saved.getArtist()).isEqualTo("아이유");
        assertThat(saved.getSheetTypes()).containsExactly(SheetType.TAB);
        assertThat(result.sheet().pages()).extracting(SheetDetailResponse.PageView::pageNumber).containsExactly(1, 2);
        assertThat(result.sheet().pages()).extracting(SheetDetailResponse.PageView::fileName).containsExactly("a.png", "b.png");
    }

    @Test
    void 파일_없이는_올릴_수_없다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));

        assertThatThrownBy(() -> sheetService.uploadSheet(USER_ID, req("곡"), List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_FILE_REQUIRED);
        assertThatThrownBy(() -> sheetService.uploadSheet(USER_ID, req("곡"), null))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_FILE_REQUIRED);
        verify(sheetMusicRepository, never()).save(any());
    }

    @Test
    void 한_번에_20장을_넘게_올릴_수_없다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));

        assertThatThrownBy(() -> sheetService.uploadSheet(USER_ID, req("곡"), files(SheetService.MAX_PAGES + 1)))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_PAGE_LIMIT_EXCEEDED);
        verify(fileStorage, never()).store(any());
    }

    @Test
    void 올리다_중간에_실패하면_먼저_저장한_파일을_지운다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(sheetMusicRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorage.store(any()))
                .thenReturn(new StoredFile("ok.png", "image/png", "a.png", 1))
                .thenThrow(new BusinessException(ErrorCode.INVALID_SHEET_FILE));

        assertThatThrownBy(() -> sheetService.uploadSheet(USER_ID, req("곡"), List.of(file("a.png"), file("b.txt"))))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SHEET_FILE);
        verify(fileStorage).delete("ok.png");
        verify(userWorkspaceRepository, never()).save(any());
    }

    @Test
    void 보관함이_가득_차면_올릴_수_없다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(userWorkspaceRepository.countByUserId(USER_ID)).thenReturn((long) SheetService.MAX_WORKSPACE_ITEMS);

        assertThatThrownBy(() -> sheetService.uploadSheet(USER_ID, req("곡"), files(1)))
                .extracting("errorCode").isEqualTo(ErrorCode.WORKSPACE_LIMIT_EXCEEDED);
        verify(sheetMusicRepository, never()).save(any());
    }

    /* ---------- 곡 정보·장 고치기 ---------- */

    @Test
    void 내가_올린_악보의_곡_정보를_고친다() {
        SheetMusic mine = sheet(10L, user(USER_ID), "옛 제목");
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.of(mine));

        SheetDetailResponse result = sheetService.updateSheet(USER_ID, 10L,
                new SheetRequest("새 제목", null, "", null, InstrumentType.BASS, SheetType.TAB));

        assertThat(result.title()).isEqualTo("새 제목");
        assertThat(result.originalKey()).isNull();
        assertThat(result.instrumentType()).isEqualTo(InstrumentType.BASS);
        assertThat(result.sheetTypes()).containsExactly(SheetType.TAB);
        assertThat(result.mine()).isTrue();
    }

    @Test
    void 남이_올린_악보나_공용_악보는_고칠_수_없다() {
        SheetMusic others = sheet(10L, user(OTHER_ID), "남의 악보");
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.of(others));
        when(sheetMusicRepository.findById(11L)).thenReturn(Optional.of(sheet(11L, null, "공용 악보")));

        assertThatThrownBy(() -> sheetService.updateSheet(USER_ID, 10L, req("x")))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_EDIT_DENIED);
        assertThatThrownBy(() -> sheetService.addPages(USER_ID, 11L, files(1)))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_EDIT_DENIED);
    }

    @Test
    void 장을_더_올리면_뒤에_이어서_번호가_붙는다() {
        SheetMusic mine = sheet(10L, user(USER_ID), "곡");
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.of(mine));
        when(sheetPageRepository.findAllBySheetMusicIdOrderByPageNumberAsc(10L))
                .thenReturn(List.of(page(1L, mine, 1, "image/png"), page(2L, mine, 2, "image/png")));
        when(sheetPageRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        storeAnything();

        sheetService.addPages(USER_ID, 10L, files(2));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SheetPage>> captor = ArgumentCaptor.forClass(List.class);
        verify(sheetPageRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(SheetPage::getPageNumber).containsExactly(3, 4);
    }

    @Test
    void 합쳐서_20장을_넘게_붙일_수_없다() {
        SheetMusic mine = sheet(10L, user(USER_ID), "곡");
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.of(mine));
        List<SheetPage> full = new ArrayList<>();
        for (int i = 1; i <= SheetService.MAX_PAGES; i++) full.add(page((long) i, mine, i, "image/png"));
        when(sheetPageRepository.findAllBySheetMusicIdOrderByPageNumberAsc(10L)).thenReturn(full);

        assertThatThrownBy(() -> sheetService.addPages(USER_ID, 10L, files(1)))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_PAGE_LIMIT_EXCEEDED);
    }

    @Test
    void 장을_지우면_뒤_장_번호를_당기고_파일도_지운다() {
        SheetMusic mine = sheet(10L, user(USER_ID), "곡");
        SheetPage p1 = page(1L, mine, 1, "image/png");
        SheetPage p2 = page(2L, mine, 2, "image/png");
        SheetPage p3 = page(3L, mine, 3, "application/pdf");
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.of(mine));
        when(sheetPageRepository.findAllBySheetMusicIdOrderByPageNumberAsc(10L)).thenReturn(List.of(p1, p2, p3));

        SheetDetailResponse result = sheetService.deletePage(USER_ID, 10L, 2L);

        verify(sheetPageRepository).delete(p2);
        verify(fileStorage).delete("f2");
        assertThat(result.pages()).extracting(SheetDetailResponse.PageView::id).containsExactly(1L, 3L);
        assertThat(result.pages()).extracting(SheetDetailResponse.PageView::pageNumber).containsExactly(1, 2);
    }

    @Test
    void 마지막_한_장은_지울_수_없다() {
        SheetMusic mine = sheet(10L, user(USER_ID), "곡");
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.of(mine));
        when(sheetPageRepository.findAllBySheetMusicIdOrderByPageNumberAsc(10L)).thenReturn(List.of(page(1L, mine, 1, "image/png")));

        assertThatThrownBy(() -> sheetService.deletePage(USER_ID, 10L, 1L))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_LAST_PAGE);
        verify(fileStorage, never()).delete(any());
    }

    /* ---------- 보기 ---------- */

    @Test
    void 남의_악보는_보관함에_없으면_볼_수도_파일을_받을_수도_없다() {
        SheetMusic others = sheet(10L, user(OTHER_ID), "남의 악보");
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.of(others));
        when(userWorkspaceRepository.existsByUserIdAndSheetMusicId(USER_ID, 10L)).thenReturn(false);

        assertThatThrownBy(() -> sheetService.getSheet(USER_ID, 10L))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_NOT_FOUND);
        assertThatThrownBy(() -> sheetService.getPageFile(USER_ID, 10L, 1L))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_NOT_FOUND);
        verify(fileStorage, never()).load(any());
    }

    @Test
    void 다른_악보의_장_번호로는_파일을_받을_수_없다() {
        SheetMusic mine = sheet(10L, user(USER_ID), "곡");
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.of(mine));
        when(sheetPageRepository.findByIdAndSheetMusicId(99L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sheetService.getPageFile(USER_ID, 10L, 99L))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_PAGE_NOT_FOUND);
    }

    /* ---------- 보관함 ---------- */

    @Test
    void 보관함은_즐겨찾기_필터와_제목순_정렬을_지원하고_첫_장_미리보기를_준다() {
        SheetMusic s1 = sheet(10L, null, "봄날");
        SheetMusic s2 = sheet(11L, null, "가로수 그늘 아래");
        SheetMusic s3 = sheet(12L, null, "나비");
        UserWorkspace a = item(1L, s1);
        UserWorkspace b = item(2L, s2);
        UserWorkspace c = item(3L, s3);
        b.changeFavorite(true);
        c.changeFavorite(true);
        when(userWorkspaceRepository.findAllWithSheetByUserId(USER_ID)).thenReturn(List.of(c, b, a));
        when(sheetPageRepository.findAllBySheetMusicIdInOrderByPageNumberAsc(anyList())).thenReturn(List.of(
                page(100L, s1, 1, "image/jpeg"), page(101L, s1, 2, "image/jpeg"), page(200L, s2, 1, "application/pdf")));

        List<WorkspaceItemResponse> recent = sheetService.getWorkspace(USER_ID, false, "recent");
        assertThat(recent).extracting(WorkspaceItemResponse::id).containsExactly(3L, 2L, 1L);
        assertThat(recent).extracting(WorkspaceItemResponse::pageCount).containsExactly(0, 1, 2);
        assertThat(recent).extracting(WorkspaceItemResponse::coverPageId).containsExactly(null, null, 100L);
        assertThat(sheetService.getWorkspace(USER_ID, false, "title"))
                .extracting(WorkspaceItemResponse::title).containsExactly("가로수 그늘 아래", "나비", "봄날");
        assertThat(sheetService.getWorkspace(USER_ID, true, "recent"))
                .extracting(WorkspaceItemResponse::id).containsExactly(3L, 2L);
    }

    @Test
    void 공용_악보를_보관함에_담는다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(sheetMusicRepository.findById(11L)).thenReturn(Optional.of(sheet(11L, null, "공용 악보")));
        when(userWorkspaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceItemResponse result = sheetService.addToWorkspace(USER_ID, 11L);

        assertThat(result.sheetId()).isEqualTo(11L);
        assertThat(result.favorite()).isFalse();
    }

    @Test
    void 이미_담은_악보는_다시_담을_수_없다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(sheetMusicRepository.findById(11L)).thenReturn(Optional.of(sheet(11L, null, "공용 악보")));
        when(userWorkspaceRepository.existsByUserIdAndSheetMusicId(USER_ID, 11L)).thenReturn(true);

        assertThatThrownBy(() -> sheetService.addToWorkspace(USER_ID, 11L))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_ALREADY_IN_WORKSPACE);
    }

    @Test
    void 남이_올린_악보는_보관함에_담을_수_없다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        SheetMusic others = sheet(10L, user(OTHER_ID), "남의 악보");
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.of(others));

        assertThatThrownBy(() -> sheetService.addToWorkspace(USER_ID, 10L))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_NOT_FOUND);
    }

    @Test
    void 보낸_항목만_바꾸고_원래_키로_바꾸면_따로_저장하지_않는다() {
        UserWorkspace item = item(1L, sheet(10L, null, "곡"));
        item.changePersonalMemo("브릿지 느리게");
        item.changeCustomKey("D");
        when(userWorkspaceRepository.findWithSheetByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(item));

        WorkspaceDetailResponse favorited = sheetService.updateWorkspaceItem(USER_ID, 1L, new WorkspaceUpdateRequest(null, null, true));
        assertThat(favorited.favorite()).isTrue();
        assertThat(favorited.customKey()).isEqualTo("D");
        assertThat(favorited.personalMemo()).isEqualTo("브릿지 느리게");

        WorkspaceDetailResponse reset = sheetService.updateWorkspaceItem(USER_ID, 1L, new WorkspaceUpdateRequest("C", "", null));
        assertThat(reset.customKey()).isNull();
        assertThat(reset.personalMemo()).isNull();
        assertThat(reset.favorite()).isTrue();
    }

    @Test
    void 남의_보관함_항목은_찾을_수_없다() {
        when(userWorkspaceRepository.findWithSheetByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sheetService.getWorkspaceItem(USER_ID, 1L))
                .extracting("errorCode").isEqualTo(ErrorCode.WORKSPACE_ITEM_NOT_FOUND);
        assertThatThrownBy(() -> sheetService.removeFromWorkspace(USER_ID, 1L))
                .extracting("errorCode").isEqualTo(ErrorCode.WORKSPACE_ITEM_NOT_FOUND);
    }

    @Test
    void 내가_올린_악보를_빼면_악보와_파일도_지운다() {
        SheetMusic mine = sheet(10L, user(USER_ID), "내 악보");
        UserWorkspace item = item(1L, mine);
        List<SheetPage> pages = List.of(page(1L, mine, 1, "image/png"), page(2L, mine, 2, "application/pdf"));
        when(userWorkspaceRepository.findWithSheetByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(item));
        when(userWorkspaceRepository.existsBySheetMusicId(10L)).thenReturn(false);
        when(sheetPageRepository.findAllBySheetMusicIdOrderByPageNumberAsc(10L)).thenReturn(pages);

        sheetService.removeFromWorkspace(USER_ID, 1L);

        verify(userWorkspaceRepository).delete(item);
        verify(sheetPageRepository).deleteAll(pages);
        verify(sheetMusicRepository).delete(mine);
        verify(fileStorage).delete("f1");
        verify(fileStorage).delete("f2");
    }

    @Test
    void 공용_악보를_빼면_보관함에서만_빠진다() {
        SheetMusic shared = sheet(11L, null, "공용 악보");
        UserWorkspace item = item(1L, shared);
        when(userWorkspaceRepository.findWithSheetByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(item));

        sheetService.removeFromWorkspace(USER_ID, 1L);

        verify(userWorkspaceRepository).delete(item);
        verify(sheetMusicRepository, never()).delete(any());
        verify(fileStorage, never()).delete(any());
    }

    @Test
    void 공용_악보_목록() {
        when(sheetMusicRepository.findAllByOwnerIsNullOrderByTitleAsc()).thenReturn(Collections.emptyList());

        assertThat(sheetService.getSharedSheets()).isEmpty();
    }
}
