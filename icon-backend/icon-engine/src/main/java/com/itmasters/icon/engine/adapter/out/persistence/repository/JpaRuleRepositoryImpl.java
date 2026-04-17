package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.QRuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Rule Repository 커스텀 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class JpaRuleRepositoryImpl implements JpaRuleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<RuleEntity> findActiveByRuleIds(Collection<String> ruleIds) {
        QRuleEntity q = QRuleEntity.ruleEntity;

        // OR 조건들을 BooleanBuilder로 구성
        BooleanBuilder orConditions = new BooleanBuilder();
        orConditions.or(q.predicateSensorId.in(ruleIds));
        orConditions.or(q.prevSensorId.in(ruleIds));
        orConditions.or(q.nextSensorId.in(ruleIds));
        orConditions.or(q.anchorSensorId.in(ruleIds));

        return queryFactory
                .selectFrom(q)
                .where(
                        q.isActive.isTrue(),
                        orConditions
                )
                .fetch();
    }
}
