package com.project.gainz.domain.band.service;

import com.project.gainz.domain.band.dto.request.JamRequest;
import com.project.gainz.domain.band.dto.response.JamResponse;
import com.project.gainz.domain.band.dto.response.JamResponse.Song;
import com.project.gainz.domain.band.dto.response.JamResponse.Summary;
import com.project.gainz.domain.band.dto.response.JamResponse.Track;
import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.entity.BandPostFile;
import com.project.gainz.domain.band.entity.Jam;
import com.project.gainz.domain.band.entity.JamTrack;
import com.project.gainz.domain.band.repository.BandMemberRepository;
import com.project.gainz.domain.band.repository.JamRepository;
import com.project.gainz.domain.band.repository.JamTrackRepository;
import com.project.gainz.domain.band.repository.SetlistItemRepository;
import com.project.gainz.domain.band.service.BandBoardService.AttachmentFile;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.notification.enums.NotificationType;
import com.project.gainz.domain.notification.service.NotificationService;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 레이어 잼: 잼(곡)과 멤버가 녹음해 올린 트랙. 병합은 브라우저에서 한다 (docs/layer-jam-research.md)
@Service
@RequiredArgsConstructor
@Transactional
public class JamService {
    public static final int MAX_JAMS = 50;
    public static final int MAX_TRACKS = 8;
    private static final int DEFAULT_VOLUME = 100;

    private final BandMemberRepository members;
    private final JamRepository jams;
    private final JamTrackRepository tracks;
    private final SetlistItemRepository setlistItems;
    private final CharacterRepository characters;
    private final BandAttachmentStorage storage;
    private final NotificationService notificationService;

    /* ---------- 잼 ---------- */

    @Transactional(readOnly = true)
    public List<Summary> list(Long userId, Long bandId) {
        requireMember(userId, bandId);
        List<Jam> result = jams.findAllByBandIdOrderByIdDesc(bandId);
        Map<Long, String> names = nicknames(result.stream().map(jam -> jam.getCreator().getId()).toList());
        return result.stream().map(jam -> new Summary(jam.getId(), jam.getTitle(), jam.getBpm(), song(jam),
                jam.getTracks().size(), names.get(jam.getCreator().getId()), jam.getUpdatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public JamResponse.Jam get(Long userId, Long bandId, Long jamId) {
        BandMember me = requireMember(userId, bandId);
        return response(findJam(bandId, jamId), me);
    }

    public JamResponse.Jam create(Long userId, Long bandId, JamRequest.Save request) {
        BandMember me = requireMember(userId, bandId);
        if (jams.countByBandId(bandId) >= MAX_JAMS) throw new BusinessException(ErrorCode.JAM_LIMIT_EXCEEDED);
        Jam jam = new Jam(me.getBand(), me.getUser(), request.title().trim(), request.bpm());
        linkSong(jam, bandId, request.setlistItemId());
        Jam saved = jams.save(jam);
        List<User> others = members.findAllByBandIdOrderByIdAsc(bandId).stream()
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .map(BandMember::getUser)
                .toList();
        notificationService.notifyAll(others, userId, NotificationType.JAM_NEW, me.getBand().getName(),
                notificationService.nicknameOf(me.getUser()) + " 님이 새 잼을 만들었어요: " + saved.getTitle(), jamUrl(saved));
        return response(saved, me);
    }

    private static String jamUrl(Jam jam) {
        return "/bands/" + jam.getBand().getId() + "/jams?jam=" + jam.getId();
    }

    public JamResponse.Jam update(Long userId, Long bandId, Long jamId, JamRequest.Save request) {
        BandMember me = requireMember(userId, bandId);
        Jam jam = findJam(bandId, jamId);
        requireManager(jam, me);
        jam.update(request.title().trim(), request.bpm());
        linkSong(jam, bandId, request.setlistItemId());
        return response(jam, me);
    }

    public void delete(Long userId, Long bandId, Long jamId) {
        BandMember me = requireMember(userId, bandId);
        Jam jam = findJam(bandId, jamId);
        requireManager(jam, me);
        jam.getTracks().forEach(track -> cleanAfterCompletion(track.getFile().getKey(), true));
        jams.delete(jam);
    }

    /* ---------- 트랙 ---------- */

    public Track addTrack(Long userId, Long bandId, Long jamId, JamRequest.Track request, MultipartFile file) {
        BandMember me = requireMember(userId, bandId);
        Jam jam = findJam(bandId, jamId);
        if (jam.getTracks().size() >= MAX_TRACKS) throw new BusinessException(ErrorCode.JAM_TRACK_LIMIT_EXCEEDED);
        var stored = storage.storeAudio(file);
        cleanAfterCompletion(stored.key(), false);
        JamTrack track = tracks.save(JamTrack.builder()
                .jam(jam)
                .uploader(me.getUser())
                .part(me.getPart())
                .file(new BandPostFile(stored.key(), stored.name(), stored.type()))
                .volume(request.volume() == null ? DEFAULT_VOLUME : request.volume())
                .offsetMs(request.offsetMs())
                .durationMs(request.durationMs())
                .build());
        jam.getTracks().add(track);
        notificationService.notifyAll(List.of(jam.getCreator()), userId, NotificationType.JAM_TRACK, me.getBand().getName(),
                notificationService.nicknameOf(me.getUser()) + " 님이 내 잼 '" + jam.getTitle() + "'에 트랙을 올렸어요", jamUrl(jam));
        return track(track, me, nicknames(List.of(userId)));
    }

    // 볼륨·시작 위치는 밴드가 함께 맞추는 믹스라 멤버 누구나 바꿀 수 있다
    public Track updateTrack(Long userId, Long bandId, Long jamId, Long trackId, JamRequest.TrackUpdate request) {
        BandMember me = requireMember(userId, bandId);
        JamTrack track = findTrack(findJam(bandId, jamId), trackId);
        if (request.volume() != null) track.changeVolume(request.volume());
        if (request.offsetMs() != null) track.changeOffset(request.offsetMs());
        return track(track, me, nicknames(List.of(track.getUploader().getId())));
    }

    public void deleteTrack(Long userId, Long bandId, Long jamId, Long trackId) {
        BandMember me = requireMember(userId, bandId);
        Jam jam = findJam(bandId, jamId);
        JamTrack track = findTrack(jam, trackId);
        if (!canDelete(track, me)) throw new BusinessException(ErrorCode.JAM_PERMISSION_DENIED);
        cleanAfterCompletion(track.getFile().getKey(), true);
        jam.getTracks().remove(track);
    }

    @Transactional(readOnly = true)
    public AttachmentFile file(Long userId, Long bandId, Long jamId, Long trackId) {
        requireMember(userId, bandId);
        BandPostFile file = findTrack(findJam(bandId, jamId), trackId).getFile();
        return new AttachmentFile(storage.load(file.getKey()), file.getName(), file.getType());
    }

    /* ---------- 내부 ---------- */

    private BandMember requireMember(Long userId, Long bandId) {
        return members.findByBandIdAndUserId(bandId, userId)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()) && !Boolean.TRUE.equals(m.getBand().getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_BAND_MEMBER));
    }

    private Jam findJam(Long bandId, Long jamId) {
        return jams.findByIdAndBandId(jamId, bandId).orElseThrow(() -> new BusinessException(ErrorCode.JAM_NOT_FOUND));
    }

    private JamTrack findTrack(Jam jam, Long trackId) {
        return jam.getTracks().stream().filter(track -> track.getId().equals(trackId)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.JAM_TRACK_NOT_FOUND));
    }

