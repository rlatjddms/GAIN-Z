package com.project.gainz.domain.character.service;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.common.enums.InstrumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NicknameNameSyncTest {

    @Mock private CharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private NicknameNameSync sync;

    @Test
    void 예전_회원의_이름을_닉네임으로_맞춘다() {
        User user = User.builder().email("me@test.invalid").name("옛 이름").build();
        Character character = Character.builder().user(user).nickname("Thunder").instrumentType(InstrumentType.DRUM)
                .instrumentColor("#000000").gender(Gender.values()[0]).hairStyle(HairStyle.values()[0]).hairColor("#000000")
                .currentOutfit(mock(OutfitItem.class)).build();
        when(characterRepository.findAllWithNameNotNickname()).thenReturn(List.of(character));

        sync.run(null);

        assertThat(user.getName()).isEqualTo("Thunder");
        verify(userRepository).saveAll(List.of(user));
    }

    @Test
    void 실패해도_서버_시작을_막지_않는다() {
        when(characterRepository.findAllWithNameNotNickname()).thenThrow(new IllegalStateException("db"));

        assertThatCode(() -> sync.run(null)).doesNotThrowAnyException();
    }
}
