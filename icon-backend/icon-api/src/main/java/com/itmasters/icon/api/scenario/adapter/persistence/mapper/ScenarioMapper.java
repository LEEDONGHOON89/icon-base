package com.itmasters.icon.api.scenario.adapter.persistence.mapper;

import com.itmasters.icon.api.scenario.adapter.persistence.entity.ScenarioEntity;
import com.itmasters.icon.api.scenario.domain.Scenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/**
 * 시나리오 도메인 ↔ Entity 변환 매퍼
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioMapper {
    private final ModelMapper modelMapper;


    /**
     * 도메인 → Entity 변환
     */
    public ScenarioEntity toEntity(Scenario scenario) {
        if (scenario == null) {
            return null;
        }

        log.info("🔍 Mapper toEntity - Domain entityFilterJson: {}", scenario.getEntityFilterJson());
        ScenarioEntity entity = modelMapper.map(scenario, ScenarioEntity.class);
        log.info("🔍 Mapper toEntity - Entity entityFilterJson: {}", entity.getEntityFilterJson());

        return entity;
    }
    
    /**
     * Entity → 도메인 변환
     */
    public Scenario toDomain(ScenarioEntity entity) {
        if (entity == null) {
            return null;
        }

        log.info("🔍 Mapper toDomain - Entity riskLevelId: {}, detectionAreaId: {}, primaryEntityType: {}",
                entity.getRiskLevelId(), entity.getDetectionAreaId(), entity.getPrimaryEntityType());

        Scenario domain = modelMapper.map(entity, Scenario.class);

        // ModelMapper가 일부 필드를 매핑하지 못할 수 있으므로 수동 보정
        if (domain.getRiskLevelId() == null && entity.getRiskLevelId() != null) {
            domain.setRiskLevelId(entity.getRiskLevelId());
        }
        if (domain.getDetectionAreaId() == null && entity.getDetectionAreaId() != null) {
            domain.setDetectionAreaId(entity.getDetectionAreaId());
        }
        if (domain.getPrimaryEntityType() == null && entity.getPrimaryEntityType() != null) {
            domain.setPrimaryEntityType(entity.getPrimaryEntityType());
        }

        log.info("🔍 Mapper toDomain - Domain riskLevelId: {}, detectionAreaId: {}, primaryEntityType: {}",
                domain.getRiskLevelId(), domain.getDetectionAreaId(), domain.getPrimaryEntityType());

        return domain;
    }
}