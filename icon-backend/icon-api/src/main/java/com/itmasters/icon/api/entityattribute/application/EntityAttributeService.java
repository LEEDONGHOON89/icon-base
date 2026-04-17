package com.itmasters.icon.api.entityattribute.application;

import com.itmasters.icon.api.entityattribute.adapter.out.persistence.entity.ApiEntityAttributeEntity;
import com.itmasters.icon.api.entityattribute.adapter.out.persistence.repository.EntityAttributeReadRepository;
import com.itmasters.icon.api.entityattribute.dto.EntityAttributeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * entity_attributes 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
public class EntityAttributeService {

    private final EntityAttributeReadRepository entityAttributeReadRepository;

    /**
     * 엔티티 타입과 ID로 entity_attributes 조회
     *
     * @param entityType 엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE 등)
     * @param entityId   엔티티 ID (CUS001, ACC001 등)
     * @return EntityAttributeDto (없으면 null)
     */
    public EntityAttributeDto findByEntityTypeAndId(String entityType, String entityId) {
        return entityAttributeReadRepository.findByEntityTypeAndId(entityType, entityId)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * 엔티티 타입과 ID로 attributes만 조회
     *
     * @param entityType 엔티티 타입
     * @param entityId   엔티티 ID
     * @return attributes Map (없으면 null)
     */
    public Map<String, Object> findAttributesByEntity(String entityType, String entityId) {
        return entityAttributeReadRepository.findAttributesByEntity(entityType, entityId)
                .orElse(null);
    }

    /**
     * 특정 타입의 모든 entity_attributes 조회
     *
     * @param entityType 엔티티 타입
     * @return 해당 타입의 모든 entity_attributes 목록
     */
    public List<EntityAttributeDto> findByEntityType(String entityType) {
        return entityAttributeReadRepository.findByEntityType(entityType).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 타입과 ID 목록으로 배치 조회
     *
     * @param entityType 엔티티 타입
     * @param entityIds  엔티티 ID 목록
     * @return 해당하는 entity_attributes 목록
     */
    public List<EntityAttributeDto> findBatch(String entityType, List<String> entityIds) {
        return entityAttributeReadRepository.findBatch(entityType, entityIds).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Entity → DTO 변환
     */
    private EntityAttributeDto toDto(ApiEntityAttributeEntity entity) {
        return EntityAttributeDto.builder()
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .attributes(entity.getAttributes())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
