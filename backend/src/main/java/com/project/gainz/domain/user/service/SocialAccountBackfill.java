package com.project.gainz.domain.user.service;

import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 연결 표(#189)가 생기기 전에 소셜로 가입한 사용자에게 그 소셜 연결을 만든다 (고유 ID는 다음 소셜 로그인 때 채움).
// 서버가 뜰 때마다 확인하지만 한 번 채우면 대상이 없다
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialAccountBackfill {

    private final SocialAccountRepository socialAccountRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfill() {
        List<User> users = socialAccountRepository.findSocialUsersWithoutAccount();
        users.forEach(user -> user.addSocialAccount(user.getProvider(), null));
        if (!users.isEmpty()) {
            log.info("소셜 연결 {}개를 채웠어요 (#189)", users.size());
        }
    }
}
