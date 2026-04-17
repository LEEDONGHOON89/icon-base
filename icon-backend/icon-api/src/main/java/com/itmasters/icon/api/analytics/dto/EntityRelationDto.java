package com.itmasters.icon.api.analytics.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entity Relation DTO (entity_relations 테이블)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EntityRelationDto {
    private String fromEntityType;
    private String fromEntityId;
    private String relationType;
    private String toEntityType;
    private String toEntityId;
    private JsonNode properties;
}
