package com.itmasters.icon.api.user.application.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateCommand {
    private String userId;
    private String userName;
    private String email;
    private String description;
}