package com.project.gainz.domain.sheet.service;

import com.project.gainz.domain.sheet.dto.request.SheetRequest;
import com.project.gainz.domain.sheet.dto.request.WorkspaceUpdateRequest;
import com.project.gainz.domain.sheet.dto.response.SheetDetailResponse;
import com.project.gainz.domain.sheet.dto.response.SheetSummaryResponse;
import com.project.gainz.domain.sheet.dto.response.WorkspaceDetailResponse;
import com.project.gainz.domain.sheet.dto.response.WorkspaceItemResponse;
import com.project.gainz.domain.band.repository.SetlistItemRepository;
import com.project.gainz.domain.sheet.entity.SheetMusic;
import com.project.gainz.domain.sheet.enums.SheetType;
import com.project.gainz.domain.sheet.entity.SheetPage;
import com.project.gainz.domain.sheet.entity.UserWorkspace;
import com.project.gainz.domain.sheet.repository.SheetMusicRepository;
import com.project.gainz.domain.sheet.repository.SheetPageRepository;
import com.project.gainz.domain.sheet.repository.TabDataRepository;
import com.project.gainz.domain.sheet.repository.UserWorkspaceRepository;
import com.project.gainz.domain.sheet.service.SheetFileStorage.StoredFile;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 악보와 내 악보 보관함.
// 악보는 파일(이미지·PDF 여러 장)로 올리고, 올리면 바로 보관함에 담긴다.
// 보관함에서 빼면 (내가 올린 악보이고 다른 사람이 담지 않았으면) 악보와 파일도 지운다
@Service
@RequiredArgsConstructor
@Transactional
public class SheetService {

    public static final int MAX_WORKSPACE_ITEMS = 200;
    public static final int MAX_PAGES = 20;

    // 장 파일 내려받기용
    public record PageFile(Resource resource, String contentType, String fileName) {
    }

    private final SheetMusicRepository sheetMusicRepository;
    private final SheetPageRepository sheetPageRepository;
    private final TabDataRepository tabDataRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final UserRepository userRepository;
    private final SheetFileStorage fileStorage;
    private final SetlistItemRepository setlistItemRepository;

    /* ---------- 악보 ---------- */

