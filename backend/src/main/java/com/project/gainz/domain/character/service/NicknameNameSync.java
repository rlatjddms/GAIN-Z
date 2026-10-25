package com.project.gainz.domain.character.service;

import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// 이름을 닉네임 하나로 통일하기 전(#227)에 가입한 회원: 시작할 때 회원 이름을 캐릭터 닉네임으로 맞춘다
// 이미 같은 회원은 다시 찾지 않으니 몇 번 시작해도 괜찮다
@Slf4j
@Component
@RequiredArgsConstructor
public class NicknameNameSync implements ApplicationRunner {

    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;

    // 실패해도 서버는 뜨고 다음 시작 때 다시 시도한다
    @Override
    public void run(ApplicationArguments args) {
        try {
            List<User> users = characterRepository.findAllWithNameNotNickname().stream()
                    .map(character -> character.getUser().update(character.getNickname()))
                    .toList();
            userRepository.saveAll(users);
            if (!users.isEmpty()) log.info("회원 {}명의 이름을 닉네임으로 맞춤", users.size());
        } catch (Exception e) {
            log.warn("이름을 닉네임으로 맞추기 실패 ({})", e.getMessage());
        }
    }
}
