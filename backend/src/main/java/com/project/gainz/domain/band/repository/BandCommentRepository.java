package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.BandComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BandCommentRepository extends JpaRepository<BandComment, Long> {
    Optional<BandComment> findByIdAndPostId(Long id, Long postId);
    // 댓글 쪽 나누기 (#188): 원댓글을 최근 것부터, 답글은 원댓글 ID로 따로
    @Query("select c from BandComment c where c.post.id = :postId and c.parent is null order by c.createdAt desc, c.id desc")
    Page<BandComment> findRootPage(@Param("postId") Long postId, Pageable pageable);
    List<BandComment> findAllByParentIdInOrderByCreatedAtAscIdAsc(Collection<Long> parentIds);
    boolean existsByParentId(Long parentId);
    long countByPostIdAndContentDeletedFalse(Long postId);
}
