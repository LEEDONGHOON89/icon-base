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
 * 탐지영역 엔티티
 *
 * 시나리오를 분류하기 위한 영역 정보 (계좌, 거래, 인증, 고객, 보안 등)
 */
@Entity
@Table(name = "detection_areas")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetectionAreaEntity {

    /**
     * 탐지영역 ID (PK)
     * 예: ACCOUNT, TRANSACTION, AUTH, CUSTOMER, SECURITY
     */
    @Id
    @Column(name = "detection_area_id", length = 50, nullable = false)
    private String detectionAreaId;

    /**
     * 탐지영역 이름
     * 예: 계좌, 거래, 인증, 고객, 보안
     */
    @Column(name = "area_name", length = 100, nullable = false)
    private String areaName;

    /**
     * 탐지영역 설명
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
     * 탐지영역 생성 (정적 팩토리 메서드)
     */
    public static DetectionAreaEntity of(
            String detectionAreaId,
            String areaName,
            String description,
            String icon,
            String color,
            Integer displayOrder
    ) {
        DetectionAreaEntity entity = new DetectionAreaEntity();
        entity.detectionAreaId = detectionAreaId;
        entity.areaName = areaName;
        entity.description = description;
        entity.icon = icon;
        entity.color = color;
        entity.isActive = true;
        entity.displayOrder = displayOrder;
        return entity;
    }

    /**
     * 탐지영역 정보 수정
     */
    public void update(
            String areaName,
            String description,
            String icon,
            String color,
            Integer displayOrder
    ) {
        this.areaName = areaName;
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
