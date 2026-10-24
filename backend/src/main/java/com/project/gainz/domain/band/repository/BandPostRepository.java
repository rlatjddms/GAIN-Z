package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.BandPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.time.Instant;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface BandPostRepository extends JpaRepository<BandPost, Long> {
    @Query("""
            select p from BandPost p where p.band.id = :bandId
            and (:mine = false or p.author.id = :userId)
            and (:notice is null
                or (:notice = true and p.isNotice = true and (p.noticeExpiresAt is null or p.noticeExpiresAt > :now))
                or (:notice = false and (p.isNotice = false or p.noticeExpiresAt <= :now)))
            order by case when p.isNotice = true and (p.noticeExpiresAt is null or p.noticeExpiresAt > :now) then 0 else 1 end,
                p.createdAt desc, p.id desc
            """)
    Page<BandPost> findVisible(@Param("bandId") Long bandId, @Param("userId") Long userId,
                              @Param("mine") boolean mine, @Param("notice") Boolean notice,
                              @Param("now") Instant now, Pageable pageable);
    // 검색 (#188): keyword는 소문자로 바꾸고 %·_·!를 !로 이스케이프한 '%검색어%' (빈 문자열이면 검색어 조건 없음).
    // 작성자는 캐릭터 닉네임과 이름 둘 다 찾는다. 작성일은 [from, to)
    @Query("""
            select p from BandPost p where p.band.id = :bandId
            and (:mine = false or p.author.id = :userId)
            and (:notice is null
                or (:notice = true and p.isNotice = true and (p.noticeExpiresAt is null or p.noticeExpiresAt > :now))
                or (:notice = false and (p.isNotice = false or p.noticeExpiresAt <= :now)))
            and p.createdAt >= :from and p.createdAt < :to
            and (:keyword = ''
                or lower(p.title) like :keyword escape '!'
                or lower(p.content) like :keyword escape '!'
                or lower(p.author.name) like :keyword escape '!'
                or exists (select c.id from Character c where c.user = p.author and lower(c.nickname) like :keyword escape '!'))
            order by case when p.isNotice = true and (p.noticeExpiresAt is null or p.noticeExpiresAt > :now) then 0 else 1 end,
                p.createdAt desc, p.id desc
            """)
    Page<BandPost> search(@Param("bandId") Long bandId, @Param("userId") Long userId,
                          @Param("mine") boolean mine, @Param("notice") Boolean notice, @Param("now") Instant now,
                          @Param("keyword") String keyword, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                          Pageable pageable);

    Optional<BandPost> findByIdAndBandId(Long id, Long bandId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from BandPost p where p.id = :id and p.band.id = :bandId")
    Optional<BandPost> findForUpdate(@Param("id") Long id, @Param("bandId") Long bandId);
}
