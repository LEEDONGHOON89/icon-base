package com.itmasters.icon.engine.adapter.out.persistence.entity;

import com.itmasters.icon.common.domain.type.FieldDataType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 룰 엔진용 표준 필드 엔티티
 * icon-api의 standard_fields 테이블과 동일한 구조
 */
@Entity
@Table(name = "standard_fields")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EngineStandardFieldEntity {

    @Id
    @Column(name = "standard_field_id")
    private String standardFieldId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "data_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FieldDataType dataType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}