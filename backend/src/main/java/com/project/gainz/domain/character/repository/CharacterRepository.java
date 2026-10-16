package com.project.gainz.domain.character.repository;

import com.project.gainz.domain.character.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Long> {

    Optional<Character> findByUserId(Long userId);
    boolean existsByNickname(String nickname);

    List<Character> findAllByUserIdIn(Collection<Long> userIds);
}
