package com.itmasters.icon.api.detectaction.adapter.out.persistence;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.QApiDetectActionEntity;
import com.itmasters.icon.api.detectaction.dto.DetectActionDto;
import com.itmasters.icon.api.scenario.adapter.persistence.entity.QScenarioEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 탐지 조치 QueryDSL Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class DetectActionQueryRepositoryImpl implements DetectActionQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<DetectActionDto> findDetectActions(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String riskLevel,
            String actionStatus,
            Pageable pageable
    ) {
        QApiDetectActionEntity a = QApiDetectActionEntity.apiDetectActionEntity;
        QScenarioEntity s = QScenarioEntity.scenarioEntity;

        // 기본 쿼리
        JPAQuery<DetectActionDto> query = queryFactory
                .select(Projections.constructor(DetectActionDto.class,
                        a.detectActionId,
                        a.detectScenarioId,
                        a.scenarioId,
                        s.scenarioName,
                        a.groupKey,
                        a.actionType,
                        a.actionStatus,
                        a.riskLevel,
                        a.actionMemo,
                        a.actionReason,
                        a.requestedBy,
                        a.requestedAt,
                        a.approvedBy,
                        a.approvedAt,
                        a.completedBy,
                        a.completedAt,
                        a.regDt
                ))
                .from(a)
                .leftJoin(s).on(a.scenarioId.eq(s.scenarioId))
                .where(
                        dateRangePredicate(a, startDate, endDate),
                        riskLevelPredicate(a, riskLevel),
                        actionStatusPredicate(a, actionStatus)
                )
                .orderBy(a.requestedAt.desc());

        // 전체 개수 조회
        long total = query.fetchCount();

        // 페이징 적용하여 결과 조회
        List<DetectActionDto> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 날짜 범위 조건
     */
    private BooleanExpression dateRangePredicate(QApiDetectActionEntity a, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            return a.requestedAt.between(startDate, endDate);
        } else if (startDate != null) {
            return a.requestedAt.goe(startDate);
        } else if (endDate != null) {
            return a.requestedAt.loe(endDate);
        }
        return null;
    }

    /**
     * 위험수준 조건
     */
    private BooleanExpression riskLevelPredicate(QApiDetectActionEntity a, String riskLevel) {
        return riskLevel != null ? a.riskLevel.eq(riskLevel) : null;
    }

    /**
     * 조치 상태 조건 (다중 선택 지원)
     */
    private BooleanExpression actionStatusPredicate(QApiDetectActionEntity a, String actionStatus) {
        if (actionStatus == null || actionStatus.isEmpty()) {
            return null;
        }

        // 쉼표로 구분된 여러 상태 처리
        if (actionStatus.contains(",")) {
            String[] statuses = actionStatus.split(",");
            return a.actionStatus.in(statuses);
        }

        // 단일 상태
        return a.actionStatus.eq(actionStatus);
    }
}
