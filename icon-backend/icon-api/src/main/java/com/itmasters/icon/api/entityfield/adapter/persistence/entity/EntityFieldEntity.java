package com.itmasters.icon.api.entityfield.adapter.persistence.entity;

import com.itmasters.icon.entity.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 엔티티 필드 Entity
 */
@Entity
@Table(name = "entity_fields")
@Getter
@Setter
// [2026-04-24] CRUD 생성 시 직접 인스턴스화 필요 → PROTECTED → PUBLIC 으로 변경
@NoArgsConstructor
public class EntityFieldEntity extends Auditable {

    @Id
    @Column(name = "entity_field_id", length = 100)
    private String entityFieldId;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "data_type", length = 50)
    private String dataType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
