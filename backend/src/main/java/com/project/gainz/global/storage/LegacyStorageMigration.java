package com.project.gainz.global.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

// Supabase Storage에 있던 파일을 R2로 한 번 옮긴다 (#262). 서버가 켜질 때 뒤에서 돌고, 이미 R2에 있는 파일은 건너뛴다.
// 옮기는 동안에도 R2FileStore가 R2에 없는 파일은 Supabase에서 읽어서 사용자는 차이를 모른다.
// 다 옮겼다는 로그가 나오면 운영 설정에서 SUPABASE_URL·SUPABASE_SERVICE_KEY를 지워도 된다 (Supabase 원본은 지우지 않는다)
@Slf4j
@Component
@ConditionalOnProperty(name = "gainz.storage.type", havingValue = "r2")
@RequiredArgsConstructor
public class LegacyStorageMigration implements ApplicationRunner {

    // 저장소마다 쓰는 폴더 (SheetFileStorage·ProfileImageStorage·BandAttachmentStorage)
    static final List<String> FOLDERS = List.of("sheets", "profiles", "band");

    private final R2FileStore store;

    @Override
    public void run(ApplicationArguments args) {
        if (store.legacy() == null) return;
        Thread thread = new Thread(this::migrate, "legacy-storage-migration");
        thread.setDaemon(true);
        thread.start();
    }

    // 옮긴 개수. 실패한 파일은 남겨 두고 다음에 서버가 켜질 때 다시 해 본다
    int migrate() {
        SupabaseFileStore legacy = store.legacy();
        int moved = 0, failed = 0;
        for (String folder : FOLDERS) {
            List<String> keys;
            try {
                keys = legacy.list(folder);
            } catch (Exception e) {
                log.warn("Supabase 파일 옮기기: {} 목록을 못 받았어요 ({})", folder, e.getMessage());
                failed++;
                continue;
            }
            for (String key : keys) {
                try {
                    if (store.inR2(key)) continue;
                    Resource source = legacy.get(key);
                    if (source == null) continue;
                    try (InputStream in = source.getInputStream()) {
                        store.put(key, in, source.contentLength(), "application/octet-stream");
                    }
                    moved++;
                } catch (Exception e) {
                    log.warn("Supabase 파일 옮기기 실패: {} ({})", key, e.getMessage());
                    failed++;
                }
            }
        }
        if (failed == 0) {
            log.info("Supabase 파일 옮기기 끝: 이번에 {}개 옮김. 이제 SUPABASE_URL·SUPABASE_SERVICE_KEY를 지워도 돼요", moved);
        } else {
            log.warn("Supabase 파일 옮기기: {}개 옮김, {}개 실패. 다음에 서버가 켜질 때 다시 해 봐요", moved, failed);
        }
        return moved;
    }
}
