package com.project.gainz.domain.user.repository;

import com.project.gainz.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository  extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
}
