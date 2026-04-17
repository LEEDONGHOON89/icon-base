package com.itmasters.icon.api.user.adapter.in.web.dto;

import com.itmasters.icon.api.user.application.command.UpdateUserCommand;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

  @NotBlank(message = "이름은 필수입니다.")
  private String userName;

  private String email;
  private String description;

  public UpdateUserCommand toCommand(String userId) {
    return new UpdateUserCommand(userId, userName, email, description);
  }
}