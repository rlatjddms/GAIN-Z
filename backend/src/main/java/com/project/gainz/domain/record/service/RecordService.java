package com.project.gainz.domain.record.service;

import com.project.gainz.domain.band.repository.BandMemberRepository;
import com.project.gainz.domain.band.repository.JamTrackRepository;
import com.project.gainz.domain.friend.enums.FriendshipStatus;
import com.project.gainz.domain.friend.repository.FriendshipRepository;
import com.project.gainz.domain.notification.enums.NotificationType;
import com.project.gainz.domain.notification.service.NotificationService;
import com.project.gainz.domain.practice.dto.response.DailyPracticeResponse;
import com.project.gainz.domain.practice.dto.response.RoutineDailySummaryResponse;
import com.project.gainz.domain.practice.repository.PracticeLogRepository;
import com.project.gainz.domain.practice.service.RoutineService;
import com.project.gainz.domain.record.dto.response.RecordResponse;
import com.project.gainz.domain.record.entity.UserRecord;
import com.project.gainz.domain.record.enums.RecordMetric;
import com.project.gainz.domain.record.enums.RecordType;
import com.project.gainz.domain.record.event.RecordCheckEvent;
import com.project.gainz.domain.record.repository.UserRecordRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 업적 레코드: 판정·지급·도감
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecordService {

    // 루틴 기록은 한 번에 1년씩 나눠 센다 (RoutineService.MAX_SUMMARY_DAYS)
    private static final int ROUTINE_CHUNK_DAYS = RoutineService.MAX_SUMMARY_DAYS - 1;
    // 지난 날짜도 고칠 수 있어서 가입일보다 조금 앞부터 센다
    private static final int PRACTICE_BACKFILL_DAYS = 7;
    static final String RECORDS_URL = "/home?records=open";

    private final UserRecordRepository userRecordRepository;
    private final UserRepository userRepository;
    private final PracticeLogRepository practiceLogRepository;
    private final RoutineService routineService;
    private final BandMemberRepository bandMemberRepository;
    private final FriendshipRepository friendshipRepository;
    private final JamTrackRepository jamTrackRepository;
    private final NotificationService notificationService;
    private final PlatformTransactionManager transactionManager;

    /* ---------- 도감 ---------- */

    // 도감을 열 때 이미 달성한 것(기능이 생기기 전 기록 포함)도 지급한다.
    // 지급은 따로 짧게 커밋해서, 홈과 도감이 동시에 열어 같은 레코드를 겹쳐 지급하려 해도 도감 조회는 실패하지 않는다
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RecordResponse.Collection getCollection(Long userId) {
        Map<RecordMetric, Long> values = measure(userId, EnumSet.allOf(RecordMetric.class));
        inNewTransaction(userId, "도감 지급", () -> grant(userId, values, ownedOf(userId).keySet()));
        Map<RecordType, LocalDateTime> owned = ownedOf(userId);
        List<RecordResponse> records = Arrays.stream(RecordType.values())
                .map(type -> RecordResponse.of(type, owned.get(type), values.getOrDefault(type.metric(), 0L)))
                .toList();
        return new RecordResponse.Collection(owned.size(), RecordType.values().length, records);
    }

    /* ---------- 판정 ---------- */

    // 연습·루틴·밴드·친구·잼의 일이 커밋된 뒤, 새 트랜잭션에서 판정한다. 실패해도 원래 일에는 영향이 없다
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onRecordCheck(RecordCheckEvent event) {
        inNewTransaction(event.userId(), "판정 " + event.metrics(), () -> check(event.userId(), event.metrics()));
    }

    // 새 트랜잭션에서 지급하고, 실패(겹친 지급 등)는 로그만 남긴다
    private void inNewTransaction(Long userId, String what, Runnable work) {
        try {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            template.executeWithoutResult(status -> work.run());
        } catch (RuntimeException e) {
            log.warn("레코드 {} 실패: 사용자 {} ({})", what, userId, e.getMessage());
        }
    }

    // 이 수치들로 얻을 수 있는 레코드 중 아직 없는 것을 지급한다 → 새로 얻은 레코드
    public List<UserRecord> check(Long userId, Set<RecordMetric> metrics) {
        Map<RecordType, LocalDateTime> owned = ownedOf(userId);
        Set<RecordMetric> needed = Arrays.stream(RecordType.values())
                .filter(type -> metrics.contains(type.metric()) && !owned.containsKey(type))
                .map(RecordType::metric)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(RecordMetric.class)));
        if (needed.isEmpty()) return List.of();
        return grant(userId, measure(userId, needed), owned.keySet());
    }

    private List<UserRecord> grant(Long userId, Map<RecordMetric, Long> values, Set<RecordType> owned) {
        List<RecordType> reached = Arrays.stream(RecordType.values())
                .filter(type -> !owned.contains(type) && values.containsKey(type.metric()))
                .filter(type -> values.get(type.metric()) >= type.goal())
                .toList();
        if (reached.isEmpty()) return List.of();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        List<UserRecord> granted = new ArrayList<>();
        for (RecordType type : reached) {
            granted.add(userRecordRepository.save(new UserRecord(user, type)));
            notificationService.notify(user, NotificationType.RECORD, "MY RECORDS",
                    "새 레코드를 얻었어요: " + type.title() + " (" + type.shape() + ")", RECORDS_URL);
        }
        return granted;
    }

    private Map<RecordType, LocalDateTime> ownedOf(Long userId) {
        Map<RecordType, LocalDateTime> owned = new EnumMap<>(RecordType.class);
        userRecordRepository.findAllByUserId(userId).forEach(r -> owned.put(r.getRecordType(), r.getCreatedAt()));
        return owned;
    }

    /* ---------- 수치 ---------- */

    Map<RecordMetric, Long> measure(Long userId, Set<RecordMetric> metrics) {
        Map<RecordMetric, Long> values = new EnumMap<>(RecordMetric.class);
        LocalDate today = LocalDate.now();
        LocalDate from = startDate(userId, today);

        if (metrics.contains(RecordMetric.PRACTICE_DAYS) || metrics.contains(RecordMetric.STREAK_DAYS)
                || metrics.contains(RecordMetric.PRACTICE_HOURS)) {
            List<LocalDate> practiced = new ArrayList<>();
            long seconds = 0;
            for (DailyPracticeResponse day : practiceLogRepository.findDailyTotals(userId, from, today)) {
                if (day.totalSeconds() == null || day.totalSeconds() <= 0) continue;
                practiced.add(day.date());
                seconds += day.totalSeconds();
            }
            values.put(RecordMetric.PRACTICE_DAYS, (long) practiced.size());
            values.put(RecordMetric.STREAK_DAYS, longestStreak(practiced));
            values.put(RecordMetric.PRACTICE_HOURS, seconds / 3600);
        }
        if (metrics.contains(RecordMetric.ROUTINE_DAYS)) {
            long days = 0;
            for (LocalDate start = from; !start.isAfter(today); start = start.plusDays(ROUTINE_CHUNK_DAYS + 1L)) {
                LocalDate end = start.plusDays(ROUTINE_CHUNK_DAYS).isAfter(today) ? today : start.plusDays(ROUTINE_CHUNK_DAYS);
                days += routineService.getDailySummary(userId, start, end).stream()
                        .filter(RecordService::allDone)
                        .count();
            }
            values.put(RecordMetric.ROUTINE_DAYS, days);
        }
        if (metrics.contains(RecordMetric.BANDS)) {
            values.put(RecordMetric.BANDS, bandMemberRepository.countByUserId(userId));
        }
        if (metrics.contains(RecordMetric.FRIENDS)) {
            values.put(RecordMetric.FRIENDS, friendshipRepository.countByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED));
        }
        if (metrics.contains(RecordMetric.JAM_TRACKS)) {
            values.put(RecordMetric.JAM_TRACKS, jamTrackRepository.countByUploaderId(userId));
        }
        return values;
    }

    private LocalDate startDate(Long userId, LocalDate today) {
        return userRepository.findById(userId)
                .map(User::getCreatedAt)
                .map(at -> at.toLocalDate().minusDays(PRACTICE_BACKFILL_DAYS))
                .filter(d -> !d.isAfter(today))
                .orElse(today.minusYears(1));
    }

    private static boolean allDone(RoutineDailySummaryResponse day) {
        return day.total() > 0 && day.completed() >= day.total();
    }

    // 날짜 오름차순 목록에서 가장 길게 이어진 날 수
    static long longestStreak(List<LocalDate> sortedDays) {
        long best = 0;
        long run = 0;
        LocalDate prev = null;
        for (LocalDate d : sortedDays) {
            run = prev != null && prev.plusDays(1).equals(d) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = d;
        }
        return best;
    }

    /* ---------- 회원 탈퇴 ---------- */

    public void removeAllOf(Long userId) {
        userRecordRepository.deleteAllByUserId(userId);
    }
}
