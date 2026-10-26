package com.project.gainz.global.storage;

import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.global.mail.MailClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

// 저장소 전체 용량 한도 (#262). R2 무료 범위(10GB)를 넘어 요금이 나가지 않게, 한도(기본 9GB)를 넘는 새 파일은 받지 않는다.
// 이미 올린 파일은 그대로 볼 수 있다. 쓴 용량은 저장소에서 10분마다 다시 세고, 그 사이에 올린 만큼은 더해 둔다.
// 경고 선(기본 8GB)과 한도에 닿으면 gainz.storage.alert-email로 메일을 한 번씩 보낸다 (용량이 다시 줄면 다음에 또 보낸다)
@Slf4j
@Component
public class StorageQuota {

    static final Duration RECOUNT = Duration.ofMinutes(10);

    private enum Level { OK, WARN, FULL }

    private final FileStore store;
    private final MailClient mail;
    private final long limitBytes;
    private final long warnBytes;
    private final String alertEmail;
    private final Clock clock;

    private long counted = -1;
    private long addedSince;
    private Instant countedAt;
    private Level alerted = Level.OK;

    @Autowired
    public StorageQuota(FileStore store, MailClient mail,
                        @Value("${gainz.storage.quota:9GB}") DataSize limit,
                        @Value("${gainz.storage.quota-warn:8GB}") DataSize warn,
                        @Value("${gainz.storage.alert-email:}") String alertEmail) {
        this(store, mail, limit.toBytes(), warn.toBytes(), alertEmail, Clock.systemUTC());
    }

    StorageQuota(FileStore store, MailClient mail, long limitBytes, long warnBytes, String alertEmail, Clock clock) {
        this.store = store;
        this.mail = mail;
        this.limitBytes = limitBytes;
        this.warnBytes = warnBytes;
        this.alertEmail = alertEmail;
        this.clock = clock;
    }

    // 새 파일을 받기 전에 부른다. 한도를 넘으면 STORAGE_FULL. 쓴 용량을 모르면(세지 못하는 저장소·오류) 막지 않는다
    public synchronized void check(long incomingBytes) {
        long used = used();
        if (used < 0) return;
        if (used + incomingBytes > limitBytes) {
            alert(Level.FULL, used);
            throw new BusinessException(ErrorCode.STORAGE_FULL);
        }
    }

    // 받은 파일 크기를 더해 둔다 (다음에 다시 셀 때까지)
    public synchronized void added(long bytes) {
        if (counted >= 0) addedSince += bytes;
        long used = used();
        if (used >= warnBytes) alert(used >= limitBytes ? Level.FULL : Level.WARN, used);
    }

    private long used() {
        Instant now = clock.instant();
        if (countedAt == null || !now.isBefore(countedAt.plus(RECOUNT))) {
            countedAt = now;
            try {
                counted = store.totalSize();
                addedSince = 0;
            } catch (IOException e) {
                log.warn("저장소 용량을 세지 못했어요 ({})", e.getMessage());
            }
            if (counted >= 0 && counted < warnBytes) alerted = Level.OK;
        }
        return counted < 0 ? -1 : counted + addedSince;
    }

    private void alert(Level level, long used) {
        if (level.ordinal() <= alerted.ordinal()) return;
        alerted = level;
        String usage = String.format("%.2fGB / 한도 %.2fGB", used / 1e9, limitBytes / 1e9);
        log.warn("저장소 용량 {}: {}", level == Level.FULL ? "가득 참" : "경고", usage);
        if (alertEmail == null || alertEmail.isBlank()) return;
        String subject = level == Level.FULL ? "[GAIN-Z] 저장 공간이 가득 차서 새 파일을 막았어요" : "[GAIN-Z] 저장 공간이 곧 가득 차요";
        String body = "<p>지금 쓰는 용량: " + usage + "</p>"
                + (level == Level.FULL
                ? "<p>한도를 넘는 새 파일은 받지 않고 있어요. 이미 올린 파일은 그대로 볼 수 있어요.</p>"
                : "<p>한도에 닿으면 새 파일을 받지 않아요.</p>")
                + "<p>오래된 파일을 지우거나, 한도(STORAGE_QUOTA)를 조정해 주세요. R2 무료 범위는 10GB예요.</p>";
        try {
            mail.send(alertEmail, subject, body);
        } catch (RuntimeException e) {
            log.warn("저장소 용량 알림 메일을 보내지 못했어요 ({})", e.getMessage());
        }
    }
}
