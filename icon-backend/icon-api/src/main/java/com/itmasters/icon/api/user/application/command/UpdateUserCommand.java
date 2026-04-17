package com.itmasters.icon.api.user.application.command;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateUserCommand {
  private String userId;
  private String userName;
  private String email;
  private String description;

  public UpdateUserCommand(String userId, String userName, String email, String description) {
    if (userId == null || userId.trim().isEmpty()) {
      throw new IllegalArgumentException("사용자 ID는 필수 입력값입니다.");
    }
    if (userName == null || userName.trim().isEmpty()) {
      throw new IllegalArgumentException("이름은 필수 입력값입니다.");
    }
    
    this.userId = userId;
    this.userName = userName;
    this.email = email;
    this.description = description;
  }
}