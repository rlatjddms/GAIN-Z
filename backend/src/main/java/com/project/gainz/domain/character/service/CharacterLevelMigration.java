package com.project.gainz.domain.character.service;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.enums.LevelPolicy;
import com.project.gainz.domain.character.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// 레벨 규칙이 바뀐 뒤 처음 시작할 때, 예전 규칙으로 쌓은 레벨·경험치를 총 경험치로 되살려 새 규칙의 레벨로 옮긴다 (한 번만)
@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterLevelMigration implements ApplicationRunner {

    private final CharacterRepository characterRepository;

    // 실패해도 서버는 뜨고 다음 시작 때 다시 시도한다 (옮긴 캐릭터는 levelRule로 표시돼 다시 옮기지 않음)
    @Override
    public void run(ApplicationArguments args) {
        try {
            List<Character> characters = characterRepository.findAllByLevelRuleIsNullOrLevelRuleLessThan(LevelPolicy.RULE_VERSION);
            for (Character character : characters) {
                long total = legacyTotalExp(character.getLevel(), character.getExp());
                int[] next = LevelPolicy.levelForTotalExp(total);
                character.migrateLevel(next[0], next[1], LevelPolicy.RULE_VERSION);
            }
            characterRepository.saveAll(characters);
            if (!characters.isEmpty()) log.info("새 레벨 규칙으로 캐릭터 {}명 옮김", characters.size());
        } catch (Exception e) {
            log.warn("레벨 규칙 옮기기 실패 ({})", e.getMessage());
        }
    }

    // 예전 규칙(최대 Lv.50, Lv.1~9 100 · Lv.10~29 300 · Lv.30~49 1000)으로 쌓은 총 경험치
    static long legacyTotalExp(Integer level, Integer exp) {
        int safeLevel = level == null ? 1 : Math.max(1, Math.min(level, 50));
        long total = 0;
        for (int l = 1; l < safeLevel; l++) total += l <= 9 ? 100 : l <= 29 ? 300 : 1000;
        return total + Math.max(exp == null ? 0 : exp, 0);
    }
}
