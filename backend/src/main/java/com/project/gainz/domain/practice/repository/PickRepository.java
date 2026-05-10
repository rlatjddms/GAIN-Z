package com.project.gainz.domain.practice.repository;

import com.project.gainz.domain.practice.entity.Pick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PickRepository extends JpaRepository<Pick, Long> {
}
