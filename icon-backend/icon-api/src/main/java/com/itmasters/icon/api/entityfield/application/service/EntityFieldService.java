package com.itmasters.icon.api.entityfield.application.service;

import com.itmasters.icon.api.entityfield.adapter.in.web.dto.EntityFieldDto;
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
                .map(entity -> EntityFieldDto.Response.builder()
                        .entityFieldId(entity.getEntityFieldId())
                        .displayName(entity.getDisplayName())
                        .dataType(entity.getDataType())
                        .description(entity.getDescription())
                        .isActive(entity.getIsActive())
                        .build())
                .toList();
    }

    /**
     * 모든 엔티티 필드 목록 조회
     */
    public List<EntityFieldDto.Response> getAllEntityFields() {
        log.info("Getting all entity fields");

        return entityFieldRepository.findAll().stream()
                .map(entity -> EntityFieldDto.Response.builder()
                        .entityFieldId(entity.getEntityFieldId())
                        .displayName(entity.getDisplayName())
                        .dataType(entity.getDataType())
                        .description(entity.getDescription())
                        .isActive(entity.getIsActive())
                        .build())
                .toList();
    }
}
