package com.itmasters.icon.api.user.adapter.in.web.dto;

import com.itmasters.icon.api.user.application.command.UpdateUserPwdCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdatePassword {
    @NotEmpty
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
    private String password;


    public UpdateUserPwdCommand toCommand(String userId) {
        return new UpdateUserPwdCommand(userId, password);
    }
}
