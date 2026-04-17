package com.itmasters.icon.api.entityattribute.adapter.in.web;

import com.itmasters.icon.api.entityattribute.application.EntityAttributeService;
import com.itmasters.icon.api.entityattribute.dto.EntityAttributeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * entity_attributes API Controller
 */
@RestController
@RequestMapping("/api/v1/entity-attributes")
@RequiredArgsConstructor
public class EntityAttributeController {

    private final EntityAttributeService entityAttributeService;

    /**
     * 엔티티 타입과 ID로 entity_attributes 조회
     *
     * @param entityType 엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE 등)
     * @param entityId   엔티티 ID (CUS001, ACC001 등)
     * @return EntityAttributeDto (없으면 404)
     */
    @GetMapping("/{entityType}/{entityId}")
    public EntityAttributeDto getEntityAttribute(
            @PathVariable String entityType,
            @PathVariable String entityId
    ) {
        EntityAttributeDto result = entityAttributeService.findByEntityTypeAndId(entityType, entityId);
        if (result == null) {
            throw new IllegalArgumentException(
                    String.format("Entity not found: type=%s, id=%s", entityType, entityId)
            );
        }
        return result;
    }

    /**
     * 엔티티 타입과 ID로 attributes만 조회
     *
     * @param entityType 엔티티 타입
     * @param entityId   엔티티 ID
     * @return attributes Map (없으면 404)
     */
    @GetMapping("/{entityType}/{entityId}/attributes")
    public Map<String, Object> getEntityAttributes(
            @PathVariable String entityType,
            @PathVariable String entityId
    ) {
        Map<String, Object> result = entityAttributeService.findAttributesByEntity(entityType, entityId);
        if (result == null) {
            throw new IllegalArgumentException(
                    String.format("Entity not found: type=%s, id=%s", entityType, entityId)
            );
        }
        return result;
    }

    /**
     * 특정 타입의 모든 entity_attributes 조회
     *
     * @param entityType 엔티티 타입
     * @return 해당 타입의 모든 entity_attributes 목록
     */
    @GetMapping("/type/{entityType}")
    public List<EntityAttributeDto> getEntitiesByType(
            @PathVariable String entityType
    ) {
        return entityAttributeService.findByEntityType(entityType);
    }

    /**
     * 특정 타입과 ID 목록으로 배치 조회
     *
     * @param entityType 엔티티 타입
     * @param entityIds  엔티티 ID 목록 (Request Body)
     * @return 해당하는 entity_attributes 목록
     */
    @PostMapping("/batch/{entityType}")
    public List<EntityAttributeDto> getBatchEntities(
            @PathVariable String entityType,
            @RequestBody List<String> entityIds
    ) {
        return entityAttributeService.findBatch(entityType, entityIds);
    }
}
