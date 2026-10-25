package com.project.gainz.domain.user.service;

import com.project.gainz.domain.user.dto.response.TokenResponse;
import com.project.gainz.domain.user.entity.OAuthLoginCode;
import com.project.gainz.domain.user.repository.OAuthLoginCodeRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

// 소셜 로그인 한 번용 코드 (#281): 로그인이 끝나면 토큰을 맡겨 두고 코드를 주고, 프론트가 그 코드로 한 번만 찾아간다
@Service
@RequiredArgsConstructor
public class OAuthLoginCodeService {

    private final OAuthLoginCodeRepository codeRepository;
    private final SecureRandom random = new SecureRandom();

    // 토큰을 맡기고 주소에 실을 코드를 돌려준다 (Redis에 1분)
    public String issue(String accessToken, String refreshToken) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        codeRepository.save(new OAuthLoginCode(AccountMailService.hash(raw), accessToken, refreshToken));
        return raw;
    }

    // 코드를 토큰으로. 한 번 쓰면 지운다. 없거나 1분이 지났으면 다시 로그인해야 한다
    public TokenResponse exchange(String rawCode) {
        String id = AccountMailService.hash(rawCode.trim());
        OAuthLoginCode code = codeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_OAUTH_CODE));
        codeRepository.deleteById(id);
        return new TokenResponse(code.getAccessToken(), code.getRefreshToken());
    }
}
