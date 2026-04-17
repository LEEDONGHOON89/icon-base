package com.itmasters.icon.api.user.application.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateCommand {
    private String loginId;
    private String password;
    private String userName;
    private String email;
    private String description;
}