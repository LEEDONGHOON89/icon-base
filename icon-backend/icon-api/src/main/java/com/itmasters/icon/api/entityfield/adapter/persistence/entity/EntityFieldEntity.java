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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
