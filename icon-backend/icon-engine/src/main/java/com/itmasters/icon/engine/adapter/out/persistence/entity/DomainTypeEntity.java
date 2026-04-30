package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 도메인 타입 엔티티
 */
@Entity
@Table(name = "domain_types")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DomainTypeEntity {

    /**
     * 도메인 타입 ID (PK)
     * 예: CUSTOMER, ACCOUNT, DEVICE, AUTHENTICATION
     */
    @Id
    @Column(name = "domain_type_id", length = 50, nullable = false)
    private String domainTypeId;

    /**
     * 도메인 이름
     * 예: 고객, 계좌, 디바이스, 인증
     */
    @Column(name = "domain_name", length = 100, nullable = false)
    private String domainName;

    /**
     * 도메인 설명
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 아이콘 이름
     * 예: UserIcon, CreditCardIcon 등
     */
    @Column(name = "icon", length = 50)
    private String icon;

    /**
     * 색상 코드
     * 예: blue, green, purple 등
     */
    @Column(name = "color", length = 50)
    private String color;

    /**
     * 활성화 여부
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 표시 순서
     */
    @Column(name = "display_order")
    private Integer displayOrder;

    /**
     * 생성 일시
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 도메인 타입 생성 (정적 팩토리 메서드)
     */
    public static DomainTypeEntity of(
            String domainTypeId,
            String domainName,
            String description,
            String icon,
            String color,
            Integer displayOrder
    ) {
        DomainTypeEntity entity = new DomainTypeEntity();
        entity.domainTypeId = domainTypeId;
        entity.domainName = domainName;
        entity.description = description;
        entity.icon = icon;
        entity.color = color;
        entity.isActive = true;
        entity.displayOrder = displayOrder;
        return entity;
    }

    /**
     * 도메인 정보 수정
     */
    public void update(
            String domainName,
            String description,
            String icon,
            String color,
            Integer displayOrder
    ) {
        this.domainName = domainName;
        this.description = description;
        this.icon = icon;
        this.color = color;
        this.displayOrder = displayOrder;
    }

    /**
     * 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }
}
