package com.itmasters.icon.api.auth.adapter.in.web;

import com.itmasters.icon.api.auth.adapter.in.web.dto.*;
import com.itmasters.icon.api.auth.application.command.LoginCommand;
import com.itmasters.icon.api.auth.application.port.in.AuthService;
import com.itmasters.icon.api.auth.application.result.LoginResult;
import com.itmasters.icon.api.auth.application.service.TokenManager;
import com.itmasters.icon.api.auth.adapter.persistence.entity.RefreshTokenEntity;
import com.itmasters.icon.api.user.application.port.in.UserService;
import com.itmasters.icon.api.user.application.result.UserResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Auth", description = "인증")
@RestController
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final TokenManager tokenManager;
  private final ModelMapper modelMapper;
  private final UserService userService;

  @PostMapping("/api/v1/auth/login")
  public LoginResponse login(@RequestBody @Validated LoginRequest loginRequest) {
    LoginCommand command = loginRequest.toCommand();
    LoginResult result = authService.login(command);
    return modelMapper.map(result, LoginResponse.class);
  }

  @PostMapping("/api/v1/auth/refresh")
  public LoginResponse refresh(@RequestBody RefreshTokenRequest request) {
    RefreshTokenEntity refreshToken = tokenManager.verifyRefreshToken(request.getRefreshToken());
    String newAccessToken = tokenManager.generateAccessToken(refreshToken.getUserId());
    return new LoginResponse(newAccessToken, refreshToken.getToken(), "토큰 갱신 성공");
  }

  @PostMapping("/api/v1/auth/logout")
  public void logout(@RequestBody LogoutRequest request) {
    tokenManager.deleteRefreshTokenByToken(request.getRefreshToken());
    //        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @GetMapping("/api/v1/auth/me")
  public UserInfoResponse getCurrentUser(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.");
    }

    String userId = authentication.getName();
    UserResult userResult = userService.getCurrentUser(userId);
    if (userResult == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
    }

    return new UserInfoResponse(
        userResult.getUserId(),
        userResult.getLoginId(),
        userResult.getUserName(),
        userResult.getRole()
    );
  }
}
