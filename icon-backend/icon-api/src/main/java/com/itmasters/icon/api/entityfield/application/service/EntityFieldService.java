package com.itmasters.icon.api.entityfield.application.service;

import com.itmasters.icon.api.entityfield.adapter.in.web.dto.EntityFieldDto;
import com.itmasters.icon.api.entityfield.adapter.persistence.entity.EntityFieldEntity;
import com.itmasters.icon.api.entityfield.adapter.persistence.repository.JpaEntityFieldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * EntityField Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntityFieldService {

    private final JpaEntityFieldRepository entityFieldRepository;

    /**
     * 활성화된 엔티티 필드 목록 조회
     */
    public List<EntityFieldDto.Response> getActiveEntityFields() {
        log.info("Getting active entity fields");

        return entityFieldRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 모든 엔티티 필드 목록 조회
     */
    public List<EntityFieldDto.Response> getAllEntityFields() {
        log.info("Getting all entity fields");

        return entityFieldRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // [2026-04-24] CRUD API 추가 — 엔티티 필드 등록
    @Transactional
    public EntityFieldDto.Response createEntityField(EntityFieldDto.CreateRequest request) {
        log.info("Creating entity field: {}", request.getEntityFieldId());

        if (entityFieldRepository.existsById(request.getEntityFieldId())) {
            throw new IllegalArgumentException("이미 존재하는 필드 ID입니다: " + request.getEntityFieldId());
        }

        EntityFieldEntity entity = new EntityFieldEntity();
        entity.setEntityFieldId(request.getEntityFieldId());
        entity.setDisplayName(request.getDisplayName());
        entity.setDataType(request.getDataType());
        entity.setDescription(request.getDescription());
        entity.setIsActive(true);

        EntityFieldEntity saved = entityFieldRepository.save(entity);
        return toResponse(saved);
    }

    // [2026-04-24] CRUD API 추가 — 엔티티 필드 수정
    @Transactional
    public EntityFieldDto.Response updateEntityField(String entityFieldId, EntityFieldDto.UpdateRequest request) {
        log.info("Updating entity field: {}", entityFieldId);

        EntityFieldEntity entity = entityFieldRepository.findById(entityFieldId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 필드입니다: " + entityFieldId));

        entity.setDisplayName(request.getDisplayName());
        entity.setDataType(request.getDataType());
        entity.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }

        return toResponse(entity);
    }

    // [2026-04-24] CRUD API 추가 — 엔티티 필드 삭제 (비활성화)
    @Transactional
    public void deleteEntityField(String entityFieldId) {
        log.info("Deleting (deactivating) entity field: {}", entityFieldId);

        EntityFieldEntity entity = entityFieldRepository.findById(entityFieldId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 필드입니다: " + entityFieldId));

        entityFieldRepository.delete(entity);
    }

    /** Entity → Response DTO 변환 */
    private EntityFieldDto.Response toResponse(EntityFieldEntity entity) {
        return EntityFieldDto.Response.builder()
                .entityFieldId(entity.getEntityFieldId())
                .displayName(entity.getDisplayName())
                .dataType(entity.getDataType())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .build();
    }
}
