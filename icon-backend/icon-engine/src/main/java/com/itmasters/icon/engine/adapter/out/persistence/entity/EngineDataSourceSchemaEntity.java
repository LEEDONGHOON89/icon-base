package com.itmasters.icon.engine.adapter.out.persistence.entity;

import com.itmasters.icon.common.domain.type.FieldDataType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 룰 엔진용 데이터소스 스키마 엔티티
 * [2026-04-20] 재설계: parser 관계 제거 (파서는 data_source_parsers 테이블로 관리)
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "standard_field_id")
    private EngineStandardFieldEntity standardField;

    @Column(name = "standard_field_id", insertable = false, updatable = false)
    private String standardFieldId;

    public boolean hasStandardFieldMapping() {
        return standardFieldId != null || standardField != null;
    }

    public String getTargetFieldName() {
        if (standardField != null && standardField.getStandardFieldId() != null) {
            return standardField.getStandardFieldId();
        }
        return fieldName;
    }

    public boolean isValidType(Object value) {
        if (value == null) return !Boolean.TRUE.equals(isRequired);
        if (dataType == null) return true;
        return switch (dataType) {
            case STRING  -> value instanceof String;
            case NUMBER  -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
            case DATE    -> value instanceof java.util.Date
                         || value instanceof java.time.LocalDate
                         || value instanceof java.time.LocalDateTime;
            default      -> true;
        };
    }
}
