package com.itmasters.icon.api.datasourceschema.adapter.out.persistence.entity;

import com.itmasters.icon.entity.Auditable;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DataSourceEntity;
import com.itmasters.icon.api.standardfield.adapter.out.persistence.entity.StandardFieldEntity;
import com.itmasters.icon.common.domain.type.FieldDataType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 데이터소스 스키마 엔티티
 * 데이터소스가 실제로 가지고 있는 모든 필드 정보와 표준 필드 매핑을 저장
 */
@Entity
@Table(name = "data_source_schemas")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DataSourceOriginalSchemaEntity extends Auditable {
    
    @Id
    @Column(name = "data_source_schema_id", nullable = false)
    private String dataSourceSchemaId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id", nullable = false)
    private DataSourceEntity dataSource;
    
    @Column(name = "field_name", nullable = false)
    private String fieldName;
    
    @Column(name = "data_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private FieldDataType dataType;
    
    @Column(name = "is_required", nullable = false)
    private boolean isRequired;
    
    @Column(name = "default_value", length = 500)
    private String defaultValue;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "field_order")
    private Integer fieldOrder;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive;
    
    // ===== 새로 추가된 필드 =====
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_field_id")
    private StandardFieldEntity standardField;
    
    // transform_rule 컬럼 제거에 따라 필드 삭제
    
    // ===== 비즈니스 메서드 =====
    
    /**
     * 표준 필드 매핑 여부 확인
     */
    public boolean hasStandardFieldMapping() {
        return standardField != null;
    }
    
    /**
     * 대상 필드명 결정 (표준 필드명 > 원본 필드명)
     */
    public String getTargetFieldName() {
        if (standardField != null && standardField.getStandardFieldId() != null) {
            return standardField.getStandardFieldId();
        }
        return fieldName;
    }
    
    /**
     * 표준 필드 매핑 설정
     */
    public void mapToStandardField(StandardFieldEntity standardField) {
        this.standardField = standardField;
    }
    
    /**
     * 표준 필드 매핑 해제
     */
    public void unmapStandardField() {
        this.standardField = null;
    }
    
    /**
     * 활성화 상태 변경
     */
    public void updateActiveStatus(boolean isActive) {
        this.isActive = isActive;
    }
}
