package com.project.gainz.domain.notification.service;

import com.project.gainz.domain.notification.dto.response.LiveChange;
import com.project.gainz.domain.notification.dto.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// 실시간 알림 연결 (SSE). 사용자마다 열린 탭 수만큼 연결을 들고 있다가 새 알림을 바로 보낸다.
// 서버 한 대 기준 (여러 대로 늘리면 Redis pub/sub으로 다른 서버의 연결에도 전해야 한다)
@Slf4j
@Component
public class NotificationStream {

    // 연결은 30분마다 끊기고 화면이 다시 붙는다
    static final long TIMEOUT_MS = 30 * 60 * 1000L;
    // 한 사람이 동시에 열 수 있는 연결 (탭) 수. 넘으면 가장 오래된 연결을 닫는다
    static final int MAX_CONNECTIONS_PER_USER = 5;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // 새 연결. 붙자마자 안 읽은 개수를 보낸다
    public SseEmitter subscribe(Long userId, long unreadCount) {
        SseEmitter emitter = newEmitter();
        // 목록 찾기와 넣기를 한 번에 (#282). 나눠 하면 그 사이 다른 연결이 끊기며 빈 목록을 지워서, 새 연결이 아무도 안 보는 목록에 들어갈 수 있다
        List<SseEmitter> overflow = new ArrayList<>();
        emitters.compute(userId, (id, mine) -> {
            List<SseEmitter> list = mine != null ? mine : new CopyOnWriteArrayList<>();
            list.add(emitter);
            while (list.size() > MAX_CONNECTIONS_PER_USER) overflow.add(list.remove(0));
            return list;
        });
        // 닫으면 remove가 불리므로 compute 밖에서 (같은 칸을 compute 안에서 다시 고치면 안 된다)
        overflow.forEach(SseEmitter::complete);
        Runnable remove = () -> remove(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        send(userId, emitter, SseEmitter.event().name("unread").data(new NotificationResponse.UnreadCount(unreadCount)));
        return emitter;
    }

    // 새 알림 하나를 그 사람의 모든 연결로
    public void send(Long userId, NotificationResponse notification) {
        List<SseEmitter> mine = emitters.get(userId);
        if (mine == null) return;
        for (SseEmitter emitter : mine) {
            send(userId, emitter, SseEmitter.event().name("notification").id(String.valueOf(notification.id())).data(notification));
        }
    }

    // 바뀌었다는 신호 하나를 그 사람의 모든 연결로 (#259)
    public void sendChange(Long userId, LiveChange change) {
        List<SseEmitter> mine = emitters.get(userId);
        if (mine == null) return;
        for (SseEmitter emitter : mine) {
            send(userId, emitter, SseEmitter.event().name("change").data(change));
        }
    }

    // 프록시·브라우저가 조용한 연결을 끊지 않게 25초마다 빈 신호
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        emitters.forEach((userId, mine) -> mine.forEach(emitter -> send(userId, emitter, SseEmitter.event().comment("ping"))));
    }

    int connectionCount(Long userId) {
        List<SseEmitter> mine = emitters.get(userId);
        return mine == null ? 0 : mine.size();
    }

    SseEmitter newEmitter() {
        return new SseEmitter(TIMEOUT_MS);
    }

    private void send(Long userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            // 탭을 닫았거나 끊긴 연결
            remove(userId, emitter);
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (id, mine) -> {
            mine.remove(emitter);
            return mine.isEmpty() ? null : mine;
        });
    }
}
