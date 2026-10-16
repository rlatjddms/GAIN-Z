package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.BandPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface BandPostRepository extends JpaRepository<BandPost, Long> {
    Page<BandPost> findByBandIdOrderByIsNoticeDescCreatedAtDescIdDesc(Long bandId, Pageable pageable);
    Optional<BandPost> findByIdAndBandId(Long id, Long bandId);
    Page<BandPost> findByBandIdAndIsNoticeOrderByCreatedAtDescIdDesc(Long bandId, boolean notice, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from BandPost p where p.id = :id and p.band.id = :bandId")
    Optional<BandPost> findForUpdate(@Param("id") Long id, @Param("bandId") Long bandId);
}
