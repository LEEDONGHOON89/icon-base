package com.itmasters.icon.api.user.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {
  @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
  private String password;

  @NotBlank(message = "이름은 필수입니다.")
  private String userName;

  private String email;
  private String description;

  // public UserCommand toCommand() { ... } // TODO: 구현 예정
}
