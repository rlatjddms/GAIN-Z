package com.project.gainz.domain.band.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BandMentionsTest {

    private final Map<Long, String> nicknames = new LinkedHashMap<>(Map.of(
            1L, "기타왕",
            2L, "드럼",
            3L, "드럼짱",
            4L, "보컬"));

    @Test
    void 본문의_골뱅이_닉네임을_멤버로_찾는다() {
        assertThat(BandMentions.mentionedUserIds("@기타왕 @보컬 이번 주 합주 어때요?", nicknames)).containsExactly(1L, 4L);
    }

    @Test
    void 같은_자리에서는_가장_긴_닉네임_하나만() {
        // @드럼짱은 드럼짱(3)만, @드럼 은 드럼(2)
        assertThat(BandMentions.mentionedUserIds("@드럼짱 고마워", nicknames)).containsExactly(3L);
        assertThat(BandMentions.mentionedUserIds("@드럼 님도", nicknames)).containsExactly(2L);
    }

    @Test
    void 골뱅이가_없거나_멤버가_아니면_없다() {
        assertThat(BandMentions.mentionedUserIds("그냥 글", nicknames)).isEmpty();
        assertThat(BandMentions.mentionedUserIds("메일은 me@example.com", nicknames)).isEmpty();
        assertThat(BandMentions.mentionedUserIds("@베이스 어디 갔어", nicknames)).isEmpty();
        assertThat(BandMentions.mentionedUserIds(null, nicknames)).isEmpty();
    }

    @Test
    void 같은_사람을_여러_번_불러도_한_번() {
        assertThat(BandMentions.mentionedUserIds("@보컬 @보컬 @보컬", nicknames)).containsExactly(4L);
    }
}
