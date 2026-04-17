package com.itmasters.icon.api.user.application.command;

import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public class UpdateUserPwdCommand {
    private String userId;
    private String password;

    public UpdateUserPwdCommand(String userId, String password) {
        if (StringUtils.hasText(password) == false) {
            throw new IllegalArgumentException("비밀번호는 필수 입력값입니다.");
        }
        this.userId = userId;
        this.password = password;
    }
}
