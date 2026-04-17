package com.itmasters.icon.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UserDto {

    /**
     * 사용자 상세 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Info {
        private String userId;
        private String loginId;
        private String userName;
        private String email;
        private Boolean isActive;
        private LocalDateTime lastLoginAt;
        private LocalDateTime passwordChangedAt;
    }

    /**
     * 사용자 간단 정보 (민감한 정보 제외)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Simple {
        private String userId;
        private String loginId;
        private String userName;
        private String email;
    }

    /**
     * 사용자 생성 요청
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCommand {
        private String loginId;
        private String password;
        private String userName;
        private String email;
        private String description;
    }

    /**
     * 사용자 정보 업데이트 요청
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCommand {
        private String userName;
        private String email;
        private String description;
    }

    /**
     * 비밀번호 변경 요청
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordChangeCommand {
        private String currentPassword;
        private String newPassword;
    }
}