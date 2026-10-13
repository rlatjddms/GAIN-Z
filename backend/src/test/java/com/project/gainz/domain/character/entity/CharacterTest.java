package com.project.gainz.domain.character.entity;

import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.global.common.enums.InstrumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CharacterTest {

    private Character character;

    @BeforeEach
    void setUp() {
        character = Character.builder()
                .nickname("tester")
                .instrumentType(InstrumentType.values()[0])
                .instrumentColor("#000000")
                .gender(Gender.values()[0])
                .hairStyle(HairStyle.values()[0])
                .hairColor("#000000")
                .currentOutfit(mock(OutfitItem.class))
                .build();
    }

    @Test
    void 생성_직후_레벨은_1이고_경험치는_0이다() {
        assertThat(character.getLevel()).isEqualTo(1);
        assertThat(character.getExp()).isZero();
    }

    @Test
    void 필요_경험치_미만이면_레벨업하지_않는다() {
        boolean leveledUp = character.gainExperience(99);

        assertThat(leveledUp).isFalse();
        assertThat(character.getLevel()).isEqualTo(1);
        assertThat(character.getExp()).isEqualTo(99);
    }

    @Test
    void 필요_경험치에_정확히_도달하면_레벨업하고_경험치는_0이_된다() {
        boolean leveledUp = character.gainExperience(100);

        assertThat(leveledUp).isTrue();
        assertThat(character.getLevel()).isEqualTo(2);
        assertThat(character.getExp()).isZero();
    }

    @Test
    void 초과_경험치는_다음_레벨로_이월된다() {
        character.gainExperience(130);

        assertThat(character.getLevel()).isEqualTo(2);
        assertThat(character.getExp()).isEqualTo(30);
    }

    @Test
    void 한_번에_여러_레벨을_올릴_수_있다() {
        character.gainExperience(250);

        assertThat(character.getLevel()).isEqualTo(3);
        assertThat(character.getExp()).isEqualTo(50);
    }

    @Test
    void 구간이_바뀌면_바뀐_구간의_필요_경험치를_적용한다() {
        character.gainExperience(900);

        assertThat(character.getLevel()).isEqualTo(10);
        assertThat(character.getExp()).isZero();

        character.gainExperience(299);
        assertThat(character.getLevel()).isEqualTo(10);

        character.gainExperience(1);
        assertThat(character.getLevel()).isEqualTo(11);
    }

    @Test
    void 만렙에_도달하면_경험치는_0으로_고정된다() {
        character.gainExperience(Integer.MAX_VALUE / 2);

        assertThat(character.getLevel()).isEqualTo(50);
        assertThat(character.getExp()).isZero();
    }

    @Test
    void 만렙이면_경험치를_더_받지_않는다() {
        character.gainExperience(Integer.MAX_VALUE / 2);

        boolean leveledUp = character.gainExperience(1000);

        assertThat(leveledUp).isFalse();
        assertThat(character.getLevel()).isEqualTo(50);
        assertThat(character.getExp()).isZero();
    }

    @Test
    void 연습_시간은_누적된다() {
        character.addPracticeTime(30);
        character.addPracticeTime(15);

        assertThat(character.getTotalPracticeTime()).isEqualTo(45L);
    }
}
