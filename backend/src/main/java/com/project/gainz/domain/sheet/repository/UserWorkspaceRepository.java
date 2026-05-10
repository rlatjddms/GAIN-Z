package com.project.gainz.domain.sheet.repository;

import com.project.gainz.domain.sheet.entity.UserWorkspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWorkspaceRepository extends JpaRepository<UserWorkspace,Long> {
}
