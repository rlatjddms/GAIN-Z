package com.project.gainz.domain.friend.repository;

import com.project.gainz.domain.friend.entity.Friendship;
import com.project.gainz.domain.friend.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // 받은 신청 / 보낸 신청 (최근 순)
    List<Friendship> findAllByReceiverIdAndStatusOrderByIdDesc(Long receiverId, FriendshipStatus status);

    List<Friendship> findAllByRequesterIdAndStatusOrderByIdDesc(Long requesterId, FriendshipStatus status);

    // 내 친구 관계 (내가 신청했든 받았든)
    @Query("select f from Friendship f where f.status = :status and (f.requester.id = :userId or f.receiver.id = :userId)")
    List<Friendship> findAllByUserIdAndStatus(@Param("userId") Long userId, @Param("status") FriendshipStatus status);

    @Query("select count(f) from Friendship f where f.status = :status and (f.requester.id = :userId or f.receiver.id = :userId)")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") FriendshipStatus status);

    // 두 사람 사이의 관계 (어느 쪽이 신청했든)
    @Query("select f from Friendship f where (f.requester.id = :a and f.receiver.id = :b) or (f.requester.id = :b and f.receiver.id = :a)")
    List<Friendship> findAllBetween(@Param("a") Long a, @Param("b") Long b);

    // 회원 탈퇴할 때 정리
    @Query("select f from Friendship f where f.requester.id = :userId or f.receiver.id = :userId")
    List<Friendship> findAllByUserId(@Param("userId") Long userId);
}
