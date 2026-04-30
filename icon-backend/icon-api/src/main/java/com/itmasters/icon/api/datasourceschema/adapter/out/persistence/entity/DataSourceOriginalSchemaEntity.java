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
 * [2026-04-20] 재설계: parser_id 컬럼 제거 (파서는 data_source_parsers 테이블로 관리)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_field_id")
    private StandardFieldEntity standardField;

    // ===== 비즈니스 메서드 =====

    public boolean hasStandardFieldMapping() {
        return standardField != null;
    }

    public String getTargetFieldName() {
        if (standardField != null && standardField.getStandardFieldId() != null) {
            return standardField.getStandardFieldId();
        }
        return fieldName;
    }

    public void mapToStandardField(StandardFieldEntity standardField) {
        this.standardField = standardField;
    }

    public void unmapStandardField() {
        this.standardField = null;
    }

    public void updateActiveStatus(boolean isActive) {
        this.isActive = isActive;
    }
}
