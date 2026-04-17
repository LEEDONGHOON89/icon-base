package com.itmasters.icon.api.auth.adapter.persistence.repository;

import com.itmasters.icon.api.auth.adapter.persistence.entity.RefreshTokenEntity;
import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshTokenEntity save(RefreshTokenEntity refreshToken);
    Optional<RefreshTokenEntity> findByToken(String token);
    void delete(RefreshTokenEntity refreshToken);
    Optional<RefreshTokenEntity> findByUserId(String userId);
}
