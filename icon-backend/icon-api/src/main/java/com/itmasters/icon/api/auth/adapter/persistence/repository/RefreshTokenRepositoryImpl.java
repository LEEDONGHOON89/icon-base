package com.itmasters.icon.api.auth.adapter.persistence.repository;

import com.itmasters.icon.api.auth.adapter.persistence.entity.RefreshTokenEntity;
import com.itmasters.icon.common.domain.EntityType;
import com.itmasters.icon.common.domain.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final JpaRefreshTokenRepository jpaRefreshTokenRepository;
    private final IdGenerator idGenerator;

    @Override
    public RefreshTokenEntity save(RefreshTokenEntity refreshToken) {
        if (refreshToken.getId() == null) {
            refreshToken.assignId(idGenerator.generateId(EntityType.REFRESH_TOKEN));
        }
        return jpaRefreshTokenRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshTokenEntity> findByToken(String token) {
        return jpaRefreshTokenRepository.findByToken(token);
    }

    @Override
    public void delete(RefreshTokenEntity refreshToken) {
        jpaRefreshTokenRepository.delete(refreshToken);
    }

    @Override
    public Optional<RefreshTokenEntity> findByUserId(String userId) {
        return jpaRefreshTokenRepository.findByUserId(userId);
    }
}
