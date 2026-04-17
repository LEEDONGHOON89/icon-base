package com.itmasters.icon.api.auth.application.service;

import com.itmasters.icon.api.auth.application.util.JwtTokenProvider;
import com.itmasters.icon.api.auth.adapter.persistence.repository.RefreshTokenRepository;
import com.itmasters.icon.api.auth.adapter.persistence.entity.RefreshTokenEntity;
import com.itmasters.icon.api.common.exception.LoginFailedException;
import com.itmasters.icon.api.user.adapter.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenManager {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public String generateAccessToken(String loginId) {
        return jwtTokenProvider.generateAccessToken(loginId);
    }

    @Transactional
    public RefreshTokenEntity generateAndSaveRefreshToken(UserEntity user) {
        String token = jwtTokenProvider.generateRefreshToken();
        Date expiryDate = jwtTokenProvider.getExpirationDateFromToken(token);
        LocalDateTime expireDt = expiryDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        Optional<RefreshTokenEntity> existingTokenOpt = refreshTokenRepository.findByUserId(user.getUserId());
        RefreshTokenEntity refreshToken;

        if (existingTokenOpt.isPresent()) {
            refreshToken = existingTokenOpt.get();
            refreshToken.updateToken(token, expireDt);
        } else {
            refreshToken = RefreshTokenEntity.create(token, user.getUserId(), expireDt);
        }

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshTokenEntity verifyRefreshToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new LoginFailedException("유효하지 않거나 만료된 리프레시 토큰입니다.");
        }
        // 데이터베이스에서 토큰 조회
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new LoginFailedException("리프레시 토큰을 찾을 수 없습니다."));
    }


    @Transactional
    public void deleteRefreshTokenByToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void deleteRefreshTokenByUserId(String userId) {
        refreshTokenRepository.findByUserId(userId).ifPresent(refreshTokenRepository::delete);
    }
}
