package com.project.gainz;

import com.project.gainz.global.storage.FileLinks;
import com.project.gainz.global.storage.FileStore;
import com.project.gainz.global.storage.R2FileStore;
import com.project.gainz.global.storage.StorageQuota;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// 운영과 같은 설정(r2 저장소)으로 서버가 켜지는지 (#310). 단위 테스트는 객체를 직접 만들어서
// 생성자 고르기·빈 연결 같은 문제를 못 잡는다 (#308 배포 실패). DB는 H2, R2·메일은 네트워크 없이 만들기만 한다
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:context;MODE=PostgreSQL;NON_KEYWORDS=USER,CHARACTER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "jwt.secret=context-test-secret-context-test-secret-context-test-secret",
        "jwt.expiration=86400000",
        "gainz.mail.type=log",
        "gainz.storage.type=r2",
        "gainz.storage.r2.account-id=test",
        "gainz.storage.r2.bucket=test",
        "gainz.storage.r2.access-key-id=test",
        "gainz.storage.r2.secret-access-key=test",
        "gainz.storage.supabase.url=",
        "gainz.storage.supabase.service-key=",
        "spring.security.oauth2.client.registration.google.client-id=test",
        "spring.security.oauth2.client.registration.google.client-secret=test",
        "spring.security.oauth2.client.registration.kakao.client-id=test",
        "spring.security.oauth2.client.registration.kakao.client-secret=test",
        "spring.security.oauth2.client.registration.naver.client-id=test",
        "spring.security.oauth2.client.registration.naver.client-secret=test",
})
class ApplicationContextTest {

    @Autowired FileStore store;
    @Autowired FileLinks links;
    @Autowired StorageQuota quota;

    @Test
    void 운영처럼_r2_저장소로_서버가_켜진다() {
        assertThat(store).isInstanceOf(R2FileStore.class);
        assertThat(links).isNotNull();
        assertThat(quota).isNotNull();
    }
}
