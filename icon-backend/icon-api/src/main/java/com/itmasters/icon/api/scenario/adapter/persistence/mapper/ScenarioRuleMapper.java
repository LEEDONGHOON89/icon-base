package com.itmasters.icon.api.scenario.adapter.persistence.mapper;

import com.itmasters.icon.entity.ScenarioAggregateEntity;
import com.itmasters.icon.api.scenario.domain.ScenarioRule;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/**
 * 시나리오-규칙 매핑 도메인 ↔ Entity 변환 매퍼
 */
@Component
@RequiredArgsConstructor
public class ScenarioRuleMapper {
    private final ModelMapper modelMapper;

    /**
     * 도메인 → Entity 변환
     */
    public ScenarioAggregateEntity toEntity(ScenarioRule scenarioRule) {
        if (scenarioRule == null) {
            return null;
        }

        // 수동 매핑 (필드명 불일치 해결)
        ScenarioAggregateEntity entity = new ScenarioAggregateEntity();
        entity.setScenarioAggregateId(scenarioRule.getScenarioRuleId());
        entity.setScenarioId(scenarioRule.getScenarioId());
        entity.setAggregateId(scenarioRule.getRuleId()); // ruleId -> aggregateId
        entity.setOrderNo(scenarioRule.getOrderNo());
        entity.setOperator(scenarioRule.getOperator());

        return entity;
    }

    /**
     * Entity → 도메인 변환
     */
    public ScenarioRule toDomain(ScenarioAggregateEntity entity) {
        if (entity == null) {
            return null;
        }

        // 수동 매핑 (필드명 불일치 해결)
        ScenarioRule domain = ScenarioRule.of(
            entity.getScenarioId(),
            entity.getAggregateId(), // aggregateId -> ruleId
            entity.getOrderNo(),
            entity.getOperator()
        );

        // ID 할당 (이미 저장된 엔티티인 경우)
        if (entity.getScenarioAggregateId() != null) {
            domain.assignId(entity.getScenarioAggregateId());
        }

        return domain;
    }
}