    private boolean canManage(Jam jam, BandMember me) {
        return me.isLeader() || jam.getCreator().getId().equals(me.getUser().getId());
    }

    private void requireManager(Jam jam, BandMember me) {
        if (!canManage(jam, me)) throw new BusinessException(ErrorCode.JAM_PERMISSION_DENIED);
    }

    private boolean canDelete(JamTrack track, BandMember me) {
        return me.isLeader() || track.getUploader().getId().equals(me.getUser().getId());
    }

    // 같은 밴드 셋리스트의 곡만 연결하고, 곡 이름을 함께 저장한다
    private void linkSong(Jam jam, Long bandId, Long setlistItemId) {
        if (setlistItemId == null) {
            jam.linkSong(null, null);
            return;
        }
        if (setlistItemId.equals(jam.getSetlistItemId())) return;
        var item = setlistItems.findById(setlistItemId)
                .filter(i -> i.getSetlist().getBand().getId().equals(bandId))
                .orElseThrow(() -> new BusinessException(ErrorCode.JAM_SONG_INVALID));
        jam.linkSong(item.getId(), item.getCustomTitle());
    }

    private Song song(Jam jam) {
        return jam.getSetlistItemId() == null ? null : new Song(jam.getSetlistItemId(), jam.getSongTitle());
    }

    private Map<Long, String> nicknames(Collection<Long> userIds) {
        return characters.findAllByUserIdIn(new HashSet<>(userIds)).stream()
                .collect(Collectors.toMap(c -> c.getUser().getId(), Character::getNickname, (a, b) -> a));
    }

    private JamResponse.Jam response(Jam jam, BandMember me) {
        List<Long> userIds = new ArrayList<>(jam.getTracks().stream().map(track -> track.getUploader().getId()).toList());
        userIds.add(jam.getCreator().getId());
        Map<Long, String> names = nicknames(userIds);
        List<Track> result = jam.getTracks().stream().map(track -> track(track, me, names)).toList();
        return new JamResponse.Jam(jam.getId(), jam.getTitle(), jam.getBpm(), song(jam), names.get(jam.getCreator().getId()),
                canManage(jam, me), result);
    }

    private Track track(JamTrack track, BandMember me, Map<Long, String> names) {
        Jam jam = track.getJam();
        User uploader = track.getUploader();
        String url = "/api/v1/bands/" + jam.getBand().getId() + "/jams/" + jam.getId() + "/tracks/" + track.getId() + "/file";
        return new Track(track.getId(), uploader.getId(), names.get(uploader.getId()), track.getPart(),
                track.getFile().getName(), track.getFile().getType(), track.getVolume(), track.getOffsetMs(),
                track.getDurationMs(), canDelete(track, me), url, track.getCreatedAt());
    }

    // DB가 롤백되면 새 파일만, 커밋되면 지운 트랙의 파일만 정리한다
    private void cleanAfterCompletion(String key, boolean onCommit) {
        if (key == null) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if ((status == STATUS_COMMITTED) == onCommit) storage.delete(key);
            }
        });
    }
}
