package com.itmasters.icon.api.auth.application.command;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginCommand {
  private String loginId;
  private String password;

  public LoginCommand(String loginId, String password) {
    // 로그인 아이디와 비밀번호가 비어있는지 검증
    if (loginId == null || loginId.trim().isEmpty()) {
      // 한글 메시지로 예외 처리
      throw new IllegalArgumentException("로그인 아이디는 필수 입력값입니다.");
    }
    if (password == null || password.trim().isEmpty()) {
      throw new IllegalArgumentException("비밀번호는 필수 입력값입니다.");
    }
    this.loginId = loginId;
    this.password = password;
  }
}
