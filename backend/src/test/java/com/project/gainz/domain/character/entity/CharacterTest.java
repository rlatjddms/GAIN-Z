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
        boolean leveledUp = character.gainExperience(69);

        assertThat(leveledUp).isFalse();
        assertThat(character.getLevel()).isEqualTo(1);
        assertThat(character.getExp()).isEqualTo(69);
    }

    @Test
    void 필요_경험치에_정확히_도달하면_레벨업하고_경험치는_0이_된다() {
        // Lv.1 → 2: 50 × 1 + 20
        boolean leveledUp = character.gainExperience(70);

        assertThat(leveledUp).isTrue();
        assertThat(character.getLevel()).isEqualTo(2);
        assertThat(character.getExp()).isZero();
    }

    @Test
    void 초과_경험치는_다음_레벨로_이월된다() {
        character.gainExperience(100);

        assertThat(character.getLevel()).isEqualTo(2);
        assertThat(character.getExp()).isEqualTo(30);
    }

    @Test
    void 한_번에_여러_레벨을_올릴_수_있다() {
        // 70 + 120 = 190
        character.gainExperience(240);

        assertThat(character.getLevel()).isEqualTo(3);
        assertThat(character.getExp()).isEqualTo(50);
    }

    @Test
    void 레벨이_오를수록_필요_경험치가_늘어난다() {
        // Lv.10까지 2430
        character.gainExperience(2430);

        assertThat(character.getLevel()).isEqualTo(10);
        assertThat(character.getExp()).isZero();

        character.gainExperience(519);
        assertThat(character.getLevel()).isEqualTo(10);

        character.gainExperience(1);
        assertThat(character.getLevel()).isEqualTo(11);
    }

    @Test
    void 만렙에_도달하면_경험치는_0으로_고정된다() {
        character.gainExperience(Integer.MAX_VALUE / 2);

        assertThat(character.getLevel()).isEqualTo(30);
        assertThat(character.getExp()).isZero();
    }

    @Test
    void 만렙이면_경험치를_더_받지_않는다() {
        character.gainExperience(Integer.MAX_VALUE / 2);

        boolean leveledUp = character.gainExperience(1000);

        assertThat(leveledUp).isFalse();
        assertThat(character.getLevel()).isEqualTo(30);
        assertThat(character.getExp()).isZero();
    }

    @Test
    void 피부색을_지정하지_않으면_기본값이_들어간다() {
        assertThat(character.getSkinColor()).isEqualTo(Character.DEFAULT_SKIN_COLOR);
    }

    @Test
    void 수정_시_피부색이_null이면_기존_값을_유지한다() {
        character.update("tester", InstrumentType.values()[0], "#000000", Gender.values()[0], HairStyle.values()[0], "#000000", "#C68642");
        assertThat(character.getSkinColor()).isEqualTo("#C68642");

        character.update("tester", InstrumentType.values()[0], "#000000", Gender.values()[0], HairStyle.values()[0], "#000000", null);
        assertThat(character.getSkinColor()).isEqualTo("#C68642");
    }

    @Test
    void 경험치를_되돌려도_레벨은_그대로이고_부족분은_다음에_채운다() {
        character.gainExperience(70 + 20);          // Lv.2, 20
        character.loseExperience(50);

        assertThat(character.getLevel()).isEqualTo(2);
        assertThat(character.getExp()).isEqualTo(-30);

        // 부족분 30을 채우고 Lv.2 → 3에 필요한 120을 더 모아야 레벨업
        assertThat(character.gainExperience(149)).isFalse();
        assertThat(character.gainExperience(1)).isTrue();
        assertThat(character.getLevel()).isEqualTo(3);
        assertThat(character.getExp()).isZero();
    }

    @Test
    void 최대_레벨이면_경험치를_되돌리지_않는다() {
        character.gainExperience(Integer.MAX_VALUE / 2);
        character.loseExperience(500);

        assertThat(character.getLevel()).isEqualTo(30);
        assertThat(character.getExp()).isZero();
    }

    @Test
    void 연습_시간은_누적된다() {
        character.addPracticeTime(30);
        character.addPracticeTime(15);

        assertThat(character.getTotalPracticeTime()).isEqualTo(45L);
    }

    @Test
    void 연습_시간을_줄여도_0_아래로_내려가지_않는다() {
        character.addPracticeTime(10);
        character.addPracticeTime(-30);

        assertThat(character.getTotalPracticeTime()).isZero();
    }
}
