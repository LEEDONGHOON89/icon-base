package com.itmasters.icon.api.user.application.result;

import com.itmasters.icon.api.user.adapter.persistence.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserResult {
  private String userId; // TSID
  private String loginId;
  private String userName;
  private String email;
  private Boolean isActive;
  private String role;

  public static UserResult from(UserEntity user) {
    return new UserResult(
        user.getUserId(),
        user.getLoginId(),
        user.getUserName(),
        user.getEmail(),
        user.getIsActive(),
        null  // role은 별도로 설정
    );
  }

}
