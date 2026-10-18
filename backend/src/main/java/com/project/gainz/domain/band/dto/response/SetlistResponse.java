package com.project.gainz.domain.band.dto.response;

import com.project.gainz.domain.band.dto.response.BandBoardResponse.Attachment;
import java.time.LocalDate;
import java.util.List;

public final class SetlistResponse {
    private SetlistResponse() {}

    public record Setlist(Long id, String title, boolean favorite, List<Item> items) {}

    public record Item(Long id, String title, String artist, String memo, LinkedSheet sheet,
                       List<Long> memberIds, List<Attachment> attachments) {}

    // workspaceItemId: 보는 사람 보관함에 있으면 그 항목 id (없으면 null → 담은 뒤 볼 수 있다)
    public record LinkedSheet(Long sheetId, String title, String artist, Long workspaceItemId) {}

    public record PracticeWeek(LocalDate from, LocalDate to, List<MemberPractice> members) {}

    public record MemberPractice(Long memberId, Long userId, long practiceSeconds, long practiceDays,
                                 int routineTotal, int routineCompleted) {}
}
