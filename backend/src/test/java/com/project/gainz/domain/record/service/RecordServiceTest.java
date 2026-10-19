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
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordServiceTest {

    private static final Long ME = 1L;

    @Mock private UserRecordRepository userRecordRepository;
    @Mock private UserRepository userRepository;
    @Mock private PracticeLogRepository practiceLogRepository;
    @Mock private RoutineService routineService;
    @Mock private BandMemberRepository bandMemberRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private JamTrackRepository jamTrackRepository;
    @Mock private NotificationService notificationService;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks private RecordService recordService;

    private final LocalDate today = LocalDate.now();
    private User me;
    private final List<UserRecord> owned = new ArrayList<>();

    @BeforeEach
    void setUp() {
        me = User.builder().email("me@example.com").password("pw").name("나")
                .provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build();
        ReflectionTestUtils.setField(me, "id", ME);
        ReflectionTestUtils.setField(me, "createdAt", today.minusDays(20).atStartOfDay());
        lenient().when(userRepository.findById(ME)).thenReturn(Optional.of(me));
        lenient().when(userRecordRepository.findAllByUserId(ME)).thenAnswer(inv -> List.copyOf(owned));
        lenient().when(userRecordRepository.save(any(UserRecord.class))).thenAnswer(inv -> {
            UserRecord r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "createdAt", LocalDateTime.now());
            owned.add(r);
            return r;
        });
    }

    private void own(RecordType type) {
        UserRecord r = new UserRecord(me, type);
        ReflectionTestUtils.setField(r, "createdAt", LocalDateTime.of(2026, 10, 1, 12, 0));
        owned.add(r);
    }

    // 날짜별 연습 시간 (초). 연습 안 한 날도 섞어 둔다
    private void practiced(Map<LocalDate, Long> secondsByDay) {
        List<DailyPracticeResponse> days = secondsByDay.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> new DailyPracticeResponse(e.getKey(), e.getValue(), 0L)).toList();
        when(practiceLogRepository.findDailyTotals(eq(ME), any(), eq(today))).thenReturn(days);
    }

    private List<RecordType> grantedTypes() {
        ArgumentCaptor<UserRecord> saved = ArgumentCaptor.forClass(UserRecord.class);
        verify(userRecordRepository, org.mockito.Mockito.atLeast(0)).save(saved.capture());
        return saved.getAllValues().stream().map(UserRecord::getRecordType).toList();
    }

    /* ---------- 수치 ---------- */

    @Test
    void 가장_긴_연속_연습_일수를_센다() {
        LocalDate d = LocalDate.of(2026, 10, 1);
        assertThat(RecordService.longestStreak(List.of())).isZero();
        assertThat(RecordService.longestStreak(List.of(d))).isEqualTo(1);
        assertThat(RecordService.longestStreak(List.of(d, d.plusDays(1), d.plusDays(3), d.plusDays(4), d.plusDays(5))))
                .isEqualTo(3);
    }

    @Test
    void 연습_기록으로_연속_일수와_총_시간을_잰다() {
        practiced(Map.of(
                today.minusDays(3), 3600L * 4,
                today.minusDays(2), 3600L * 3,
                today.minusDays(1), 0L,          // 연습 안 한 날은 세지 않는다
                today, 1800L));

        Map<RecordMetric, Long> values = recordService.measure(ME,
                EnumSet.of(RecordMetric.STREAK_DAYS, RecordMetric.PRACTICE_HOURS));

        assertThat(values).containsEntry(RecordMetric.STREAK_DAYS, 2L)
                .containsEntry(RecordMetric.PRACTICE_HOURS, 7L);   // 7.5시간 → 내림
        // 가입 7일 전부터 센다
        verify(practiceLogRepository).findDailyTotals(ME, today.minusDays(27), today);
        verifyNoInteractions(routineService, bandMemberRepository, friendshipRepository, jamTrackRepository);
    }

    @Test
    void 루틴을_모두_완료한_날은_1년씩_나눠_센다() {
        ReflectionTestUtils.setField(me, "createdAt", today.minusDays(500).atStartOfDay());
        when(routineService.getDailySummary(eq(ME), any(), any())).thenReturn(
                List.of(new RoutineDailySummaryResponse(today, 3, 3), new RoutineDailySummaryResponse(today, 3, 2)));

        Map<RecordMetric, Long> values = recordService.measure(ME, EnumSet.of(RecordMetric.ROUTINE_DAYS));

        // 507일 → 366일씩 두 번, 두 번 다 완료한 날 1일
        verify(routineService, times(2)).getDailySummary(eq(ME), any(), any());
        assertThat(values).containsEntry(RecordMetric.ROUTINE_DAYS, 2L);
    }

    /* ---------- 판정·지급 ---------- */

    @Test
    void 달성한_것만_지급하고_알림을_보낸다() {
        // 오늘까지 7일 연속, 모두 합쳐 11시간
        Map<LocalDate, Long> week = new java.util.HashMap<>();
        for (int i = 0; i < 7; i++) week.put(today.minusDays(i), i == 0 ? 3600L * 5 : 3600L);
        practiced(week);

        List<UserRecord> granted = recordService.check(ME, EnumSet.of(RecordMetric.STREAK_DAYS, RecordMetric.PRACTICE_HOURS));

        // 7일 연속(카세트), 10시간 → 30일 연속·50시간은 아직
        assertThat(granted).extracting(UserRecord::getRecordType)
                .containsExactly(RecordType.STREAK_7, RecordType.HOURS_10);
        verify(notificationService).notify(me, NotificationType.RECORD, "MY RECORDS",
                "새 레코드를 얻었어요: 일주일 세션 (카세트)", RecordService.RECORDS_URL);
        verify(notificationService, times(2)).notify(eq(me), eq(NotificationType.RECORD), any(), any(), any());
    }

    @Test
    void 이미_가진_것은_다시_주지_않고_필요한_수치만_잰다() {
        own(RecordType.FIRST_FRIEND);

        assertThat(recordService.check(ME, EnumSet.of(RecordMetric.FRIENDS))).isEmpty();

        verifyNoInteractions(friendshipRepository);
        verify(userRecordRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void 친구_밴드_트랙은_개수로_판정한다() {
        when(friendshipRepository.countByUserIdAndStatus(ME, FriendshipStatus.ACCEPTED)).thenReturn(1L);
        when(bandMemberRepository.countByUserId(ME)).thenReturn(0L);
        when(jamTrackRepository.countByUploaderId(ME)).thenReturn(2L);

        recordService.check(ME, EnumSet.of(RecordMetric.FRIENDS, RecordMetric.BANDS, RecordMetric.JAM_TRACKS));

        // 트랙 2개: 첫 합주(1)는 되고 멀티트랙(10)은 아직
        assertThat(grantedTypes()).containsExactly(RecordType.FIRST_FRIEND, RecordType.FIRST_JAM_TRACK);
    }

    @Test
    void 판정이_실패해도_원래_일에는_예외가_나가지_않는다() {
        when(friendshipRepository.countByUserIdAndStatus(anyLong(), any())).thenThrow(new IllegalStateException("DB 오류"));

        assertThatCode(() -> recordService.onRecordCheck(RecordCheckEvent.of(ME, RecordMetric.FRIENDS))).doesNotThrowAnyException();
    }

    /* ---------- 도감 ---------- */

    @Test
    void 도감은_순서대로_얻은_것과_진행도를_보여_주고_이미_달성한_것은_이때_지급한다() {
        own(RecordType.STREAK_7);
        practiced(Map.of(today.minusDays(1), 3600L * 12, today, 60L));
        when(routineService.getDailySummary(eq(ME), any(), any())).thenReturn(List.of());
        when(bandMemberRepository.countByUserId(ME)).thenReturn(1L);
        when(friendshipRepository.countByUserIdAndStatus(ME, FriendshipStatus.ACCEPTED)).thenReturn(0L);
        when(jamTrackRepository.countByUploaderId(ME)).thenReturn(0L);

        RecordResponse.Collection collection = recordService.getCollection(ME);

        assertThat(collection.total()).isEqualTo(RecordType.values().length);
        assertThat(collection.records()).extracting(RecordResponse::key)
                .containsExactly(java.util.Arrays.stream(RecordType.values()).map(Enum::name).toArray(String[]::new));
        // 원래 있던 일주일 세션 + 이번에 지급된 10시간·첫 무대
        assertThat(collection.ownedCount()).isEqualTo(3);
        assertThat(collection.records()).filteredOn(RecordResponse::owned).extracting(RecordResponse::key)
                .containsExactly("STREAK_7", "HOURS_10", "FIRST_BAND");
        RecordResponse hours50 = collection.records().stream().filter(r -> r.key().equals("HOURS_50")).findFirst().orElseThrow();
        assertThat(hours50.owned()).isFalse();
        assertThat(hours50.acquiredAt()).isNull();
        assertThat(hours50.progress()).isEqualTo(new RecordResponse.Progress(12, 50, RecordMetric.Unit.HOUR));
        // 이미 얻은 것은 얻은 때가 그대로, 진행도는 목표를 넘지 않는다
        RecordResponse first = collection.records().get(0);
        assertThat(first.acquiredAt()).isEqualTo(LocalDateTime.of(2026, 10, 1, 12, 0));
        RecordResponse hours10 = collection.records().stream().filter(r -> r.key().equals("HOURS_10")).findFirst().orElseThrow();
        assertThat(hours10.progress().current()).isEqualTo(10);
    }
}
