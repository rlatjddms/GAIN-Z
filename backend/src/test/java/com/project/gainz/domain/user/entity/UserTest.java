package com.project.gainz.domain.user.entity;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.user.dto.response.MyInfoResponse;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.global.common.enums.InstrumentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UserTest {

    private User user() {
        User user = User.builder().email("drummer@test.invalid").password("pw")
                .provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    @Test
    void 보이는_이름은_캐릭터_닉네임이고_캐릭터가_없으면_이메일_앞부분() {
        User user = user();
        Character character = Character.builder().user(user).nickname("쿵").instrumentType(InstrumentType.DRUM)
                .instrumentColor("#000000").gender(Gender.values()[0]).hairStyle(HairStyle.values()[0]).hairColor("#000000")
                .currentOutfit(mock(OutfitItem.class)).build();

        assertThat(MyInfoResponse.displayName(user, character)).isEqualTo("쿵");
        assertThat(MyInfoResponse.displayName(user, null)).isEqualTo("drummer");
    }

    @Test
    void 탈퇴한_회원은_탈퇴한_사용자로_보인다() {
        User user = user();
        user.withdraw();

        assertThat(user.nameWithoutCharacter()).isEqualTo(User.WITHDRAWN_NAME);
    }
}
