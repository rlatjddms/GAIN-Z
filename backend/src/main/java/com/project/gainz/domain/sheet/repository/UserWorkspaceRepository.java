package com.project.gainz.domain.sheet.repository;

import com.project.gainz.domain.sheet.entity.UserWorkspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserWorkspaceRepository extends JpaRepository<UserWorkspace,Long> {

    @Query("select w from UserWorkspace w join fetch w.sheetMusic where w.user.id = :userId order by w.id desc")
    List<UserWorkspace> findAllWithSheetByUserId(@Param("userId") Long userId);

    @Query("select w from UserWorkspace w join fetch w.sheetMusic where w.id = :id and w.user.id = :userId")
    Optional<UserWorkspace> findWithSheetByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    boolean existsByUserIdAndSheetMusicId(Long userId, Long sheetId);

    boolean existsBySheetMusicId(Long sheetId);

    long countByUserId(Long userId);

    // 셋리스트에 연결된 악보 중 내 보관함에 담긴 것
    @Query("select w from UserWorkspace w where w.user.id = :userId and w.sheetMusic.id in :sheetIds")
    List<UserWorkspace> findAllByUserIdAndSheetIds(@Param("userId") Long userId, @Param("sheetIds") Collection<Long> sheetIds);
}
