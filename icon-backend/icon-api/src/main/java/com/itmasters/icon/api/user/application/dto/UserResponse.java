package com.itmasters.icon.api.user.application.dto;

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

  public static UserResponse from(UserResult userResult) {
    return new UserResponse(
        userResult.getUserId(),
        userResult.getLoginId(),
        userResult.getUserName(),
        userResult.getEmail(),
        userResult.getIsActive());
  }
}
