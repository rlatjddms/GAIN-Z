package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.SetlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SetlistItemRepository extends JpaRepository<SetlistItem, Long> {

    // 내가 속한 밴드의 셋리스트에 연결된 악보인지 (밴드 멤버에게 악보 보기를 열어 준다)
    @Query("""
            select count(i) > 0 from SetlistItem i
            join i.setlist s
            join BandMember m on m.band = s.band
            where i.sheetMusic.id = :sheetId and m.user.id = :userId
            """)
    boolean existsLinkedForMember(@Param("sheetId") Long sheetId, @Param("userId") Long userId);

    boolean existsBySheetMusicId(Long sheetId);
}
