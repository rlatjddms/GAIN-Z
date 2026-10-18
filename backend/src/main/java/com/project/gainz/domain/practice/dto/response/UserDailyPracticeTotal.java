package com.project.gainz.domain.practice.dto.response;

import java.time.LocalDate;

// 사람별·날짜별 연습 합계 (직접 수정한 차이분까지 더한 값)
public record UserDailyPracticeTotal(
        Long userId,
        LocalDate date,
        Long seconds
) {
}
