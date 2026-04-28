package com.itmasters.icon.api.dataprofile.adapter.out.persistence.entity;

import com.itmasters.icon.entity.Auditable;
import com.itmasters.icon.common.domain.type.ProfilePurpose;
import com.itmasters.icon.common.domain.type.GroupKeyType;
import com.itmasters.icon.api.common.domain.type.DestinationType;
import com.itmasters.icon.common.domain.DomainEntityType;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DataSourceEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * 스키마 프로파일 JPA 엔티티
 */
@Entity
@Table(name = "profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProfileEntity extends Auditable {
    
    @Id
    @Column(name = "profile_id")
    private String profileId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id", nullable = false)
    private DataSourceEntity dataSource;
    
    @Column(name = "profile_name", nullable = false, length = 100)
    private String profileName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "profile_purpose", length = 50)
    private ProfilePurpose profilePurpose;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "display_order")
    private Integer displayOrder = 0;
    
    @Column(name = "schema_count", nullable = false)
    private Integer schemaCount = 0;
    
    @Column(name = "rule_count", nullable = false)
    private Integer ruleCount = 0;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "group_key", length = 500)
    private String groupKey;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "group_key_type", length = 50)
    private GroupKeyType groupKeyType;

    @Comment("시간순서 관리")
    @Column(name = "timestamp_key", length = 100)
    private String timestampKey;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "timestamp_profile_schema_id")
//    private ProfileSchemaEntity timestampProfileSchema;

    /**
     * 데이터 목적지 타입
     * EVENT_STREAM: event_stream만 저장
     * ENTITY: entity_attributes만 저장
     * BOTH: event_stream + entity_attributes 양쪽 저장
     * [2026-04-24] 주석 수정: ENTITY_ATTRIBUTES 는 존재하지 않는 enum 값, BOTH 가 올바른 값
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "destination_type", length = 20)
    private DestinationType destinationType;

    /**
     * 엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE)
     * destination_type이 ENTITY 또는 BOTH일 때 필수
     * [2026-04-24] 주석 수정: ENTITY_ATTRIBUTES → ENTITY 또는 BOTH
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
}