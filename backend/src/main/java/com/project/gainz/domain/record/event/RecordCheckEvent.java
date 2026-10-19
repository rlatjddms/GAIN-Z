package com.project.gainz.domain.record.event;

import com.project.gainz.domain.record.enums.RecordMetric;

import java.util.Set;

// 업적 판정 요청. 연습·루틴·밴드·친구·잼에서 일이 끝나면 내고, RecordService가 그 일이 커밋된 뒤 판정한다
public record RecordCheckEvent(Long userId, Set<RecordMetric> metrics) {

    public static RecordCheckEvent of(Long userId, RecordMetric first, RecordMetric... rest) {
        return new RecordCheckEvent(userId, java.util.EnumSet.of(first, rest));
    }
}
