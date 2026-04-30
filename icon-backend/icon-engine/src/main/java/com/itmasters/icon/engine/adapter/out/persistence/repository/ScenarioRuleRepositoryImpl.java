package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineScenarioRuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QEngineScenarioRuleEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * scenario_rules 커스텀 Repository 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class ScenarioRuleRepositoryImpl implements ScenarioRuleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EngineScenarioRuleEntity> findByScenarioIdOrderByOrderNo(String scenarioId) {
        QEngineScenarioRuleEntity q = QEngineScenarioRuleEntity.engineScenarioRuleEntity;

        return queryFactory
                .selectFrom(q)
                .where(q.scenarioId.eq(scenarioId))
                .orderBy(q.orderNo.asc())
                .fetch();
    }

    @Override
    public List<EngineScenarioRuleEntity> findByRuleId(String ruleId) {
        QEngineScenarioRuleEntity q = QEngineScenarioRuleEntity.engineScenarioRuleEntity;

        return queryFactory
                .selectFrom(q)
                .where(q.ruleId.eq(ruleId))
                .orderBy(q.scenarioId.asc(), q.orderNo.asc())
                .fetch();
    }

    @Override
    public List<EngineScenarioRuleEntity> findByScenarioIdIn(List<String> scenarioIds) {
        QEngineScenarioRuleEntity q = QEngineScenarioRuleEntity.engineScenarioRuleEntity;

        return queryFactory
                .selectFrom(q)
                .where(q.scenarioId.in(scenarioIds))
                .orderBy(q.scenarioId.asc(), q.orderNo.asc())
                .fetch();
    }
}
