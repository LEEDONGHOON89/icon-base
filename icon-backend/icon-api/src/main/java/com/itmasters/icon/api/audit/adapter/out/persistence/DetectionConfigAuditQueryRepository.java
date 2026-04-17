package com.itmasters.icon.api.audit.adapter.out.persistence;

import com.itmasters.icon.api.audit.dto.AuditSearchRequest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.itmasters.icon.api.audit.adapter.out.persistence.QDetectionConfigAuditEntity.detectionConfigAuditEntity;

/**
 * 감사 로그 QueryDSL Repository
 */
@Repository
@RequiredArgsConstructor
public class DetectionConfigAuditQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 통합 검색 (모든 필터 선택적 적용)
     */
    public Page<DetectionConfigAuditEntity> searchAuditHistory(AuditSearchRequest request, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        // 타입 필터
        if (request.getTargetType() != null) {
            builder.and(detectionConfigAuditEntity.targetType.eq(request.getTargetType()));
        }

        // 액션 필터
        if (request.getAction() != null) {
            builder.and(detectionConfigAuditEntity.action.eq(request.getAction()));
        }

        // 시작 날짜 필터
        if (request.getStartDate() != null) {
            builder.and(detectionConfigAuditEntity.changedAt.goe(request.getStartDate()));
        }

        // 종료 날짜 필터
        if (request.getEndDate() != null) {
            builder.and(detectionConfigAuditEntity.changedAt.loe(request.getEndDate()));
        }

        // 검색어 필터 (targetName, changedBy, targetId)
        if (request.getSearch() != null && !request.getSearch().isBlank()) {
            String search = request.getSearch().toLowerCase();
            builder.and(
                detectionConfigAuditEntity.targetName.lower().contains(search)
                    .or(detectionConfigAuditEntity.changedBy.lower().contains(search))
                    .or(detectionConfigAuditEntity.targetId.lower().contains(search))
            );
        }

        // 쿼리 실행
        List<DetectionConfigAuditEntity> content = queryFactory
                .selectFrom(detectionConfigAuditEntity)
                .where(builder)
                .orderBy(detectionConfigAuditEntity.changedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 카운트
        Long total = queryFactory
                .select(detectionConfigAuditEntity.count())
                .from(detectionConfigAuditEntity)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
