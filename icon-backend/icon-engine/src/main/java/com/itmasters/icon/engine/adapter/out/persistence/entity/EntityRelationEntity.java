package com.itmasters.icon.engine.adapter.out.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * entity_attributes 간 상태 관계 저장 (그래프 엣지)
 *
 * 핵심 원칙:

   * -✅ 상태 관계만 저장 (OWNS, USES, ACCESSES)
   * -❌ 행위는 저장 안 함 (TRANSFERS_TO는 event_stream 사용)
   * -✅ 관계의 양쪽 끝은 entity_attributes에 존재하는 엔티티
   * -✅ 관계 생성은 relation_rules 설정에 따라 자동 감지

 *
 * 관계 타입 (예시):

   * -OWNS: CUSTOMER → ACCOUNT (고객이 계좌 소유)
   * -USES: CUSTOMER → DEVICE (고객이 디바이스 사용)
   * -ACCESSES: DEVICE → ACCOUNT (디바이스가 계좌 접근)

 *
 * @since 2025-02-01
 */
@Entity
@Table(
    name = "entity_relations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_entity_relations",
            columnNames = {"from_entity_type", "from_entity_id", "relation_type", "to_entity_type", "to_entity_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntityRelationEntity {

    /**
     * 관계 고유 ID (자동 증가)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relation_id")
    private Long relationId;

    /**
     * 출발 엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE)
     */
    @Column(name = "from_entity_type", nullable = false, length = 50)
    private String fromEntityType;

    /**
     * 출발 엔티티 ID (entity_attributes 참조)
     */
    @Column(name = "from_entity_id", nullable = false, length = 255)
    private String fromEntityId;

    /**
     * 관계 유형 (OWNS, USES, ACCESSES)
     */
    @Column(name = "relation_type", nullable = false, length = 50)
    private String relationType;

    /**
     * 도착 엔티티 타입
     */
    @Column(name = "to_entity_type", nullable = false, length = 50)
    private String toEntityType;

    /**
     * 도착 엔티티 ID (entity_attributes 참조)
     */
    @Column(name = "to_entity_id", nullable = false, length = 255)
    private String toEntityId;

    /**
     * 관계 속성 (JSONB)
     *
     * 예시:

   * -OWNS: {"ownership_ratio": 100, "since": "2020-01-15"}
   * -USES: {"first_used": "2024-11-01", "usage_count": 150, "is_trusted": false}
   * -ACCESSES: {"first_access": "2025-01-31", "access_count": 50}

     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", columnDefinition = "jsonb")
    private JsonNode properties;

    /**
     * 관계 최초 생성 시점
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 관계 속성 마지막 업데이트 시점
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 새 관계 생성 (정적 팩토리 메서드)
     *
     * @param fromEntityType 출발 엔티티 타입 (예: "CUSTOMER")
     * @param fromEntityId 출발 엔티티 ID (예: "C001")
     * @param relationType 관계 타입 (예: "OWNS")
     * @param toEntityType 도착 엔티티 타입 (예: "ACCOUNT")
     * @param toEntityId 도착 엔티티 ID (예: "A1234")
     * @param properties 관계 속성 (JSONB)
     * @return 생성된 EntityRelationEntity
     */
    public static EntityRelationEntity of(
            String fromEntityType,
            String fromEntityId,
            String relationType,
            String toEntityType,
            String toEntityId,
            JsonNode properties) {

        EntityRelationEntity entity = new EntityRelationEntity();
        entity.fromEntityType = fromEntityType;
        entity.fromEntityId = fromEntityId;
        entity.relationType = relationType;
        entity.toEntityType = toEntityType;
        entity.toEntityId = toEntityId;
        entity.properties = properties;

        LocalDateTime now = LocalDateTime.now();
        entity.createdAt = now;
        entity.updatedAt = now;

        return entity;
    }

    /**
     * 관계 속성 업데이트 (UPSERT 패턴)
     *
     * 기존 관계가 있으면 properties만 업데이트하고 updated_at 갱신
     *
     * @param newProperties 새 속성
     */
    public void updateProperties(JsonNode newProperties) {
        this.properties = newProperties;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * PrePersist: 생성 시점 자동 설정
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * PreUpdate: 업데이트 시점 자동 설정
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
