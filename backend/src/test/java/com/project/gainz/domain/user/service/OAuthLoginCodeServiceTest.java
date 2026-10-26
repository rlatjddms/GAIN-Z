package com.project.gainz.domain.user.service;

import com.project.gainz.domain.user.dto.response.TokenResponse;
import com.project.gainz.domain.user.entity.OAuthLoginCode;
import com.project.gainz.domain.user.repository.OAuthLoginCodeRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 소셜 로그인 한 번용 코드 (#281)
@ExtendWith(MockitoExtension.class)
class OAuthLoginCodeServiceTest {

    @Mock OAuthLoginCodeRepository codeRepository;
    @InjectMocks OAuthLoginCodeService service;

    @Test void issuedCodeIsStoredOnlyAsHashAndExchangedOnce() {
        String raw = service.issue("access", "refresh");

        ArgumentCaptor<OAuthLoginCode> saved = ArgumentCaptor.forClass(OAuthLoginCode.class);
        verify(codeRepository).save(saved.capture());
        assertThat(raw).hasSizeGreaterThanOrEqualTo(40);
        assertThat(saved.getValue().getId()).isEqualTo(AccountMailService.hash(raw)).isNotEqualTo(raw);

        when(codeRepository.findById(saved.getValue().getId())).thenReturn(Optional.of(saved.getValue()));
        TokenResponse tokens = service.exchange(raw);

        assertThat(tokens.accessToken()).isEqualTo("access");
        assertThat(tokens.refreshToken()).isEqualTo("refresh");
        verify(codeRepository).deleteById(saved.getValue().getId());
    }

    @Test void unknownOrExpiredCodeAsksToLogInAgain() {
        assertThatThrownBy(() -> service.exchange("old"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_OAUTH_CODE);
    }
}
