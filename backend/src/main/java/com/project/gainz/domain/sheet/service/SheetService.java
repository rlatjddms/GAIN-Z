package com.project.gainz.domain.sheet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.gainz.domain.sheet.dto.request.SheetRequest;
import com.project.gainz.domain.sheet.dto.request.WorkspaceUpdateRequest;
import com.project.gainz.domain.sheet.dto.response.AnnotationResponse;
import com.project.gainz.domain.sheet.dto.response.SheetDetailResponse;
import com.project.gainz.domain.sheet.dto.response.WorkspaceDetailResponse;
import com.project.gainz.domain.sheet.dto.response.WorkspaceItemResponse;
import com.project.gainz.domain.band.repository.SetlistItemRepository;
import com.project.gainz.domain.sheet.entity.SheetAnnotation;
import com.project.gainz.domain.sheet.entity.SheetMusic;
import com.project.gainz.domain.sheet.enums.SheetType;
import com.project.gainz.domain.sheet.entity.SheetPage;
import com.project.gainz.domain.sheet.entity.UserWorkspace;
import com.project.gainz.domain.sheet.repository.SheetAnnotationRepository;
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
import java.util.HashSet;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final SheetAnnotationRepository sheetAnnotationRepository;

    /* ---------- 악보 ---------- */

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

    // 글 악보 만들기: 파일 없이 글(content)만. 만들면 바로 내 보관함에 담김
    // 코드 붙여넣기(#157)는 CHORD, 타브 붙여넣기·직접 쓰기(#166)는 TAB. 다른 종류는 코드 악보로 본다
    public WorkspaceDetailResponse createTextSheet(Long userId, SheetRequest request) {
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
                .sheetTypes(List.of(textSheetType(request.sheetType())))
                .build());
        UserWorkspace item = userWorkspaceRepository.save(UserWorkspace.builder().user(user).sheetMusic(sheet).build());
        return WorkspaceDetailResponse.of(item, List.of(), List.of(), userId);
    }

    // 내가 올린 악보의 곡 정보를 고친다. content를 보내면 글 악보(코드·타브) 글도 바꾼다 (보내지 않으면 그대로)
    public SheetDetailResponse updateSheet(Long userId, Long sheetId, SheetRequest request) {
        SheetMusic sheet = findOwnSheet(userId, sheetId);
        sheet.updateInfo(request.title().trim(), request.artistOrEmpty(), blankToNull(request.originalKey()), request.bpm(),
                request.instrumentType(), request.sheetType());
        if (request.content() != null) {
            sheet.updateContent(requireContent(request.content()));
        }
        return detail(sheet, userId);
    }

    private static SheetType textSheetType(SheetType requested) {
        return requested == SheetType.TAB ? SheetType.TAB : SheetType.CHORD;
    }

    // 앞뒤 빈 줄만 지운다. 타브는 칸 위치가 박이라서 첫 줄 앞 공백(코드 줄 맞춤)을 남긴다
    private static String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.SHEET_CONTENT_REQUIRED);
        }
        return content.replaceFirst("^(?:[ \\t]*\\r?\\n)+", "").stripTrailing();
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

    // 장 순서를 바꾼다 (#191). 지금 있는 장 ID가 빠짐없이 한 번씩 와야 한다
    public SheetDetailResponse reorderPages(Long userId, Long sheetId, List<Long> pageIds) {
        SheetMusic sheet = findOwnSheet(userId, sheetId);
        List<SheetPage> pages = sheetPageRepository.findAllBySheetMusicIdOrderByPageNumberAsc(sheetId);
        Map<Long, SheetPage> byId = pages.stream().collect(Collectors.toMap(SheetPage::getId, p -> p));
        if (pageIds.size() != pages.size() || !byId.keySet().equals(new HashSet<>(pageIds))) {
            throw new BusinessException(ErrorCode.SHEET_PAGE_ORDER_INVALID);
        }
        List<SheetPage> ordered = pageIds.stream().map(byId::get).toList();
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).changePageNumber(i + 1);
        }
        return SheetDetailResponse.of(sheet, ordered, tabDataRepository.findAllBySheetMusicIdOrderByMeasureNumberAsc(sheetId), userId);
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
        sheetAnnotationRepository.deleteAllBySheetPageIdIn(List.of(target.getId()));
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

    /* ---------- 악보 위 필기 (#167) ---------- */

    public static final int MAX_ANNOTATION_LENGTH = 500_000;
    public static final int MAX_PDF_PAGE = 1000;
    private static final ObjectMapper JSON = new ObjectMapper();

    // 이 악보의 내 필기 (볼 수 있는 악보만)
    @Transactional(readOnly = true)
    public List<AnnotationResponse> getAnnotations(Long userId, Long sheetId) {
        findViewableSheet(userId, sheetId);
        return sheetAnnotationRepository.findAllByUserIdAndSheetPageSheetMusicId(userId, sheetId).stream()
                .map(AnnotationResponse::from)
                .toList();
    }

    // 장(PDF는 쪽) 하나의 내 필기를 저장한다. 획이 없으면 지우고 null
    public AnnotationResponse saveAnnotation(Long userId, Long sheetId, Long pageId, int pdfPage, String data) {
        findViewableSheet(userId, sheetId);
        SheetPage page = sheetPageRepository.findByIdAndSheetMusicId(pageId, sheetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHEET_PAGE_NOT_FOUND));
        if (pdfPage < 1 || pdfPage > MAX_PDF_PAGE || (page.isImage() && pdfPage != 1)) {
            throw new BusinessException(ErrorCode.INVALID_ANNOTATION);
        }
        boolean empty = strokeCount(data) == 0;

        Optional<SheetAnnotation> saved = sheetAnnotationRepository.findByUserIdAndSheetPageIdAndPdfPage(userId, pageId, pdfPage);
        if (empty) {
            saved.ifPresent(sheetAnnotationRepository::delete);
            return null;
        }
        if (saved.isPresent()) {
            saved.get().updateData(data);
            return AnnotationResponse.from(saved.get());
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return AnnotationResponse.from(sheetAnnotationRepository.save(SheetAnnotation.builder()
                .user(user).sheetPage(page).pdfPage(pdfPage).data(data).build()));
    }

    // 필기 JSON의 획 수. { strokes: [...] } 모양이 아니면 거절한다 (그리는 건 프론트가 하니 획 안쪽은 보지 않음)
    private static int strokeCount(String data) {
        if (data.length() > MAX_ANNOTATION_LENGTH) {
            throw new BusinessException(ErrorCode.ANNOTATION_TOO_LARGE);
        }
        try {
            JsonNode strokes = JSON.readTree(data).path("strokes");
            if (!strokes.isArray()) {
                throw new BusinessException(ErrorCode.INVALID_ANNOTATION);
            }
            return strokes.size();
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_ANNOTATION);
        }
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
        if (request.customChords() != null) {
            item.changeCustomChords(request.customChords());
        }
        return workspaceDetail(item, userId);
    }

    // 내 악보로 복사 (#191): 남이 올린(또는 공용) 악보의 사본을 내 것으로 만들고, 이 보관함 항목을 사본으로 바꾼다.
    // 장 파일까지 새로 저장하고, 내 메모·연습 키·즐겨찾기는 항목에 그대로, 내 필기는 사본의 장으로 옮긴다. 원본은 그대로
    public WorkspaceDetailResponse copyToMine(Long userId, Long itemId) {
        UserWorkspace item = findOwnItem(userId, itemId);
        SheetMusic source = item.getSheetMusic();
        if (source.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.SHEET_ALREADY_MINE);
        }
        SheetMusic copy = sheetMusicRepository.save(SheetMusic.builder()
                .owner(item.getUser())
                .title(source.getTitle())
                .artist(source.getArtist())
                .originalAudioUrl(source.getOriginalAudioUrl())
                .bpm(source.getBpm())
                .originalKey(source.getOriginalKey())
                .content(source.getContent())
                .instrumentType(source.getInstrumentType())
                .sheetTypes(new ArrayList<>(source.getSheetTypes()))
                .build());
        tabDataRepository.saveAll(tabDataRepository.findAllBySheetMusicIdOrderByMeasureNumberAsc(source.getId()).stream()
                .map(tab -> tab.copyTo(copy)).toList());

        Map<Long, SheetPage> copiedPages = new HashMap<>();
        List<String> stored = new ArrayList<>();
        try {
            for (SheetPage page : sheetPageRepository.findAllBySheetMusicIdOrderByPageNumberAsc(source.getId())) {
                String copyName = fileStorage.copy(page.getStoredName());
                stored.add(copyName);
                copiedPages.put(page.getId(), sheetPageRepository.save(SheetPage.builder()
                        .sheetMusic(copy)
                        .pageNumber(page.getPageNumber())
                        .storedName(copyName)
                        .contentType(page.getContentType())
                        .fileName(page.getFileName())
                        .size(page.getSize())
                        .build()));
            }
        } catch (RuntimeException e) {
            stored.forEach(fileStorage::delete);
            throw e;
        }
        deleteFilesOnRollback(stored);

        List<SheetAnnotation> mine = sheetAnnotationRepository.findAllByUserIdAndSheetPageSheetMusicId(userId, source.getId());
        sheetAnnotationRepository.saveAll(mine.stream()
                .filter(a -> copiedPages.containsKey(a.getSheetPage().getId()))
                .map(a -> SheetAnnotation.builder().user(a.getUser()).sheetPage(copiedPages.get(a.getSheetPage().getId()))
                        .pdfPage(a.getPdfPage()).data(a.getData()).build())
                .toList());
        sheetAnnotationRepository.deleteAll(mine);

        item.changeSheet(copy);
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
            sheetAnnotationRepository.deleteAllBySheetPageIdIn(pages.stream().map(SheetPage::getId).toList());
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
