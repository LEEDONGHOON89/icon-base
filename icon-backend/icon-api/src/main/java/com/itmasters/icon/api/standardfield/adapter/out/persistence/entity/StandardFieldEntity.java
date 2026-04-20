package com.itmasters.icon.api.standardfield.adapter.out.persistence.entity;

import com.itmasters.icon.entity.Auditable;
import com.itmasters.icon.common.domain.type.FieldDataType;
import com.itmasters.icon.common.domain.rule.FieldCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 표준 필드 엔티티
 */
// [2026-04-20] 매퍼에서 직접 인스턴스 생성을 위해 생성자 접근자를 public으로 변경
@Entity
@Table(name = "standard_fields")
@Getter
@Setter
@NoArgsConstructor
public class StandardFieldEntity extends Auditable {
    
    @Id
    @Column(name = "standard_field_id", length = 100)
    private String standardFieldId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private FieldCategory category;
    
    @Column(name = "display_name", length = 200)
    private String displayName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 50)
    private FieldDataType dataType;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Anchor whitelist flag (optional column)
    @Column(name = "can_anchor")
    private Boolean canAnchor;

    // Optional synonyms for normalization
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "aliases", columnDefinition = "jsonb")
    private String aliases;

    // Allowed operators for this field (optional, overrides defaults)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
    @Column(name = "allowed_operators", columnDefinition = "text[]")
    private String[] allowedOperators;
}
