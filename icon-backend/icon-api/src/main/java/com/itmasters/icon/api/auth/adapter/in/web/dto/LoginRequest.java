package com.itmasters.icon.api.auth.adapter.in.web.dto;

import com.itmasters.icon.api.auth.application.command.LoginCommand;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotEmpty(message = "로그인 ID는 필수입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4~20자여야 합니다.")
    private String loginId;

    @NotEmpty(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
    private String password;


    public LoginCommand toCommand() {
        return new LoginCommand(this.loginId, this.password);
    }
}
