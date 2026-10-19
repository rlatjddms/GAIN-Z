package com.project.gainz.domain.friend.dto.response;

import java.time.LocalDateTime;

// 친구 신청. 받은 신청이면 user는 보낸 사람, 보낸 신청이면 받는 사람
public record FriendRequestResponse(
        Long requestId,
        FriendUserResponse user,
        LocalDateTime createdAt
) {
}
