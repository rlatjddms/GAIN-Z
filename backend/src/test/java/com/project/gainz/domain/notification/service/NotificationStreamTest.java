package com.project.gainz.domain.notification.service;

import com.project.gainz.domain.notification.dto.response.LiveChange;
import com.project.gainz.domain.notification.dto.response.NotificationResponse;
import com.project.gainz.domain.notification.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationStreamTest {

    // 보낸 이벤트를 글자로 모아 두는 연결 (끊긴 연결 흉내도 낸다)
    static class RecordingEmitter extends SseEmitter {
        final List<String> sent = new ArrayList<>();
        boolean broken;
        boolean completed;

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (broken) throw new IOException("끊김");
            Set<DataWithMediaType> parts = builder.build();
            sent.add(parts.stream().map(p -> String.valueOf(p.getData())).collect(Collectors.joining()));
        }

        @Override
        public void complete() {
            completed = true;
        }
    }

    final List<RecordingEmitter> created = new ArrayList<>();
    final NotificationStream stream = new NotificationStream() {
        @Override
        SseEmitter newEmitter() {
            RecordingEmitter emitter = new RecordingEmitter();
            created.add(emitter);
            return emitter;
        }
    };

    NotificationResponse notification(long id) {
        return new NotificationResponse(id, NotificationType.FRIEND_REQUEST, "친구", "가 님이 친구 신청을 보냈어요", "/friends",
                false, LocalDateTime.of(2026, 10, 19, 12, 0));
    }

    @Test void sendsUnreadCountRightAfterConnecting() {
        stream.subscribe(1L, 3);

        RecordingEmitter emitter = created.get(0);
        assertThat(emitter.sent).hasSize(1);
        assertThat(emitter.sent.get(0)).contains("event:unread").contains("UnreadCount[count=3]");
    }

    @Test void newNotificationGoesToEveryTabOfThatUserOnly() {
        stream.subscribe(1L, 0);
        stream.subscribe(1L, 0);
        stream.subscribe(2L, 0);

        stream.send(1L, notification(9L));

        assertThat(created.get(0).sent).last().asString().contains("event:notification").contains("id:9");
        assertThat(created.get(1).sent).last().asString().contains("event:notification");
        assertThat(created.get(2).sent).hasSize(1);   // 2번 사용자는 처음 개수만
        stream.send(3L, notification(10L));           // 연결 없는 사람은 그냥 넘어간다
    }

    @Test void brokenConnectionIsDroppedAndHeartbeatReachesTheRest() {
        stream.subscribe(1L, 0);
        stream.subscribe(1L, 0);
        created.get(0).broken = true;

        stream.heartbeat();

        assertThat(stream.connectionCount(1L)).isEqualTo(1);
        assertThat(created.get(1).sent).last().asString().contains(":ping");
    }

    @Test void tooManyTabsCloseTheOldestConnection() {
        for (int i = 0; i <= NotificationStream.MAX_CONNECTIONS_PER_USER; i++) stream.subscribe(1L, 0);

        assertThat(stream.connectionCount(1L)).isEqualTo(NotificationStream.MAX_CONNECTIONS_PER_USER);
        assertThat(created.get(0).completed).isTrue();
    }

    // 바뀌었다는 신호 (#259)는 그 사람의 모든 연결로, 다른 사람에게는 안 간다
    @Test void sendsChangeToEveryConnectionOfThatUser() {
        stream.subscribe(1L, 0);
        stream.subscribe(1L, 0);
        stream.subscribe(2L, 0);

        stream.sendChange(1L, LiveChange.band(10L));

        assertThat(created.get(0).sent).hasSize(2);
        assertThat(created.get(0).sent.get(1)).contains("event:change").contains("topic=BAND").contains("bandId=10");
        assertThat(created.get(1).sent).hasSize(2);
        assertThat(created.get(2).sent).hasSize(1);
    }
}
