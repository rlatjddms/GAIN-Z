package com.project.gainz.domain.sheet.service;

import com.project.gainz.domain.sheet.dto.request.SheetRequest;
import com.project.gainz.domain.sheet.dto.request.WorkspaceUpdateRequest;
import com.project.gainz.domain.sheet.dto.response.SheetDetailResponse;
import com.project.gainz.domain.sheet.dto.response.SheetSummaryResponse;
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
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

// 악보와 내 악보 보관함.
// 직접 쓴 악보는 만들면 바로 보관함에 담기고, 보관함에서 빼면 (다른 사람이 담지 않았으면) 악보도 지운다
@Service
@RequiredArgsConstructor
@Transactional
public class SheetService {

    public static final int MAX_WORKSPACE_ITEMS = 200;

    private final SheetMusicRepository sheetMusicRepository;
    private final TabDataRepository tabDataRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final UserRepository userRepository;

    /* ---------- 악보 ---------- */

    // 누구나 담을 수 있는 공용 악보 목록
    @Transactional(readOnly = true)
    public List<SheetSummaryResponse> getSharedSheets() {
        return sheetMusicRepository.findAllByOwnerIsNullOrderByTitleAsc().stream()
                .map(SheetSummaryResponse::from)
                .toList();
    }

    // 공용 악보, 내가 만든 악보, 내 보관함에 있는 악보만 볼 수 있다 (그 밖에는 없는 악보로 본다)
    @Transactional(readOnly = true)
    public SheetDetailResponse getSheet(Long userId, Long sheetId) {
        SheetMusic sheet = sheetMusicRepository.findById(sheetId)
                .filter(s -> s.isShared() || s.isOwnedBy(userId)
                        || userWorkspaceRepository.existsByUserIdAndSheetMusicId(userId, sheetId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SHEET_NOT_FOUND));
        return SheetDetailResponse.of(sheet, tabDataRepository.findAllBySheetMusicIdOrderByMeasureNumberAsc(sheetId), userId);
    }

    // 코드 악보를 직접 써서 만들고 보관함에 담는다
    public WorkspaceDetailResponse createSheet(Long userId, SheetRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        checkWorkspaceLimit(userId);
        SheetMusic sheet = sheetMusicRepository.save(SheetMusic.builder()
                .owner(user)
                .title(request.title().trim())
                .artist(request.artistOrEmpty())
                .originalKey(request.originalKey())
                .bpm(request.bpm())
                .instrumentType(request.instrumentType())
                .content(request.content())
                .sheetTypes(List.of(SheetType.CHORD))
                .build());
        UserWorkspace item = userWorkspaceRepository.save(UserWorkspace.builder().user(user).sheetMusic(sheet).build());
        return WorkspaceDetailResponse.of(item, List.of(), userId);
    }

    // 직접 만든 악보만 고칠 수 있다
    public SheetDetailResponse updateSheet(Long userId, Long sheetId, SheetRequest request) {
        SheetMusic sheet = sheetMusicRepository.findById(sheetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHEET_NOT_FOUND));
        if (!sheet.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.SHEET_EDIT_DENIED);
        }
        sheet.updateInfo(request.title().trim(), request.artistOrEmpty(), request.originalKey(), request.bpm(),
                request.instrumentType(), request.content());
        return SheetDetailResponse.of(sheet, tabDataRepository.findAllBySheetMusicIdOrderByMeasureNumberAsc(sheetId), userId);
    }

    /* ---------- 보관함 ---------- */

    // 내 보관함 목록. sort: recent(담은 순, 기본) / title(제목 순)
    @Transactional(readOnly = true)
    public List<WorkspaceItemResponse> getWorkspace(Long userId, boolean favoriteOnly, String sort) {
        Comparator<WorkspaceItemResponse> order = "title".equals(sort)
                ? Comparator.comparing(WorkspaceItemResponse::title, String.CASE_INSENSITIVE_ORDER)
                : Comparator.comparing(WorkspaceItemResponse::id).reversed();
        return userWorkspaceRepository.findAllWithSheetByUserId(userId).stream()
                .filter(item -> !favoriteOnly || Boolean.TRUE.equals(item.getIsFavorite()))
                .map(item -> WorkspaceItemResponse.of(item, userId))
                .sorted(order)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceDetailResponse getWorkspaceItem(Long userId, Long itemId) {
        UserWorkspace item = findOwnItem(userId, itemId);
        return WorkspaceDetailResponse.of(item,
                tabDataRepository.findAllBySheetMusicIdOrderByMeasureNumberAsc(item.getSheetMusic().getId()), userId);
    }

    // 이미 있는 악보(공용 악보 등)를 보관함에 담는다
    public WorkspaceItemResponse addToWorkspace(Long userId, Long sheetId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        SheetMusic sheet = sheetMusicRepository.findById(sheetId)
                .filter(s -> s.isShared() || s.isOwnedBy(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SHEET_NOT_FOUND));
        if (userWorkspaceRepository.existsByUserIdAndSheetMusicId(userId, sheetId)) {
            throw new BusinessException(ErrorCode.SHEET_ALREADY_IN_WORKSPACE);
        }
        checkWorkspaceLimit(userId);
        UserWorkspace item = userWorkspaceRepository.save(UserWorkspace.builder().user(user).sheetMusic(sheet).build());
        return WorkspaceItemResponse.of(item, userId);
    }

    // 메모·바꾼 키·즐겨찾기 중 보낸 것만 바꾼다
    public WorkspaceDetailResponse updateWorkspaceItem(Long userId, Long itemId, WorkspaceUpdateRequest request) {
        UserWorkspace item = findOwnItem(userId, itemId);
        if (request.customKey() != null) {
            item.changeCustomKey(request.customKey());
        }
        if (request.personalMemo() != null) {
            item.changePersonalMemo(request.personalMemo());
        }
        if (request.isFavorite() != null) {
            item.changeFavorite(request.isFavorite());
        }
        return WorkspaceDetailResponse.of(item,
                tabDataRepository.findAllBySheetMusicIdOrderByMeasureNumberAsc(item.getSheetMusic().getId()), userId);
    }

    // 보관함에서 뺀다. 내가 만든 악보이고 다른 사람 보관함에도 없으면 악보까지 지운다
    public void removeFromWorkspace(Long userId, Long itemId) {
        UserWorkspace item = findOwnItem(userId, itemId);
        SheetMusic sheet = item.getSheetMusic();
        userWorkspaceRepository.delete(item);
        userWorkspaceRepository.flush();
        if (sheet.isOwnedBy(userId) && !userWorkspaceRepository.existsBySheetMusicId(sheet.getId())) {
            tabDataRepository.deleteAllBySheetMusicId(sheet.getId());
            sheetMusicRepository.delete(sheet);
        }
    }

    private UserWorkspace findOwnItem(Long userId, Long itemId) {
        return userWorkspaceRepository.findWithSheetByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_ITEM_NOT_FOUND));
    }

    private void checkWorkspaceLimit(Long userId) {
        if (userWorkspaceRepository.countByUserId(userId) >= MAX_WORKSPACE_ITEMS) {
            throw new BusinessException(ErrorCode.WORKSPACE_LIMIT_EXCEEDED);
        }
    }
}
