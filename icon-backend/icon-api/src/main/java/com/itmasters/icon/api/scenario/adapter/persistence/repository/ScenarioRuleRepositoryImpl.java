package com.itmasters.icon.api.scenario.adapter.persistence.repository;

import com.itmasters.icon.api.scenario.adapter.persistence.mapper.ScenarioRuleMapper;
import com.itmasters.icon.api.scenario.application.port.out.ScenarioRuleRepository;
import com.itmasters.icon.api.scenario.domain.ScenarioRule;
import com.itmasters.icon.common.domain.IdGenerator;
import com.itmasters.icon.common.domain.EntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.itmasters.icon.api.scenario.adapter.persistence.repository.JpaScenarioAggregateRepository;

/**
 * 시나리오-규칙 매핑 Repository 구현체
 */
@Repository("apiScenarioRuleRepositoryImpl")
@RequiredArgsConstructor
public class ScenarioRuleRepositoryImpl implements ScenarioRuleRepository {
    
    private final JpaScenarioAggregateRepository jpaScenarioAggregateRepository;
    private final ScenarioRuleMapper scenarioRuleMapper;
    private final IdGenerator idGenerator;
    
    @Override
    public ScenarioRule save(ScenarioRule scenarioRule) {
        // ID가 없는 경우 새로 생성
        if (scenarioRule.getScenarioRuleId() == null) {
            scenarioRule.assignId(idGenerator.generateId(EntityType.SCENARIO_RULE));
        }
        
        var entity = scenarioRuleMapper.toEntity(scenarioRule);
        var savedEntity = jpaScenarioAggregateRepository.save(entity);
        return scenarioRuleMapper.toDomain(savedEntity);
    }
    
    @Override
    public List<ScenarioRule> findByScenarioIdOrderByOrderNo(String scenarioId) {
        return jpaScenarioAggregateRepository.findByScenarioIdOrderByOrderNoAsc(scenarioId)
                .stream().map(scenarioRuleMapper::toDomain).toList();
    }
    
    @Override
    public List<ScenarioRule> findByRuleId(String ruleId) {
        return jpaScenarioAggregateRepository.findByAggregateId(ruleId)
                .stream().map(scenarioRuleMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByScenarioId(String scenarioId) {
        jpaScenarioAggregateRepository.deleteByScenarioId(scenarioId);
    }

    @Override
    @Transactional
    public void deleteByRuleId(String ruleId) {
        jpaScenarioAggregateRepository.deleteByAggregateId(ruleId);
    }

    @Override
    @Transactional
    public void deleteByScenarioIdAndRuleId(String scenarioId, String ruleId) {
        jpaScenarioAggregateRepository.deleteByScenarioIdAndAggregateId(scenarioId, ruleId);
    }

    @Override
    public boolean existsByScenarioIdAndRuleId(String scenarioId, String ruleId) {
        return jpaScenarioAggregateRepository.existsByScenarioIdAndAggregateId(scenarioId, ruleId);
    }
    
    @Override
    public List<ScenarioRule> saveAll(List<ScenarioRule> scenarioRules) {
        // ID가 없는 경우 새로 생성
        scenarioRules.forEach(scenarioRule -> {
            if (scenarioRule.getScenarioRuleId() == null) {
                scenarioRule.assignId(idGenerator.generateId(EntityType.SCENARIO_RULE));
            }
        });
            
        var entities = scenarioRules.stream().map(scenarioRuleMapper::toEntity).toList();
        var savedEntities = jpaScenarioAggregateRepository.saveAll(entities);
        return savedEntities.stream().map(scenarioRuleMapper::toDomain).toList();
    }
}
