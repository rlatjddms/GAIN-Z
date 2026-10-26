package com.project.gainz.domain.band.service;

import com.project.gainz.domain.band.dto.request.BandUploadRequest;
import com.project.gainz.domain.band.dto.response.BandUploadResponse;
import com.project.gainz.domain.band.repository.BandMemberRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 밴드 파일을 브라우저가 저장소에 바로 올리는 주소 (#262). 밴드 멤버만 받을 수 있다
@Service
@RequiredArgsConstructor
public class BandUploadService {

    private final BandMemberRepository members;
    private final BandAttachmentStorage storage;

    @Transactional(readOnly = true)
    public BandUploadResponse.Started start(Long userId, Long bandId, BandUploadRequest.Start request) {
        members.findByBandIdAndUserId(bandId, userId)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()) && !Boolean.TRUE.equals(m.getBand().getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_BAND_MEMBER));
        return storage.start(userId, bandId, request.kind(), request.name(), request.size(), request.contentType());
    }
}
