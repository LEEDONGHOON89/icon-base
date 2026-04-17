package com.itmasters.icon.api.auth.application.result;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResult {
    private String accessToken;
    private String refreshToken;
    private String message;

    public LoginResult(String accessToken, String refreshToken, String message) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.message = message;
    }
}
