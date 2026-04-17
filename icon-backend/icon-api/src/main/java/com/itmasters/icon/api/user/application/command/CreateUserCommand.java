package com.itmasters.icon.api.user.application.command;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateUserCommand {
  private String loginId;
  private String password;
  private String userName;
  private String email;
  private String description;

  public CreateUserCommand(String loginId, String password, String userName, String email, String description) {
    if (loginId == null || loginId.trim().isEmpty()) {
      throw new IllegalArgumentException("로그인 ID는 필수 입력값입니다.");
    }
    if (password == null || password.trim().isEmpty()) {
      throw new IllegalArgumentException("비밀번호는 필수 입력값입니다.");
    }
    if (userName == null || userName.trim().isEmpty()) {
      throw new IllegalArgumentException("이름은 필수 입력값입니다.");
    }
    
    this.loginId = loginId;
    this.password = password;
    this.userName = userName;
    this.email = email;
    this.description = description;
  }
}