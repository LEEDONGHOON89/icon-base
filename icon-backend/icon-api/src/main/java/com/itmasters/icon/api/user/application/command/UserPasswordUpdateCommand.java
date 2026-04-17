package com.itmasters.icon.api.user.application.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserPasswordUpdateCommand {
    private String userId;
    private String currentPassword;
    private String newPassword;
}