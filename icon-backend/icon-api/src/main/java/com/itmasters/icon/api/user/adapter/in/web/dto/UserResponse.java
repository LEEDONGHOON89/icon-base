package com.itmasters.icon.api.user.adapter.in.web.dto;

import com.itmasters.icon.api.user.application.result.UserResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
  private String userId;
  private String loginId;
  private String userName;
  private String email;
  private Boolean isActive;

  public static UserResponse from(UserResult result) {
    return new UserResponse(
        result.getUserId(),
        result.getLoginId(),
        result.getUserName(),
        result.getEmail(),
        result.getIsActive());
  }
}