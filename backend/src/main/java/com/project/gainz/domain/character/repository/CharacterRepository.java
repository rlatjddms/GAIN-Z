package com.project.gainz.domain.character.repository;

import com.project.gainz.domain.character.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Long> {

    Optional<Character> findByUserId(Long userId);
    boolean existsByNickname(String nickname);

    List<Character> findAllByUserIdIn(Collection<Long> userIds);

    // 아직 지금 레벨 규칙으로 옮기지 않은 캐릭터
    List<Character> findAllByLevelRuleIsNullOrLevelRuleLessThan(Integer ruleVersion);

    // 회원 이름이 닉네임과 다른 캐릭터 (#227: 이름을 닉네임으로 맞추기 전에 가입한 회원, 탈퇴 회원은 빼고)
    @Query("select c from Character c join fetch c.user u where u.isDeleted = false and u.name <> c.nickname")
    List<Character> findAllWithNameNotNickname();
}
