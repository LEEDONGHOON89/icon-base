package com.itmasters.icon.api.entityattribute.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * entity_attributes 테이블에 대한 API 모듈 전용 엔티티
 *
 * 정적 데이터(고객 프로필, 계좌 정보 등)를 저장하는 테이블
 */
@Entity
@Table(name = "entity_attributes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_entity_attributes",
        columnNames = {"entity_type", "entity_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiEntityAttributeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entity_attr_id")
    private Long entityAttrId;

    /**
     * 엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE 등)
     */
    @Column(name = "entity_type", length = 20, nullable = false)
    private String entityType;

    /**
     * 엔티티 ID (CUS001, ACC001 등)
     */
    @Column(name = "entity_id", length = 100, nullable = false)
    private String entityId;

    /**
     * 정적 속성 정보 (JSONB)
     * 예: {"age": 68, "grade": "VIP", "region": "부산", "owned_accounts": ["ACC001"]}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> attributes;

    /**
     * 마지막 업데이트 시각
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
