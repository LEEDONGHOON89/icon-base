package com.itmasters.icon.api.auth.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RefreshTokenEntity {

    @Id
    @Column(length = 13, nullable = false, updatable = false)
    private String id;

    @Column(length = 13, nullable = false, unique = true)
    private String token;

    @Column(name = "user_id", length = 13, nullable = false, unique = true)
    @Comment("토큰을 발급받은 사용자의 ID")
    private String userId;

    @Column(name = "expire_dt", nullable = false)
    private LocalDateTime expireDt;

    // 생성 메서드
    public static RefreshTokenEntity create(String token, String userId, LocalDateTime expireDt) {
        return RefreshTokenEntity.builder()
                .token(token)
                .userId(userId)
                .expireDt(expireDt)
                .build();
    }

    // 비즈니스 로직
    public void updateToken(String token, LocalDateTime expireDt) {
        this.token = token;
        this.expireDt = expireDt;
    }

    /**
     * ID 할당 (Repository에서 사용)
     */
    public void assignId(String id) {
        if (this.id != null) {
            throw new IllegalStateException("ID가 이미 할당되었습니다");
        }
        this.id = id;
    }
}
