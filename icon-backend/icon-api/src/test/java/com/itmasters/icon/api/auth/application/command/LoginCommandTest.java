package com.itmasters.icon.api.auth.application.command;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class LoginCommandTest {

  @Test
  public void testLoginCommand() {
    LoginCommand command = new LoginCommand("test", "test");
    Assertions.assertThat(command.getLoginId()).isEqualTo("test");
    Assertions.assertThat(command.getPassword()).isEqualTo("test");
  }

  @Test
  public void testLoginCommand_loginIdIsNull() {
    // 로그인 아이디가 null인 경우 예외 발생
    Assertions.assertThatThrownBy(() -> new LoginCommand(null, "password"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("로그인 아이디는 필수 입력값입니다.");
  }

  @Test
  public void testLoginCommand_loginIdIsEmpty() {
    // 로그인 아이디가 빈 문자열인 경우 예외 발생
    Assertions.assertThatThrownBy(() -> new LoginCommand("   ", "password"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("로그인 아이디는 필수 입력값입니다.");
  }

  @Test
  public void testLoginCommand_passwordIsNull() {
    // 비밀번호가 null인 경우 예외 발생
    Assertions.assertThatThrownBy(() -> new LoginCommand("loginId", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("비밀번호는 필수 입력값입니다.");
  }

  @Test
  public void testLoginCommand_passwordIsEmpty() {
    // 비밀번호가 빈 문자열인 경우 예외 발생
    Assertions.assertThatThrownBy(() -> new LoginCommand("loginId", "   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("비밀번호는 필수 입력값입니다.");
  }
}
