package com.project.gainz.domain.band.enums;

// 브라우저가 R2에 바로 올리는 밴드 파일 종류 (#262). 종류마다 크기 한도와 받는 형식이 다르다 (BandAttachmentStorage)
public enum BandUploadKind {
    // 게시판 첨부 (사진·영상·오디오·PDF)
    ATTACHMENT,
    // 셋리스트 곡 첨부 (형식은 게시판과 같고 한도가 작다)
    SETLIST_FILE,
    // 레이어 잼 녹음 트랙
    JAM_TRACK
}
