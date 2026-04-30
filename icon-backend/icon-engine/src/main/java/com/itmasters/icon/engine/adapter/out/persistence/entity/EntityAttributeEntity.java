package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 엔티티 정적 속성 엔티티
 *
 * 고객, 계좌, 디바이스 등의 정적 속성 저장
 * (나이, 등급, 지역, 계좌개설일 등)
 *
 * 최신 값만 유지 (이력 관리 없음)
 */
@Entity
@Table(
    name = "entity_attributes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_entity_attributes",
        columnNames = {"entity_type", "entity_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntityAttributeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entity_attr_id")
    private Long entityAttrId;

    /**
     * 엔티티 타입
     * CUSTOMER, ACCOUNT, DEVICE
     */
    @Column(name = "entity_type", length = 20, nullable = false)
    private String entityType;

    /**
     * 엔티티 실제 ID
     * 예: customer_12345, account_67890
     */
    @Column(name = "entity_id", length = 100, nullable = false)
    private String entityId;

    /**
     * 속성 값 (JSONB)
     *
     * 예시:
     * {
     *   "age": 70,
     *   "grade": "VIP",
     *   "region": "서울",
     *   "account_open_date": "2015-03-20",
     *   "device_type": "MOBILE"
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> attributes;

    /**
     * 엔티티를 처음 발견한 DataSource ID
     */
    @Column(name = "discovered_from", length = 50)
    private String discoveredFrom;

    /**
     * 엔티티를 처음 발견한 시각
     */
    @Column(name = "discovered_at")
    private LocalDateTime discoveredAt;

    /**
     * 엔티티 상태
     * - SHELL: 껍데기만 있음 (ID만 알고 속성 없음)
     * - ENRICHED: 일부 속성이 채워짐
     * - COMPLETE: 모든 필요한 속성이 채워짐
     */
    @Column(name = "status", length = 20)
    private String status = "SHELL";

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static EntityAttributeEntity of(
            String entityType,
            String entityId,
            Map<String, Object> attributes
    ) {
        EntityAttributeEntity entity = new EntityAttributeEntity();
        entity.entityType = entityType;
        entity.entityId = entityId;
        entity.attributes = attributes;
        entity.status = "SHELL";
        entity.updatedAt = LocalDateTime.now();
        return entity;
    }

    /**
     * Shell 엔티티 생성 (메타데이터 포함)
     */
    public static EntityAttributeEntity createShell(
            String entityType,
            String entityId,
            String discoveredFrom,
            LocalDateTime discoveredAt,
            Map<String, Object> attributes
    ) {
        EntityAttributeEntity entity = new EntityAttributeEntity();
        entity.entityType = entityType;
        entity.entityId = entityId;
        entity.attributes = attributes;
        entity.discoveredFrom = discoveredFrom;
        entity.discoveredAt = discoveredAt;
        entity.status = "SHELL";
        entity.updatedAt = LocalDateTime.now();
        return entity;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 속성 업데이트 (병합 방식)
     * 
     * 기존 attributes에 새 attributes를 병합합니다.
     * - 기존 필드: 유지
     * - 새 필드: 추가
     * - 중복 필드: 새 값으로 덮어쓰기
     */
    public void updateAttributes(Map<String, Object> newAttributes) {
        if (this.attributes == null) {
            this.attributes = new java.util.HashMap<>(newAttributes);
        } else {
            // 기존 attributes에 새 attributes 병합
            this.attributes.putAll(newAttributes);
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 특정 속성 값 조회
     */
    public Object getAttribute(String key) {
        return attributes != null ? attributes.get(key) : null;
    }

    /**
     * 특정 속성 존재 여부
     */
    public boolean hasAttribute(String key) {
        return attributes != null && attributes.containsKey(key);
    }
}
