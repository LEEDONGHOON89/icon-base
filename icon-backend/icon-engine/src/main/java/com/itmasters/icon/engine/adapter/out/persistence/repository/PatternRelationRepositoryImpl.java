package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.PatternRelationEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QPatternRelationEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pattern Relations 커스텀 Repository 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class PatternRelationRepositoryImpl implements PatternRelationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<PatternRelationEntity> findHighConfidencePatterns(
            String approvalStatus,
            BigDecimal minConfidence) {

        QPatternRelationEntity q = QPatternRelationEntity.patternRelationEntity;

        return queryFactory
                .selectFrom(q)
                .where(
                        q.approvalStatus.eq(approvalStatus),
                        q.confidenceScore.goe(minConfidence)
                )
                .orderBy(q.confidenceScore.desc())
                .fetch();
    }

    @Override
    public List<PatternRelationEntity> findRecentPendingPatterns(int limit) {
        QPatternRelationEntity q = QPatternRelationEntity.patternRelationEntity;

        return queryFactory
                .selectFrom(q)
                .where(q.approvalStatus.eq("PENDING"))
                .orderBy(q.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Object[]> countByApprovalStatus(String dataSourceId) {
        QPatternRelationEntity q = QPatternRelationEntity.patternRelationEntity;

        List<com.querydsl.core.Tuple> results = queryFactory
                .select(
                        q.approvalStatus,
                        q.count()
                )
                .from(q)
                .where(q.dataSourceId.eq(dataSourceId))
                .groupBy(q.approvalStatus)
                .fetch();

        // Tuple을 Object[]로 변환
        return results.stream()
                .map(tuple -> new Object[]{
                        tuple.get(q.approvalStatus),
                        tuple.get(q.count())
                })
                .toList();
    }
}
