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
 * 관계 자동 생성 규칙
 *
 * 목적: "어떤 엔티티 조합이 나타나면 어떤 관계를 만들지" 설정
 *
 * 예시:

   * -customer_id와 account_id가 동시에 있으면 → CUSTOMER --OWNS--> ACCOUNT
   * -customer_id와 device_id가 동시에 있으면 → CUSTOMER --USES--> DEVICE
   * -device_id와 account_id가 동시에 있으면 → DEVICE --ACCESSES--> ACCOUNT

 *
 * 처리 흐름:

   * -DataSource별 활성 규칙 조회
   * -이벤트 데이터에서 from_id_field, to_id_field 확인
   * -둘 다 있으면 entity_relations 생성/업데이트

 *
 * @since 2025-02-01
 */
@Entity
@Table(
    name = "entity_relation_rules",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_entity_relation_rules",
            columnNames = {"data_source_id", "from_entity_type", "relation_type", "to_entity_type"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RelationRuleEntity {

    /**
     * 규칙 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;

    /**
     * 적용 대상 DataSource
     */
    @Column(name = "data_source_id", nullable = false, length = 50)
    private String dataSourceId;

    /**
     * From Entity Type (출발 엔티티 타입)
     * 예: "CUSTOMER"
     */
    @Column(name = "from_entity_type", nullable = false, length = 50)
    private String fromEntityType;

    /**
     * From ID Field (출발 엔티티 ID 필드명)
     * 예: "customer_id" (이벤트 데이터의 실제 필드명)
     */
    @Column(name = "from_id_field", nullable = false, length = 100)
    private String fromIdField;

    /**
     * Relation Type (생성할 관계 타입)
     * 예: "OWNS", "USES", "ACCESSES"
     */
    @Column(name = "relation_type", nullable = false, length = 50)
    private String relationType;

    /**
     * To Entity Type (도착 엔티티 타입)
     * 예: "ACCOUNT"
     */
    @Column(name = "to_entity_type", nullable = false, length = 50)
    private String toEntityType;

    /**
     * To ID Field (도착 엔티티 ID 필드명)
     * 예: "account_id" (이벤트 데이터의 실제 필드명)
     */
    @Column(name = "to_id_field", nullable = false, length = 100)
    private String toIdField;

    /**
     * 활성화 여부
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 규칙 설명 (선택)
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 관계 속성 템플릿 (JSONB)
     *
     * 관계 생성 시 초기 properties를 정의합니다.
     * 템플릿 변수 지원:

   * -${event_time} - 이벤트 발생 시각
   * -${from_entity_id} - 출발 엔티티 ID
   * -${to_entity_id} - 도착 엔티티 ID

     *
     * 예시: {"ownership_ratio": 100, "since": "${event_time}"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties_template", columnDefinition = "jsonb")
    private JsonNode propertiesTemplate;

    /**
     * To 엔티티 속성 템플릿 (JSONB)
     *
     * 관계 생성 시 To 엔티티를 entity_attributes에 생성할 때 사용할 초기 속성 템플릿
     * 템플릿 변수 지원:

   * -${event_time} - 이벤트 발생 시각
   * -${data_source_id} - DataSource ID
   * -${to_entity_id} - To 엔티티 ID
   * -${from_entity_id} - From 엔티티 ID

     *
     * 예시: {"discovered_from": "${data_source_id}", "discovered_at": "${event_time}", "status": "SHELL"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "to_entity_attributes_template", columnDefinition = "jsonb")
    private JsonNode toEntityAttributesTemplate;

    /**
     * 생성 일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 새 규칙 생성 (정적 팩토리 메서드)
     *
     * @param dataSourceId 적용 대상 DataSource ID
     * @param fromEntityType 출발 엔티티 타입
     * @param fromIdField 출발 엔티티 ID 필드명
     * @param relationType 관계 타입
     * @param toEntityType 도착 엔티티 타입
     * @param toIdField 도착 엔티티 ID 필드명
     * @param description 설명
     * @return 생성된 RelationRuleEntity
     */
    public static RelationRuleEntity of(
            String dataSourceId,
            String fromEntityType,
            String fromIdField,
            String relationType,
            String toEntityType,
            String toIdField,
            String description) {

        RelationRuleEntity entity = new RelationRuleEntity();
        entity.dataSourceId = dataSourceId;
        entity.fromEntityType = fromEntityType;
        entity.fromIdField = fromIdField;
        entity.relationType = relationType;
        entity.toEntityType = toEntityType;
        entity.toIdField = toIdField;
        entity.description = description;
        entity.isActive = true;

        LocalDateTime now = LocalDateTime.now();
        entity.createdAt = now;
        entity.updatedAt = now;

        return entity;
    }

    /**
     * 규칙 활성화
     */
    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 규칙 비활성화
     */
    public void deactivate() {
        this.isActive = false;
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
        if (this.isActive == null) {
            this.isActive = true;
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
