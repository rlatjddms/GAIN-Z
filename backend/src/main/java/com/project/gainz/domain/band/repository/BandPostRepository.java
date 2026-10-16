package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.BandPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.time.Instant;
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
    Optional<BandPost> findByIdAndBandId(Long id, Long bandId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from BandPost p where p.id = :id and p.band.id = :bandId")
    Optional<BandPost> findForUpdate(@Param("id") Long id, @Param("bandId") Long bandId);
}
