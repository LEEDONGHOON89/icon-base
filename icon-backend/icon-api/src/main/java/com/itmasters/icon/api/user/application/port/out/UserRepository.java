package com.itmasters.icon.api.user.application.port.out;

import com.itmasters.icon.api.user.adapter.persistence.entity.UserEntity;

import java.util.List;
import java.util.Optional;

/**
 * User Entity의 영속성 포트 인터페이스
 * 헥사고날 아키텍처에서 Application -> Infrastructure 방향의 포트 역할
 */
public interface UserRepository {
    UserEntity save(UserEntity user);
    Optional<UserEntity> findById(String userId);
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByLoginId(String loginId);
    List<UserEntity> findAll();
    boolean existsByEmail(String email);
    void deleteById(String userId);
}