package com.project.gainz.domain.character.repository;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.enums.OutfitCategory;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.global.common.enums.InstrumentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

// 외부 DB 없이 이름이 닉네임과 다른 회원 찾기(#227)를 검증한다.
@DataJpaTest(properties = "spring.config.location=classpath:band-board-test.properties", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CharacterRepositoryTest.Config.class)
class CharacterRepositoryTest {
    @Configuration
    @EnableJpaAuditing
    @EntityScan("com.project.gainz.domain")
    @EnableJpaRepositories("com.project.gainz.domain.character.repository")
    static class Config {}

    @Autowired TestEntityManager em;
    @Autowired CharacterRepository characters;

    @Test void 이름이_닉네임과_다른_회원만_찾고_탈퇴_회원은_뺀다() {
        OutfitItem outfit = em.persist(OutfitItem.builder().name("기본").category(OutfitCategory.CLOTHES).requiredLevel(1).spriteKey("DEFAULT").build());
        Character old = character(user("old", "옛 이름"), "Thunder", outfit);
        character(user("same", "Bolt"), "Bolt", outfit);
        User gone = user("gone", "탈퇴한 사용자");
        character(gone, "Ghost", outfit);
        gone.withdraw();
        em.flush();
        em.clear();

        assertThat(characters.findAllWithNameNotNickname()).extracting(Character::getId).containsExactly(old.getId());
    }

    private User user(String email, String name) {
        return em.persist(User.builder().email(email + "@test.invalid").name(name).provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build());
    }

    private Character character(User user, String nickname, OutfitItem outfit) {
        return em.persist(Character.builder().user(user).nickname(nickname).instrumentType(InstrumentType.DRUM).currentOutfit(outfit)
                .gender(Gender.MALE).hairStyle(HairStyle.SHORT_CUT).hairColor("#000000").instrumentColor("#ff0000").build());
    }
}
