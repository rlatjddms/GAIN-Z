package com.project.gainz.domain.sheet.service;

import com.project.gainz.domain.sheet.dto.request.SheetRequest;
import com.project.gainz.domain.sheet.dto.request.WorkspaceUpdateRequest;
import com.project.gainz.domain.sheet.dto.response.SheetDetailResponse;
import com.project.gainz.domain.sheet.dto.response.WorkspaceDetailResponse;
import com.project.gainz.domain.sheet.dto.response.WorkspaceItemResponse;
import com.project.gainz.domain.sheet.entity.SheetMusic;
import com.project.gainz.domain.sheet.entity.UserWorkspace;
import com.project.gainz.domain.sheet.enums.SheetType;
import com.project.gainz.domain.sheet.repository.SheetMusicRepository;
import com.project.gainz.domain.sheet.repository.TabDataRepository;
import com.project.gainz.domain.sheet.repository.UserWorkspaceRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SheetServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_ID = 2L;

    @Mock private SheetMusicRepository sheetMusicRepository;
    @Mock private TabDataRepository tabDataRepository;
    @Mock private UserWorkspaceRepository userWorkspaceRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private SheetService sheetService;

    private User user(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private SheetMusic sheet(Long id, User owner, String title) {
        SheetMusic sheet = SheetMusic.builder().owner(owner).title(title).artist("").originalKey("C")
                .instrumentType(InstrumentType.GUITAR).content("[C]가사").sheetTypes(List.of(SheetType.CHORD)).build();
        ReflectionTestUtils.setField(sheet, "id", id);
        return sheet;
    }

    private UserWorkspace item(Long id, SheetMusic sheet) {
        UserWorkspace item = UserWorkspace.builder().user(mock(User.class)).sheetMusic(sheet).build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private SheetRequest req(String title) {
        return new SheetRequest(title, " 아이유 ", "G", 92, InstrumentType.GUITAR, "[G]가사 [D]가사");
    }

    /* ---------- 악보 ---------- */

    @Test
    void 직접_쓴_악보를_만들면_내_보관함에_담긴다() {
        User me = mock(User.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(me));
        when(sheetMusicRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userWorkspaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceDetailResponse result = sheetService.createSheet(USER_ID, req("  밤편지 "));

        ArgumentCaptor<SheetMusic> captor = ArgumentCaptor.forClass(SheetMusic.class);
        verify(sheetMusicRepository).save(captor.capture());
        SheetMusic saved = captor.getValue();
        assertThat(saved.getOwner()).isSameAs(me);
        assertThat(saved.getTitle()).isEqualTo("밤편지");
        assertThat(saved.getArtist()).isEqualTo("아이유");
        assertThat(saved.getSheetTypes()).containsExactly(SheetType.CHORD);
        assertThat(result.sheet().content()).isEqualTo("[G]가사 [D]가사");
        assertThat(result.customKey()).isNull();
    }

    @Test
    void 보관함이_가득_차면_악보를_만들_수_없다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(userWorkspaceRepository.countByUserId(USER_ID)).thenReturn((long) SheetService.MAX_WORKSPACE_ITEMS);

        assertThatThrownBy(() -> sheetService.createSheet(USER_ID, req("곡")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WORKSPACE_LIMIT_EXCEEDED);
        verify(sheetMusicRepository, never()).save(any());
    }

    @Test
    void 내가_만든_악보는_고칠_수_있다() {
        SheetMusic mine = sheet(10L, user(USER_ID), "옛 제목");
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.of(mine));
        when(tabDataRepository.findAllBySheetMusicIdOrderByMeasureNumberAsc(10L)).thenReturn(List.of());

        SheetDetailResponse result = sheetService.updateSheet(USER_ID, 10L, req("새 제목"));

        assertThat(result.title()).isEqualTo("새 제목");
        assertThat(result.originalKey()).isEqualTo("G");
        assertThat(result.bpm()).isEqualTo(92);
        assertThat(result.mine()).isTrue();
    }

    @Test
    void 남이_만든_악보나_공용_악보는_고칠_수_없다() {
        SheetMusic others = sheet(10L, user(OTHER_ID), "남의 악보");
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.of(others));
        when(sheetMusicRepository.findById(11L)).thenReturn(Optional.of(sheet(11L, null, "공용 악보")));

        assertThatThrownBy(() -> sheetService.updateSheet(USER_ID, 10L, req("x")))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_EDIT_DENIED);
        assertThatThrownBy(() -> sheetService.updateSheet(USER_ID, 11L, req("x")))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_EDIT_DENIED);
    }

    @Test
    void 남의_악보는_보관함에_없으면_볼_수_없다() {
        SheetMusic others = sheet(10L, user(OTHER_ID), "남의 악보");
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.of(others));
        when(userWorkspaceRepository.existsByUserIdAndSheetMusicId(USER_ID, 10L)).thenReturn(false);

        assertThatThrownBy(() -> sheetService.getSheet(USER_ID, 10L))
                .extracting("errorCode").isEqualTo(ErrorCode.SHEET_NOT_FOUND);
    }

    @Test
    void 공용_악보는_누구나_볼_수_있다() {
        when(sheetMusicRepository.findById(11L)).thenReturn(Optional.of(sheet(11L, null, "공용 악보")));
        when(tabDataRepository.findAllBySheetMusicIdOrderByMeasureNumberAsc(11L)).thenReturn(List.of());

        SheetDetailResponse result = sheetService.getSheet(USER_ID, 11L);

        assertThat(result.title()).isEqualTo("공용 악보");
        assertThat(result.mine()).isFalse();
    }

    /* ---------- 보관함 ---------- */

    @Test
    void 보관함은_즐겨찾기_필터와_제목순_정렬을_지원한다() {
        UserWorkspace a = item(1L, sheet(10L, null, "봄날"));
        UserWorkspace b = item(2L, sheet(11L, null, "가로수 그늘 아래"));
        UserWorkspace c = item(3L, sheet(12L, null, "나비"));
        b.changeFavorite(true);
        c.changeFavorite(true);
        when(userWorkspaceRepository.findAllWithSheetByUserId(USER_ID)).thenReturn(List.of(c, b, a));

        assertThat(sheetService.getWorkspace(USER_ID, false, "recent"))
                .extracting(WorkspaceItemResponse::id).containsExactly(3L, 2L, 1L);
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
    void 남이_만든_악보는_보관함에_담을_수_없다() {
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
    void 내가_만든_악보를_빼면_악보도_지운다() {
        SheetMusic mine = sheet(10L, user(USER_ID), "내 악보");
        UserWorkspace item = item(1L, mine);
        when(userWorkspaceRepository.findWithSheetByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(item));
        when(userWorkspaceRepository.existsBySheetMusicId(10L)).thenReturn(false);

        sheetService.removeFromWorkspace(USER_ID, 1L);

        verify(userWorkspaceRepository).delete(item);
        verify(tabDataRepository).deleteAllBySheetMusicId(10L);
        verify(sheetMusicRepository).delete(mine);
    }

    @Test
    void 공용_악보를_빼면_보관함에서만_빠진다() {
        SheetMusic shared = sheet(11L, null, "공용 악보");
        UserWorkspace item = item(1L, shared);
        when(userWorkspaceRepository.findWithSheetByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(item));

        sheetService.removeFromWorkspace(USER_ID, 1L);

        verify(userWorkspaceRepository).delete(item);
        verify(sheetMusicRepository, never()).delete(any());
    }
}
