package com.itmasters.icon.api.scenario.adapter.persistence.repository;

import com.itmasters.icon.api.scenario.adapter.persistence.entity.QScenarioEntity;
import com.itmasters.icon.api.scenario.adapter.persistence.entity.ScenarioEntity;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 시나리오 커스텀 Repository 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class JpaScenarioRepositoryImpl implements ScenarioRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ScenarioEntity> findByIdOrNameContaining(String search, Pageable pageable) {
        QScenarioEntity scenario = QScenarioEntity.scenarioEntity;

        BooleanExpression predicate = scenario.scenarioId.containsIgnoreCase(search)
                .or(scenario.scenarioName.containsIgnoreCase(search));

        List<ScenarioEntity> content = queryFactory
                .selectFrom(scenario)
                .where(predicate)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(scenario.count())
                .from(scenario)
                .where(predicate)
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<ScenarioEntity> findByIsActiveTrueAndIdOrNameContaining(String search, Pageable pageable) {
        QScenarioEntity scenario = QScenarioEntity.scenarioEntity;

        BooleanExpression predicate = scenario.isActive.isTrue()
                .and(
                        scenario.scenarioId.containsIgnoreCase(search)
                                .or(scenario.scenarioName.containsIgnoreCase(search))
                );

        List<ScenarioEntity> content = queryFactory
                .selectFrom(scenario)
                .where(predicate)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(scenario.count())
                .from(scenario)
                .where(predicate)
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }
}
