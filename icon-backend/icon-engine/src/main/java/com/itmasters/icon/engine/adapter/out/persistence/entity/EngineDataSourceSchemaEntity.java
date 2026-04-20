package com.itmasters.icon.engine.adapter.out.persistence.entity;

import com.itmasters.icon.common.domain.type.FieldDataType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 룰 엔진용 데이터소스 스키마 엔티티
 * icon-api의 data_source_schemas 테이블과 동일한 구조
 */
@Entity
@Table(name = "data_source_schemas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EngineDataSourceSchemaEntity {

    @Id
    @Column(name = "data_source_schema_id")
    private String schemaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id", nullable = false)
    private EngineDataSourceEntity dataSource;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "data_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private FieldDataType dataType;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Column(name = "field_order")
    private Integer fieldOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    // 표준 필드 매핑 정보 추가
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "standard_field_id")
    private EngineStandardFieldEntity standardField;
    
    @Column(name = "standard_field_id", insertable = false, updatable = false)
    private String standardFieldId;
    
    // [2026-04-20] 파서 연동 - 원본 필드에 적용할 파서 (선택사항)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parser_id")
    private EngineParserEntity parser;

    // transform_rule 컬럼 제거에 따라 필드 삭제
    
    /**
     * 표준 필드 매핑 여부 확인
     */
    public boolean hasStandardFieldMapping() {
        return standardFieldId != null || standardField != null;
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
     * 값의 타입 유효성 검증
     */
    public boolean isValidType(Object value) {
        if (value == null) {
            return !Boolean.TRUE.equals(isRequired);
        }
        
        if (dataType == null) {
            return true; // 타입이 지정되지 않은 경우 모든 값 허용
        }
        
        switch (dataType) {
            case STRING:
                return value instanceof String;
            case NUMBER:
                return value instanceof Number;
            case BOOLEAN:
                return value instanceof Boolean;
            case DATE:
                return value instanceof java.util.Date || 
                       value instanceof java.time.LocalDate ||
                       value instanceof java.time.LocalDateTime;
            default:
                return true;
        }
    }
}
