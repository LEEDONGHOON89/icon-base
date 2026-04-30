package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.entity.EntityUpdateRuleEntity;
import com.itmasters.icon.entity.QEntityUpdateRuleEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Entity Update Rule 커스텀 Repository 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class EntityUpdateRuleRepositoryImpl implements EntityUpdateRuleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EntityUpdateRuleEntity> findActiveRulesByScenarioId(String scenarioId) {
        QEntityUpdateRuleEntity q = QEntityUpdateRuleEntity.entityUpdateRuleEntity;

        return queryFactory
                .selectFrom(q)
                .where(
                        q.scenarioId.eq(scenarioId),
                        q.isActive.isTrue()
                )
                .fetch();
    }

    @Override
    public List<EntityUpdateRuleEntity> findAllActiveRules() {
        QEntityUpdateRuleEntity q = QEntityUpdateRuleEntity.entityUpdateRuleEntity;

        return queryFactory
                .selectFrom(q)
                .where(q.isActive.isTrue())
                .fetch();
    }

    @Override
    public List<EntityUpdateRuleEntity> findActiveRulesByDataSourceId(String dataSourceId) {
        QEntityUpdateRuleEntity q = QEntityUpdateRuleEntity.entityUpdateRuleEntity;

        return queryFactory
                .selectFrom(q)
                .where(
                        q.triggerType.eq("EVENT"),
                        q.dataSourceId.eq(dataSourceId),
                        q.isActive.isTrue()
                )
                .fetch();
    }

    @Override
    public List<EntityUpdateRuleEntity> findAllActiveEventRules() {
        QEntityUpdateRuleEntity q = QEntityUpdateRuleEntity.entityUpdateRuleEntity;

        return queryFactory
                .selectFrom(q)
                .where(
                        q.triggerType.eq("EVENT"),
                        q.isActive.isTrue()
                )
                .fetch();
    }
}
