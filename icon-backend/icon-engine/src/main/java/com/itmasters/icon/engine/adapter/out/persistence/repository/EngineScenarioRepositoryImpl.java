package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineScenarioEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QEngineScenarioEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QEngineScenarioRuleEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 시나리오 Repository 구현체 (QueryDSL 사용)
 */
@Repository
@RequiredArgsConstructor
public class EngineScenarioRepositoryImpl implements EngineScenarioRepository {
    
    private final JPAQueryFactory queryFactory;
    
    private final QEngineScenarioEntity scenario = QEngineScenarioEntity.engineScenarioEntity;
    private final QEngineScenarioRuleEntity scenarioRule = QEngineScenarioRuleEntity.engineScenarioRuleEntity;
    
    @Override
    public List<EngineScenarioEntity> findActiveScenarios() {
        return queryFactory
            .selectFrom(scenario)
            .where(scenario.isActive.isTrue())
            .orderBy(scenario.scenarioName.asc())
            .fetch();
    }
    
    @Override
    public List<EngineScenarioEntity> findActiveScenariosByProfileId(String profileId) {
        // profile 매핑 폐지: 전역 활성 시나리오 반환
        return findActiveScenarios();
    }
    
    @Override
    public EngineScenarioEntity findByScenarioId(String scenarioId) {
        return queryFactory
            .selectFrom(scenario)
            .where(scenario.scenarioId.eq(scenarioId))
            .fetchOne();
    }
}
