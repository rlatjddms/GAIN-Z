package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.BandComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BandCommentRepository extends JpaRepository<BandComment, Long> {
    List<BandComment> findAllByPostIdOrderByCreatedAtAscIdAsc(Long postId);
    Optional<BandComment> findByIdAndPostId(Long id, Long postId);
    boolean existsByParentId(Long parentId);
    long countByPostIdAndContentDeletedFalse(Long postId);
}
