package com.project.gainz.domain.band.entity;

import com.project.gainz.domain.band.enums.BandUploadKind;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

// 브라우저가 R2(tmp/{id})에 바로 올리는 중인 파일 (#262). 글·곡·트랙을 저장할 때 id로 가져가면(BandAttachmentStorage.claim) 지운다.
// 1시간 안에 안 가져가면 이 기록은 사라지고, 버킷의 tmp/ 파일은 R2 수명 규칙(하루)이 지운다 (docs/deploy.md)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "bandUpload", timeToLive = 3600)
public class BandUpload {

    @Id
    private String id;

    private Long userId;

    private Long bandId;

    private BandUploadKind kind;

    // 화면에 보일 원래 파일 이름 (정리한 값)
    private String name;
}
