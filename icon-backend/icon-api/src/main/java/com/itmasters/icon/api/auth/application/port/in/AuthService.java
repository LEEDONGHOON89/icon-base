package com.itmasters.icon.api.auth.application.port.in;

import com.itmasters.icon.api.auth.application.command.LoginCommand;
import com.itmasters.icon.api.auth.application.result.LoginResult;

public interface AuthService {
    LoginResult login(LoginCommand command);
}
