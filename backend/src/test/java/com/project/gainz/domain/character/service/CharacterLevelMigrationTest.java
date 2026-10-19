package com.project.gainz.domain.character.service;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.enums.LevelPolicy;
import com.project.gainz.domain.character.enums.StageBackground;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.global.common.enums.InstrumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterLevelMigrationTest {

    @Mock private CharacterRepository characterRepository;
    @InjectMocks private CharacterLevelMigration migration;

    // 예전 규칙의 레벨·경험치를 가진 캐릭터
    private Character legacy(int level, int exp) {
        Character c = Character.builder().nickname("기타").instrumentType(InstrumentType.GUITAR).instrumentColor("#000000")
                .gender(Gender.values()[0]).hairStyle(HairStyle.values()[0]).hairColor("#000000")
                .currentOutfit(mock(OutfitItem.class)).build();
        ReflectionTestUtils.setField(c, "level", level);
        ReflectionTestUtils.setField(c, "exp", exp);
        ReflectionTestUtils.setField(c, "levelRule", null);
        return c;
    }

    @Test
    void 새로_만든_캐릭터는_지금_규칙으로_표시된다() {
        assertThat(ReflectionTestUtils.getField(legacy(1, 0), "levelRule")).isNull();
        Character fresh = Character.builder().nickname("새").instrumentType(InstrumentType.DRUM).instrumentColor("#000000")
                .gender(Gender.values()[0]).hairStyle(HairStyle.values()[0]).hairColor("#000000")
                .currentOutfit(mock(OutfitItem.class)).build();
        assertThat(ReflectionTestUtils.getField(fresh, "levelRule")).isEqualTo(LevelPolicy.RULE_VERSION);
    }

    @Test
    void 예전_규칙으로_모은_총_경험치를_되살린다() {
        assertThat(CharacterLevelMigration.legacyTotalExp(1, 40)).isEqualTo(40);
        assertThat(CharacterLevelMigration.legacyTotalExp(10, 0)).isEqualTo(900);           // 9 × 100
        assertThat(CharacterLevelMigration.legacyTotalExp(30, 50)).isEqualTo(6950);         // 900 + 20 × 300 + 50
        assertThat(CharacterLevelMigration.legacyTotalExp(50, 0)).isEqualTo(26900);          // 예전 만렙
    }

    @Test
    void 총_경험치를_새_규칙의_레벨로_옮기고_다시_옮기지_않게_표시한다() {
        Character beginner = legacy(10, 0);     // 900 EXP → 새 규칙 Lv.6 (70+120+170+220+270 = 850) 남은 50
        Character veteran = legacy(30, 50);     // 6950 EXP → 새 규칙 Lv.16
        Character maxed = legacy(50, 0);        // 26900 EXP → Lv.30
        when(characterRepository.findAllByLevelRuleIsNullOrLevelRuleLessThan(LevelPolicy.RULE_VERSION))
                .thenReturn(List.of(beginner, veteran, maxed));

        migration.run(null);

        assertThat(beginner.getLevel()).isEqualTo(6);
        assertThat(beginner.getExp()).isEqualTo(50);
        assertThat(veteran.getLevel()).isEqualTo(16);
        assertThat(maxed.getLevel()).isEqualTo(30);
        assertThat(maxed.getExp()).isZero();
        assertThat(List.of(beginner, veteran, maxed))
                .allSatisfy(c -> assertThat(ReflectionTestUtils.getField(c, "levelRule")).isEqualTo(LevelPolicy.RULE_VERSION));
        verify(characterRepository).saveAll(List.of(beginner, veteran, maxed));
    }

    @Test
    void 옮기기가_실패해도_서버_시작을_막지_않는다() {
        when(characterRepository.findAllByLevelRuleIsNullOrLevelRuleLessThan(LevelPolicy.RULE_VERSION))
                .thenThrow(new IllegalStateException("DB 오류"));

        assertThatCode(() -> migration.run(null)).doesNotThrowAnyException();
    }

    @Test
    void 무대_배경은_Lv1_10_20_30에_열린다() {
        assertThat(StageBackground.highestUnlockedAt(9)).isEqualTo(StageBackground.ROOKIE);
        assertThat(StageBackground.highestUnlockedAt(10)).isEqualTo(StageBackground.RISING);
        assertThat(StageBackground.highestUnlockedAt(20)).isEqualTo(StageBackground.PRO);
        assertThat(StageBackground.highestUnlockedAt(30)).isEqualTo(StageBackground.LEGEND);
    }
}
