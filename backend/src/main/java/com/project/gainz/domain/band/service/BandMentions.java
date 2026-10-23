package com.project.gainz.domain.band.service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 글·댓글 본문의 @닉네임을 밴드 멤버와 맞춰 본다.
// 화면(MentionText)과 같은 규칙: '@' 자리에서 맞는 닉네임 중 가장 긴 것 하나
final class BandMentions {

    private BandMentions() {
    }

    // nicknames: userId → 캐릭터 닉네임. 불린 사람의 userId (본문 순서)
    static Set<Long> mentionedUserIds(String text, Map<Long, String> nicknames) {
        Set<Long> result = new LinkedHashSet<>();
        if (text == null || text.indexOf('@') < 0 || nicknames.isEmpty()) return result;
        List<Map.Entry<Long, String>> byLength = nicknames.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .sorted(Comparator.comparingInt((Map.Entry<Long, String> e) -> e.getValue().length()).reversed())
                .toList();
        for (int at = text.indexOf('@'); at >= 0; at = text.indexOf('@', at + 1)) {
            for (Map.Entry<Long, String> entry : byLength) {
                if (text.startsWith(entry.getValue(), at + 1)) {
                    result.add(entry.getKey());
                    break;
                }
            }
        }
        return result;
    }
}
