package com.itmasters.icon.api.user.adapter.persistence.repository;

import com.itmasters.icon.api.user.adapter.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserRepository extends JpaRepository<UserEntity, String> {
    boolean existsByEmail(String email);
}
