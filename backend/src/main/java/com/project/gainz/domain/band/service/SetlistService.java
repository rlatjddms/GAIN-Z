package com.project.gainz.domain.band.service;

import com.project.gainz.domain.band.dto.request.SetlistRequest;
import com.project.gainz.domain.band.dto.response.BandBoardResponse.Attachment;
import com.project.gainz.domain.band.dto.response.SetlistResponse;
import com.project.gainz.domain.band.dto.response.SetlistResponse.Item;
import com.project.gainz.domain.band.dto.response.SetlistResponse.LinkedSheet;
import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.entity.BandPostFile;
import com.project.gainz.domain.band.entity.Setlist;
import com.project.gainz.domain.band.entity.SetlistItem;
import com.project.gainz.domain.band.repository.BandMemberRepository;
import com.project.gainz.domain.band.repository.SetlistRepository;
import com.project.gainz.domain.band.service.BandBoardService.AttachmentFile;
import com.project.gainz.domain.sheet.entity.SheetMusic;
import com.project.gainz.domain.sheet.entity.UserWorkspace;
import com.project.gainz.domain.sheet.repository.SheetMusicRepository;
import com.project.gainz.domain.sheet.repository.UserWorkspaceRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SetlistService {
    public static final int MAX_SETLISTS = 50;
    public static final int MAX_ITEMS = 50;
    public static final int MAX_ATTACHMENTS = 8;

    private final BandMemberRepository members;
    private final SetlistRepository setlists;
    private final SheetMusicRepository sheets;
    private final UserWorkspaceRepository workspaces;
    private final BandAttachmentStorage storage;

    /* ---------- 셋리스트 ---------- */

    @Transactional(readOnly = true)
    public List<SetlistResponse.Setlist> list(Long userId, Long bandId) {
        requireMember(userId, bandId);
        List<Setlist> result = setlists.findAllByBandIdOrderByListOrderAscIdAsc(bandId);
        Map<Long, Long> workspaceIds = workspaceIds(userId, result);
        return result.stream().map(s -> response(s, workspaceIds)).toList();
    }

    public SetlistResponse.Setlist create(Long userId, Long bandId, SetlistRequest.Create request) {
        BandMember me = requireMember(userId, bandId);
        long count = setlists.countByBandId(bandId);
        if (count >= MAX_SETLISTS) throw new BusinessException(ErrorCode.SETLIST_LIMIT_EXCEEDED);
        Setlist setlist = setlists.save(new Setlist(me.getBand(), request.title().trim(), (int) count));
        return response(setlist, Map.of());
    }

    public SetlistResponse.Setlist update(Long userId, Long bandId, Long setlistId, SetlistRequest.Update request) {
        requireMember(userId, bandId);
        Setlist setlist = findSetlist(bandId, setlistId);
        if (request.title() != null) {
            if (request.title().isBlank()) throw new BusinessException(ErrorCode.SETLIST_TITLE_REQUIRED);
            setlist.rename(request.title().trim());
        }
        if (request.favorite() != null) setlist.changeFavorite(request.favorite());
        return response(setlist, workspaceIds(userId, List.of(setlist)));
    }

    public void delete(Long userId, Long bandId, Long setlistId) {
        requireMember(userId, bandId);
        Setlist setlist = findSetlist(bandId, setlistId);
        setlist.getItems().forEach(item -> item.getFiles().forEach(f -> cleanAfterCompletion(f.getKey(), true)));
        setlists.delete(setlist);
    }

    // 지금 셋리스트 전체를 원하는 순서로 보낸다
    public List<SetlistResponse.Setlist> reorder(Long userId, Long bandId, SetlistRequest.Order request) {
        requireMember(userId, bandId);
        List<Setlist> current = setlists.findAllByBandIdOrderByListOrderAscIdAsc(bandId);
        Map<Long, Setlist> byId = current.stream().collect(Collectors.toMap(Setlist::getId, Function.identity()));
        checkSameIds(byId.keySet(), request.ids());
        for (int i = 0; i < request.ids().size(); i++) byId.get(request.ids().get(i)).changeListOrder(i);
        List<Setlist> ordered = request.ids().stream().map(byId::get).toList();
        Map<Long, Long> workspaceIds = workspaceIds(userId, ordered);
        return ordered.stream().map(s -> response(s, workspaceIds)).toList();
    }

    /* ---------- 곡 ---------- */

    public SetlistResponse.Setlist saveItem(Long userId, Long bandId, Long setlistId, Long itemId,
                                            SetlistRequest.Item request, List<MultipartFile> uploads) {
        requireMember(userId, bandId);
        Setlist setlist = findSetlist(bandId, setlistId);
        SetlistItem item;
        if (itemId == null) {
            if (setlist.getItems().size() >= MAX_ITEMS) throw new BusinessException(ErrorCode.SETLIST_ITEM_LIMIT_EXCEEDED);
            int next = setlist.getItems().stream().mapToInt(SetlistItem::getPlayOrder).max().orElse(-1) + 1;
            item = new SetlistItem(setlist, next);
        } else {
            item = findItem(setlist, itemId);
        }

        SheetMusic sheet = linkableSheet(userId, item, request.sheetId());
        List<BandMember> assignees = assignees(bandId, request.memberIds());
        List<BandPostFile> files = files(item, request.keepAttachmentIds(), uploads);

        item.update(request.title().trim(), blankToNull(request.artist()), blankToNull(request.memo()), sheet);
        item.replaceAssignees(assignees);
        item.replaceFiles(files);
        if (itemId == null) setlist.getItems().add(item);
        setlists.saveAndFlush(setlist);
        return response(setlist, workspaceIds(userId, List.of(setlist)));
    }

    public SetlistResponse.Setlist deleteItem(Long userId, Long bandId, Long setlistId, Long itemId) {
        requireMember(userId, bandId);
        Setlist setlist = findSetlist(bandId, setlistId);
        SetlistItem item = findItem(setlist, itemId);
        item.getFiles().forEach(f -> cleanAfterCompletion(f.getKey(), true));
        setlist.getItems().remove(item);
        return response(setlist, workspaceIds(userId, List.of(setlist)));
    }

    public SetlistResponse.Setlist reorderItems(Long userId, Long bandId, Long setlistId, SetlistRequest.Order request) {
        requireMember(userId, bandId);
        Setlist setlist = findSetlist(bandId, setlistId);
        Map<Long, SetlistItem> byId = setlist.getItems().stream().collect(Collectors.toMap(SetlistItem::getId, Function.identity()));
        checkSameIds(byId.keySet(), request.ids());
        for (int i = 0; i < request.ids().size(); i++) byId.get(request.ids().get(i)).changePlayOrder(i);
        setlist.getItems().sort((a, b) -> Integer.compare(a.getPlayOrder(), b.getPlayOrder()));
        return response(setlist, workspaceIds(userId, List.of(setlist)));
    }

    @Transactional(readOnly = true)
    public AttachmentFile file(Long userId, Long bandId, Long setlistId, Long itemId, String attachmentId) {
        requireMember(userId, bandId);
        SetlistItem item = findItem(findSetlist(bandId, setlistId), itemId);
        BandPostFile file = item.getFiles().stream().filter(f -> f.getKey().equals(attachmentId)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAND_FILE_NOT_FOUND));
        return new AttachmentFile(storage.load(file.getKey()), file.getName(), file.getType());
    }

    /* ---------- 도움 ---------- */

    private BandMember requireMember(Long userId, Long bandId) {
        return members.findByBandIdAndUserId(bandId, userId)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()) && !Boolean.TRUE.equals(m.getBand().getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_BAND_MEMBER));
    }

    private Setlist findSetlist(Long bandId, Long setlistId) {
        return setlists.findByIdAndBandId(setlistId, bandId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETLIST_NOT_FOUND));
    }

    private SetlistItem findItem(Setlist setlist, Long itemId) {
        return setlist.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SETLIST_ITEM_NOT_FOUND));
    }

    private void checkSameIds(Set<Long> current, List<Long> ids) {
        if (ids.size() != current.size() || !current.equals(new HashSet<>(ids))) {
            throw new BusinessException(ErrorCode.INVALID_SETLIST_ORDER);
        }
    }

    // 이미 연결된 악보는 그대로 두고, 새로 연결하는 악보는 내가 볼 수 있는 것만 받는다
    private SheetMusic linkableSheet(Long userId, SetlistItem item, Long sheetId) {
        if (sheetId == null) return null;
        if (item.getSheetMusic() != null && item.getSheetMusic().getId().equals(sheetId)) return item.getSheetMusic();
        return sheets.findById(sheetId)
                .filter(s -> s.isShared() || s.isOwnedBy(userId) || workspaces.existsByUserIdAndSheetMusicId(userId, sheetId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SHEET_NOT_FOUND));
    }

    private List<BandMember> assignees(Long bandId, List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return List.of();
        Map<Long, BandMember> byId = members.findAllByBandIdOrderByIdAsc(bandId).stream()
                .collect(Collectors.toMap(BandMember::getId, Function.identity()));
        List<Long> unique = memberIds.stream().distinct().toList();
        if (!byId.keySet().containsAll(unique)) throw new BusinessException(ErrorCode.SETLIST_ASSIGNEE_INVALID);
        return unique.stream().map(byId::get).toList();
    }

    // 남길 첨부 + 새 파일. DB가 롤백되면 새 파일만, 커밋되면 뺀 파일만 지운다
    private List<BandPostFile> files(SetlistItem item, List<String> keepIds, List<MultipartFile> uploads) {
        List<String> keep = keepIds == null ? List.of() : keepIds;
        List<MultipartFile> added = uploads == null ? List.of() : uploads.stream().filter(f -> !f.isEmpty()).toList();
        var existing = item.getFiles().stream().map(BandPostFile::getKey).collect(Collectors.toSet());
        if (new HashSet<>(keep).size() != keep.size() || !existing.containsAll(keep)) {
            throw new BusinessException(ErrorCode.BAND_FILE_SELECTION_INVALID);
        }
        if (keep.size() + added.size() > MAX_ATTACHMENTS) throw new BusinessException(ErrorCode.BAND_FILE_LIMIT);
        var next = new ArrayList<BandPostFile>();
        for (BandPostFile file : item.getFiles()) {
            if (keep.contains(file.getKey())) next.add(file);
            else cleanAfterCompletion(file.getKey(), true);
        }
        for (MultipartFile file : added) {
            var stored = storage.store(file);
            cleanAfterCompletion(stored.key(), false);
            next.add(new BandPostFile(stored.key(), stored.name(), stored.type()));
        }
        return next;
    }

    // 연결 악보마다 보는 사람의 보관함 항목 id
    private Map<Long, Long> workspaceIds(Long userId, List<Setlist> lists) {
        List<Long> sheetIds = lists.stream().flatMap(s -> s.getItems().stream())
                .map(SetlistItem::getSheetMusic).filter(Objects::nonNull).map(SheetMusic::getId).distinct().toList();
        if (sheetIds.isEmpty()) return Map.of();
        return workspaces.findAllByUserIdAndSheetIds(userId, sheetIds).stream()
                .collect(Collectors.toMap(w -> w.getSheetMusic().getId(), UserWorkspace::getId, (a, b) -> a));
    }

    private SetlistResponse.Setlist response(Setlist setlist, Map<Long, Long> workspaceIds) {
        String base = "/api/v1/bands/" + setlist.getBand().getId() + "/setlists/" + setlist.getId() + "/items/";
        List<Item> items = setlist.getItems().stream().map(item -> {
            SheetMusic sheet = item.getSheetMusic();
            LinkedSheet linked = sheet == null ? null
                    : new LinkedSheet(sheet.getId(), sheet.getTitle(), sheet.getArtist(), workspaceIds.get(sheet.getId()));
            List<Attachment> attachments = item.getFiles().stream()
                    .map(f -> new Attachment(f.getKey(), f.getName(), f.getType(), base + item.getId() + "/attachments/" + f.getKey()))
                    .toList();
            return new Item(item.getId(), item.getCustomTitle(), item.getCustomArtist(), item.getDescription(), linked,
                    item.getAssignees().stream().map(BandMember::getId).toList(), attachments);
        }).toList();
        return new SetlistResponse.Setlist(setlist.getId(), setlist.getTitle(), setlist.isFavorite(), items);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void cleanAfterCompletion(String key, boolean onCommit) {
        if (key == null || !TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if ((status == STATUS_COMMITTED) == onCommit) storage.delete(key);
            }
        });
    }
}
