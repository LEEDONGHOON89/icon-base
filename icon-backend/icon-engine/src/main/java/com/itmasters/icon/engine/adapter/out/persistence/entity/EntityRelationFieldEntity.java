package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity Relation Fields Entity
 * 엔티티 관계 발견을 위한 후보 필드 설정
 */
@Entity
@Table(name = "entity_relation_fields")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntityRelationFieldEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;

    @Column(name = "data_source_id", nullable = false, length = 50)
    private String dataSourceId;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "field_role", length = 20)
    private String fieldRole;  // FROM, TO, BOTH

    @Column(name = "priority")
    private Integer priority = 0;

    // 명시적 관계 생성을 위한 메타데이터
    @Column(name = "entity_type", length = 50)
    private String entityType;  // ACCOUNT, CUSTOMER 등

    @Column(name = "relation_type", length = 50)
    private String relationType;  // TRANSFERS_TO, OWNS 등 (NULL이면 패턴 발견용 힌트)

    @Column(name = "target_field", length = 100)
    private String targetField;  // 대상 필드명

    @Column(name = "target_entity_type", length = 50)
    private String targetEntityType;  // 대상 엔티티 타입

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 기본 필드만 설정 (패턴 발견용 힌트)
    public static EntityRelationFieldEntity of(
            String dataSourceId,
            String fieldName,
            Boolean isEnabled,
            String fieldRole,
            Integer priority) {
        EntityRelationFieldEntity entity = new EntityRelationFieldEntity();
        entity.dataSourceId = dataSourceId;
        entity.fieldName = fieldName;
        entity.isEnabled = isEnabled;
        entity.fieldRole = fieldRole;
        entity.priority = priority;
        return entity;
    }

    // 명시적 관계 생성용 (모든 메타데이터 포함)
    public static EntityRelationFieldEntity ofExplicitRelation(
            String dataSourceId,
            String fieldName,
            Boolean isEnabled,
            String fieldRole,
            Integer priority,
            String entityType,
            String relationType,
            String targetField,
            String targetEntityType) {
        EntityRelationFieldEntity entity = new EntityRelationFieldEntity();
        entity.dataSourceId = dataSourceId;
        entity.fieldName = fieldName;
        entity.isEnabled = isEnabled;
        entity.fieldRole = fieldRole;
        entity.priority = priority;
        entity.entityType = entityType;
        entity.relationType = relationType;
        entity.targetField = targetField;
        entity.targetEntityType = targetEntityType;
        return entity;
    }

    /**
     * 명시적 관계 설정 여부 확인
     * @return relationType이 설정되어 있으면 true (즉시 관계 생성)
     */
    public boolean hasExplicitRelation() {
        return relationType != null && !relationType.isEmpty();
    }
}
