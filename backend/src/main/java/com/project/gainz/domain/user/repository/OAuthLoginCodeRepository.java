package com.project.gainz.domain.user.repository;

import com.project.gainz.domain.user.entity.OAuthLoginCode;
import org.springframework.data.repository.CrudRepository;

public interface OAuthLoginCodeRepository extends CrudRepository<OAuthLoginCode, String> {
}
