package com.itmasters.icon.engine.adapter.out.persistence.entity;

import com.itmasters.icon.common.domain.DomainEntityType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * 엔진용 프로파일 JPA 엔티티
 */
@Entity
@Table(name = "profiles")
@Getter
public class EngineProfileEntity {

    @Id
    @Column(name = "profile_id")
    private String profileId;
    
    @Column(name = "profile_name", nullable = false)
    private String profileName;
    
    @Column(name = "data_source_id", nullable = false)
    private String dataSourceId;
    
    @Column(name = "profile_purpose")
    private String profilePurpose;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "display_order")
    private Integer displayOrder;
    
    @Column(name = "schema_count")
    private Integer schemaCount;
    
    @Column(name = "rule_count")
    private Integer ruleCount;

    // standartd_field_id
    @Column(name = "timestamp_key", length = 500)
    private String timestampKey;

    /**
     * 데이터 목적지 타입
     * EVENT_STREAM: event_stream만 저장
     * ENTITY: entity_attributes만 저장
     * BOTH: event_stream + entity_attributes 양쪽 저장
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "destination_type", length = 20)
    private DestinationType destinationType;

    /**
     * 엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE)
     * destination_type이 ENTITY 또는 BOTH일 때 필수
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 20)
    private DomainEntityType entityType;

    /**
     * entity_id로 사용할 필드명
     * 예: account_number, customer_id
     */
    @Column(name = "entity_id_field", length = 100)
    private String entityIdField;

    /**
     * entity_attributes에 저장할 필드 목록 (JSONB)
     * NULL이면 전체 필드 저장, 배열이 있으면 지정된 필드만 저장
     * 예: ["account_number", "customer_id", "account_type", "opened_at"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "store_fields", columnDefinition = "jsonb")
    private List<String> storeFields;

    // 기본 생성자
    protected EngineProfileEntity() {}

    // 생성자
    @Builder
    public EngineProfileEntity(String profileId, String profileName, String dataSourceId,
                             String profilePurpose, String description, Boolean isActive,
                             Integer displayOrder, String timestampKey, DestinationType destinationType,
                             DomainEntityType entityType, String entityIdField, List<String> storeFields) {
        this.profileId = profileId;
        this.profileName = profileName;
        this.dataSourceId = dataSourceId;
        this.profilePurpose = profilePurpose;
        this.description = description;
        this.isActive = isActive;
        this.displayOrder = displayOrder;
        this.timestampKey = timestampKey;
        this.destinationType = destinationType;
        this.entityType = entityType;
        this.entityIdField = entityIdField;
        this.storeFields = storeFields;
    }

    /**
     * 이 프로파일이 엔티티 속성을 저장하는지 여부
     */
    public boolean shouldStoreEntityAttributes() {
        return destinationType == DestinationType.ENTITY 
            || destinationType == DestinationType.BOTH;
    }

    /**
     * 이 프로파일이 event_stream을 저장하는지 여부
     */
    public boolean shouldStoreEventStream() {
        return destinationType == DestinationType.EVENT_STREAM 
            || destinationType == DestinationType.BOTH;
    }

    /**
     * 모든 필드를 저장해야 하는지 여부
     */
    public boolean shouldStoreAllFields() {
        return storeFields == null || storeFields.isEmpty();
    }
}