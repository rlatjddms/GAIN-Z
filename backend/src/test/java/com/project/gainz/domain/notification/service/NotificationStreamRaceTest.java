package com.project.gainz.domain.notification.service;

import com.project.gainz.domain.notification.dto.response.LiveChange;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// 새 연결과 끊긴 연결 정리가 동시에 일어나도 새 연결이 알림을 받는다 (#282)
class NotificationStreamRaceTest {

    static class RecordingEmitter extends SseEmitter {
        final List<String> sent = new CopyOnWriteArrayList<>();
        volatile boolean broken;

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (broken) throw new IOException("끊김");
            Set<DataWithMediaType> parts = builder.build();
            sent.add(parts.stream().map(p -> String.valueOf(p.getData())).collect(Collectors.joining()));
        }

        @Override
        public void complete() {
        }
    }

    @Test void newConnectionIsNotLostWhileOldOneIsRemoved() throws Exception {
        for (int round = 0; round < 3000; round++) {
            NotificationStream stream = new NotificationStream() {
                @Override
                SseEmitter newEmitter() {
                    return new RecordingEmitter();
                }
            };
            // 이미 끊긴 연결 하나만 있는 사용자
            RecordingEmitter old = (RecordingEmitter) stream.subscribe(1L, 0);
            old.broken = true;

            CyclicBarrier start = new CyclicBarrier(2);
            RecordingEmitter[] fresh = new RecordingEmitter[1];
            Thread connect = new Thread(() -> {
                await(start);
                fresh[0] = (RecordingEmitter) stream.subscribe(1L, 0);
            });
            // 신호를 보내다 끊긴 연결을 발견하고 지운다 (목록이 비면 칸째로)
            Thread cleanup = new Thread(() -> {
                await(start);
                stream.sendChange(1L, LiveChange.friends());
            });
            connect.start();
            cleanup.start();
            connect.join();
            cleanup.join();

            String marker = "bandId=" + round;
            stream.sendChange(1L, LiveChange.band((long) round));
            assertThat(fresh[0].sent).as("round %d", round).anyMatch(s -> s.contains(marker));
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
