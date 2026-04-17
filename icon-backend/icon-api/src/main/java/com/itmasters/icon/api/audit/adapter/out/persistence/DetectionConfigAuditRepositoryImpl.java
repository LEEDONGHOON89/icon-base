package com.itmasters.icon.api.audit.adapter.out.persistence;

import com.itmasters.icon.api.audit.domain.ConfigAuditTargetType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detection Config Audit 커스텀 Repository 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class DetectionConfigAuditRepositoryImpl implements DetectionConfigAuditRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<DetectionConfigAuditEntity> findByDateRange(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        QDetectionConfigAuditEntity q = QDetectionConfigAuditEntity.detectionConfigAuditEntity;

        // 데이터 조회
        List<DetectionConfigAuditEntity> content = queryFactory
                .selectFrom(q)
                .where(q.changedAt.between(startDate, endDate))
                .orderBy(q.changedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회
        Long total = queryFactory
                .select(q.count())
                .from(q)
                .where(q.changedAt.between(startDate, endDate))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<DetectionConfigAuditEntity> findByTargetTypeAndDateRange(
            ConfigAuditTargetType targetType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        QDetectionConfigAuditEntity q = QDetectionConfigAuditEntity.detectionConfigAuditEntity;

        // 데이터 조회
        List<DetectionConfigAuditEntity> content = queryFactory
                .selectFrom(q)
                .where(
                        q.targetType.eq(targetType),
                        q.changedAt.between(startDate, endDate)
                )
                .orderBy(q.changedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회
        Long total = queryFactory
                .select(q.count())
                .from(q)
                .where(
                        q.targetType.eq(targetType),
                        q.changedAt.between(startDate, endDate)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
