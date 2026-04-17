package com.itmasters.icon.api.user.adapter.in.web.dto;

import com.itmasters.icon.api.user.application.command.CreateUserCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateRequest {
  @NotBlank(message = "로그인 ID는 필수입니다.")
  @Size(min = 4, max = 20, message = "아이디는 4~20자여야 합니다.")
  private String loginId;

  @NotBlank(message = "비밀번호는 필수입니다.")
  @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
  private String password;

  @NotBlank(message = "이름은 필수입니다.")
  private String userName;

  private String email;
  private String description;

  public CreateUserCommand toCommand() {
    return new CreateUserCommand(loginId, password, userName, email, description);
  }
}