    // 누구나 담을 수 있는 공용 악보 목록
    @Transactional(readOnly = true)
    public List<SheetSummaryResponse> getSharedSheets() {
        return sheetMusicRepository.findAllByOwnerIsNullOrderByTitleAsc().stream()
                .map(SheetSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SheetDetailResponse getSheet(Long userId, Long sheetId) {
        return detail(findViewableSheet(userId, sheetId), userId);
    }

    // 악보 파일을 올려서 악보를 만들고 보관함에 담는다 (고른 순서가 장 순서)
    public WorkspaceDetailResponse uploadSheet(Long userId, SheetRequest request, List<MultipartFile> files) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        List<MultipartFile> uploads = nonEmpty(files);
        if (uploads.isEmpty()) {
            throw new BusinessException(ErrorCode.SHEET_FILE_REQUIRED);
        }
        if (uploads.size() > MAX_PAGES) {
            throw new BusinessException(ErrorCode.SHEET_PAGE_LIMIT_EXCEEDED);
        }
        checkWorkspaceLimit(userId);

        SheetMusic sheet = sheetMusicRepository.save(SheetMusic.builder()
                .owner(user)
                .title(request.title().trim())
                .artist(request.artistOrEmpty())
                .originalKey(blankToNull(request.originalKey()))
                .bpm(request.bpm())
                .instrumentType(request.instrumentType())
                .sheetTypes(List.of(request.sheetType()))
                .build());
        List<SheetPage> pages = savePages(sheet, uploads, 1);
        UserWorkspace item = userWorkspaceRepository.save(UserWorkspace.builder().user(user).sheetMusic(sheet).build());
        return WorkspaceDetailResponse.of(item, pages, List.of(), userId);
    }

    // 코드 붙여넣기로 코드 악보 만들기 (#157): 파일 없이 코드 글(content)만. 종류는 늘 CHORD, 만들면 바로 내 보관함에 담김
    public WorkspaceDetailResponse createChordSheet(Long userId, SheetRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String content = requireContent(request.content());
        checkWorkspaceLimit(userId);

        SheetMusic sheet = sheetMusicRepository.save(SheetMusic.builder()
                .owner(user)
                .title(request.title().trim())
                .artist(request.artistOrEmpty())
                .originalKey(blankToNull(request.originalKey()))
                .bpm(request.bpm())
                .content(content)
                .instrumentType(request.instrumentType())
                .sheetTypes(List.of(SheetType.CHORD))
                .build());
        UserWorkspace item = userWorkspaceRepository.save(UserWorkspace.builder().user(user).sheetMusic(sheet).build());
        return WorkspaceDetailResponse.of(item, List.of(), List.of(), userId);
    }

    // 내가 올린 악보의 곡 정보를 고친다. content를 보내면 코드 악보 글도 바꾼다 (보내지 않으면 그대로)
    public SheetDetailResponse updateSheet(Long userId, Long sheetId, SheetRequest request) {
        SheetMusic sheet = findOwnSheet(userId, sheetId);
        sheet.updateInfo(request.title().trim(), request.artistOrEmpty(), blankToNull(request.originalKey()), request.bpm(),
                request.instrumentType(), request.sheetType());
        if (request.content() != null) {
            sheet.updateContent(requireContent(request.content()));
        }
        return detail(sheet, userId);
    }

    private static String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.SHEET_CONTENT_REQUIRED);
        }
        return content.strip();
    }

    // 장을 뒤에 더 붙인다
    public SheetDetailResponse addPages(Long userId, Long sheetId, List<MultipartFile> files) {
        SheetMusic sheet = findOwnSheet(userId, sheetId);
        List<MultipartFile> uploads = nonEmpty(files);
        if (uploads.isEmpty()) {
            throw new BusinessException(ErrorCode.SHEET_FILE_REQUIRED);
        }
        List<SheetPage> current = sheetPageRepository.findAllBySheetMusicIdOrderByPageNumberAsc(sheetId);
        if (current.size() + uploads.size() > MAX_PAGES) {
            throw new BusinessException(ErrorCode.SHEET_PAGE_LIMIT_EXCEEDED);
        }
        int next = current.isEmpty() ? 1 : current.get(current.size() - 1).getPageNumber() + 1;
        savePages(sheet, uploads, next);
        return detail(sheet, userId);
    }

    // 장 하나를 지우고 뒤 장 번호를 당긴다 (마지막 한 장은 못 지움)
    public SheetDetailResponse deletePage(Long userId, Long sheetId, Long pageId) {
        SheetMusic sheet = findOwnSheet(userId, sheetId);
        List<SheetPage> pages = sheetPageRepository.findAllBySheetMusicIdOrderByPageNumberAsc(sheetId);
        SheetPage target = pages.stream().filter(p -> p.getId().equals(pageId)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SHEET_PAGE_NOT_FOUND));
        if (pages.size() <= 1) {
            throw new BusinessException(ErrorCode.SHEET_LAST_PAGE);
        }
        sheetPageRepository.delete(target);
        List<SheetPage> rest = pages.stream().filter(p -> p != target).toList();
        for (int i = 0; i < rest.size(); i++) {
            rest.get(i).changePageNumber(i + 1);
        }
        deleteFilesAfterCommit(List.of(target.getStoredName()));
        return SheetDetailResponse.of(sheet, rest, tabDataRepository.findAllBySheetMusicIdOrderByMeasureNumberAsc(sheetId), userId);
    }

    // 장 파일 (볼 수 있는 악보만)
    @Transactional(readOnly = true)
    public PageFile getPageFile(Long userId, Long sheetId, Long pageId) {
        findViewableSheet(userId, sheetId);
        SheetPage page = sheetPageRepository.findByIdAndSheetMusicId(pageId, sheetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHEET_PAGE_NOT_FOUND));
        return new PageFile(fileStorage.load(page.getStoredName()), page.getContentType(), page.getFileName());
    }

    /* ---------- 보관함 ---------- */

    // 내 보관함 목록. sort: recent(담은 순, 기본) / title(제목 순)
    @Transactional(readOnly = true)
    public List<WorkspaceItemResponse> getWorkspace(Long userId, boolean favoriteOnly, String sort) {
        Comparator<WorkspaceItemResponse> order = "title".equals(sort)
                ? Comparator.comparing(WorkspaceItemResponse::title, String.CASE_INSENSITIVE_ORDER)
                : Comparator.comparing(WorkspaceItemResponse::id).reversed();
        List<UserWorkspace> items = userWorkspaceRepository.findAllWithSheetByUserId(userId).stream()
                .filter(item -> !favoriteOnly || Boolean.TRUE.equals(item.getIsFavorite()))
                .toList();
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, List<SheetPage>> pagesBySheet = sheetPageRepository
                .findAllBySheetMusicIdInOrderByPageNumberAsc(items.stream().map(i -> i.getSheetMusic().getId()).toList())
                .stream()
                .collect(Collectors.groupingBy(p -> p.getSheetMusic().getId()));
        return items.stream()
                .map(item -> WorkspaceItemResponse.of(item, pagesBySheet.getOrDefault(item.getSheetMusic().getId(), List.of()), userId))
                .sorted(order)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceDetailResponse getWorkspaceItem(Long userId, Long itemId) {
        return workspaceDetail(findOwnItem(userId, itemId), userId);
    }

    // 이미 있는 악보(공용 악보, 내 밴드 셋리스트에 연결된 악보 등)를 보관함에 담는다
    public WorkspaceItemResponse addToWorkspace(Long userId, Long sheetId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        SheetMusic sheet = sheetMusicRepository.findById(sheetId)
                .filter(s -> s.isShared() || s.isOwnedBy(userId) || setlistItemRepository.existsLinkedForMember(sheetId, userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SHEET_NOT_FOUND));
        if (userWorkspaceRepository.existsByUserIdAndSheetMusicId(userId, sheetId)) {
            throw new BusinessException(ErrorCode.SHEET_ALREADY_IN_WORKSPACE);
        }
        checkWorkspaceLimit(userId);
        UserWorkspace item = userWorkspaceRepository.save(UserWorkspace.builder().user(user).sheetMusic(sheet).build());
        return WorkspaceItemResponse.of(item, sheetPageRepository.findAllBySheetMusicIdOrderByPageNumberAsc(sheetId), userId);
    }

    // 메모·연습 키·즐겨찾기 중 보낸 것만 바꾼다
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
        return workspaceDetail(item, userId);
    }

    // 보관함에서 뺀다. 내가 올린 악보이고 다른 사람 보관함·셋리스트에도 없으면 악보와 파일까지 지운다
    public void removeFromWorkspace(Long userId, Long itemId) {
        UserWorkspace item = findOwnItem(userId, itemId);
        SheetMusic sheet = item.getSheetMusic();
        userWorkspaceRepository.delete(item);
        userWorkspaceRepository.flush();
        if (sheet.isOwnedBy(userId) && !userWorkspaceRepository.existsBySheetMusicId(sheet.getId())
                && !setlistItemRepository.existsBySheetMusicId(sheet.getId())) {
            List<SheetPage> pages = sheetPageRepository.findAllBySheetMusicIdOrderByPageNumberAsc(sheet.getId());
            sheetPageRepository.deleteAll(pages);
            tabDataRepository.deleteAllBySheetMusicId(sheet.getId());
            sheetMusicRepository.delete(sheet);
            deleteFilesAfterCommit(pages.stream().map(SheetPage::getStoredName).toList());
        }
    }

    /* ---------- 도움 ---------- */

    // 공용 악보, 내가 올린 악보, 내 보관함에 있는 악보, 내 밴드 셋리스트에 연결된 악보만 볼 수 있다 (그 밖에는 없는 악보로 본다)
    private SheetMusic findViewableSheet(Long userId, Long sheetId) {
        return sheetMusicRepository.findById(sheetId)
                .filter(s -> s.isShared() || s.isOwnedBy(userId)
                        || userWorkspaceRepository.existsByUserIdAndSheetMusicId(userId, sheetId)
                        || setlistItemRepository.existsLinkedForMember(sheetId, userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SHEET_NOT_FOUND));
    }

    private SheetMusic findOwnSheet(Long userId, Long sheetId) {
        SheetMusic sheet = sheetMusicRepository.findById(sheetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHEET_NOT_FOUND));
        if (!sheet.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.SHEET_EDIT_DENIED);
        }
        return sheet;
    }

    private UserWorkspace findOwnItem(Long userId, Long itemId) {
        return userWorkspaceRepository.findWithSheetByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_ITEM_NOT_FOUND));
    }

    private SheetDetailResponse detail(SheetMusic sheet, Long userId) {
        return SheetDetailResponse.of(sheet,
                sheetPageRepository.findAllBySheetMusicIdOrderByPageNumberAsc(sheet.getId()),
                tabDataRepository.findAllBySheetMusicIdOrderByMeasureNumberAsc(sheet.getId()), userId);
    }

    private WorkspaceDetailResponse workspaceDetail(UserWorkspace item, Long userId) {
        Long sheetId = item.getSheetMusic().getId();
        return WorkspaceDetailResponse.of(item,
                sheetPageRepository.findAllBySheetMusicIdOrderByPageNumberAsc(sheetId),
                tabDataRepository.findAllBySheetMusicIdOrderByMeasureNumberAsc(sheetId), userId);
    }

    private void checkWorkspaceLimit(Long userId) {
        if (userWorkspaceRepository.countByUserId(userId) >= MAX_WORKSPACE_ITEMS) {
            throw new BusinessException(ErrorCode.WORKSPACE_LIMIT_EXCEEDED);
        }
    }

    // 파일을 저장하고 장으로 만든다. 중간에 실패하거나 나중에 DB가 롤백되면 저장한 파일을 지운다
    private List<SheetPage> savePages(SheetMusic sheet, List<MultipartFile> files, int firstNumber) {
        List<String> stored = new ArrayList<>();
        try {
            List<SheetPage> pages = new ArrayList<>();
            for (MultipartFile file : files) {
                StoredFile saved = fileStorage.store(file);
                stored.add(saved.storedName());
                pages.add(SheetPage.builder()
                        .sheetMusic(sheet)
                        .pageNumber(firstNumber + pages.size())
                        .storedName(saved.storedName())
                        .contentType(saved.contentType())
                        .fileName(saved.fileName())
                        .size(saved.size())
                        .build());
            }
            List<SheetPage> result = sheetPageRepository.saveAll(pages);
            deleteFilesOnRollback(stored);
            return result;
        } catch (RuntimeException e) {
            stored.forEach(fileStorage::delete);
            throw e;
        }
    }

    private void deleteFilesOnRollback(List<String> storedNames) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) storedNames.forEach(fileStorage::delete);
            }
        });
    }

    // DB에서 지운 게 확정되면 파일도 지운다
    private void deleteFilesAfterCommit(List<String> storedNames) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            storedNames.forEach(fileStorage::delete);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storedNames.forEach(fileStorage::delete);
            }
        });
    }

    private static List<MultipartFile> nonEmpty(List<MultipartFile> files) {
        return files == null ? List.of() : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
