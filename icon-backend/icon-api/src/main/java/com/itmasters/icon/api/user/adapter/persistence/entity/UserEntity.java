package com.itmasters.icon.api.user.adapter.persistence.entity;

import com.itmasters.icon.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import com.itmasters.icon.api.user.dto.UserDto;

import java.time.LocalDateTime;

/**
 * User 엔티티 - 내부통제 시스템 사용자 (JPA 매핑 및 비즈니스 로직 포함)
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@DynamicInsert
@DynamicUpdate
public class UserEntity extends Auditable {
    @Id
    @Column(length = 13, nullable = false, updatable = false)
    private String userId;

    @Column(name = "login_id", nullable = false, length = 50, unique = true)
    private String loginId;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // === 정적 팩토리 메서드 ===
    
    public static UserEntity of(String loginId, String userName, String email) {
        UserEntity user = new UserEntity();
        user.loginId = loginId;
        user.userName = userName;
        user.email = email;
        user.isActive = true;
        return user;
    }

    public static UserEntity from(UserDto.CreateCommand command) {
        return UserEntity.of(
                command.getLoginId(),
                command.getUserName(),
                command.getEmail()
        );
    }

    // === 비즈니스 로직 메서드 ===
    
    /**
     * ID 할당 (Repository에서 사용)
     */
    public void assignId(String id) {
        if (this.userId != null) {
            throw new IllegalStateException("ID가 이미 할당되었습니다");
        }
        this.userId = id;
    }

    /**
     * 비밀번호 업데이트
     */
    public void updatePassword(String password) {
        this.password = password;
        this.passwordChangedAt = LocalDateTime.now();
    }

    /**
     * 사용자 정보 업데이트
     */
    public void updateUserInfo(String userName, String email) {
        this.userName = userName;
        this.email = email;
    }

    /**
     * UserDto.UpdateCommand로부터 정보 업데이트
     */
    public void update(UserDto.UpdateCommand command) {
        this.userName = command.getUserName();
        this.email = command.getEmail();
    }
    
    /**
     * 사용자 활성화/비활성화
     */
    public void setActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * 마지막 로그인 시간 업데이트
     */
    public void updateLastLoginTime() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // === DTO 변환 메서드 ===

    /**
     * UserDto.Info로 변환
     */
    public UserDto.Info toInfo() {
        return new UserDto.Info(
                this.userId,
                this.loginId,
                this.userName,
                this.email,
                this.isActive,
                this.lastLoginAt,
                this.passwordChangedAt
        );
    }

    /**
     * UserDto.Simple로 변환 (민감한 정보 제외)
     */
    public UserDto.Simple toSimple() {
        return new UserDto.Simple(
                this.userId,
                this.loginId,
                this.userName,
                this.email
        );
    }
}
