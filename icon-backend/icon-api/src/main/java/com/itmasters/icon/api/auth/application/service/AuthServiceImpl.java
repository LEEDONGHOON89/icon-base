package com.itmasters.icon.api.auth.application.service;

import com.itmasters.icon.api.auth.application.command.LoginCommand;
import com.itmasters.icon.api.auth.application.port.in.AuthService;
import com.itmasters.icon.api.auth.application.result.LoginResult;
import com.itmasters.icon.api.auth.adapter.persistence.entity.RefreshTokenEntity;
import com.itmasters.icon.api.common.exception.LoginFailedException;
import com.itmasters.icon.api.user.application.port.out.UserRepository;
import com.itmasters.icon.api.user.adapter.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenManager tokenManager;

    @Override
    public LoginResult login(LoginCommand command) {
        // 1. 로그인 ID로 사용자 조회
        UserEntity user = userRepository.findByLoginId(command.getLoginId())
                .orElseThrow(() -> new LoginFailedException("잘못된 로그인 ID 또는 비밀번호"));

        // 2. 비밀번호 비교
        if (!passwordEncoder.matches(command.getPassword(), user.getPassword())) {
            throw new LoginFailedException("잘못된 로그인 ID 또는 비밀번호");
        }

        // 3. 인증 성공 시 액세스 토큰 및 리프레시 토큰 생성 및 저장
        String accessToken = tokenManager.generateAccessToken(user.getUserId());
        RefreshTokenEntity refreshToken = tokenManager.generateAndSaveRefreshToken(user);

        return new LoginResult(accessToken, refreshToken.getToken(), "로그인 성공");
    }
}
