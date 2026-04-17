package com.itmasters.icon.api.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 생성/수정 이력 공통 베이스 클래스
@MappedSuperclass // JPA 엔티티 상속 구조에서 사용
@EntityListeners(AuditingEntityListener.class) // Auditing 기능 활성화
@Getter
public abstract class Auditable {
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy; // 생성자(유저 도메인 참조)

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정일시

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy; // 수정자(유저 도메인 참조)
}
