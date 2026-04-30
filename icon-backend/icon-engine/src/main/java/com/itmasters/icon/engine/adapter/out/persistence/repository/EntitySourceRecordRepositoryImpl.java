package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EntitySourceRecordEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QEntitySourceRecordEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 엔티티 원본 데이터 이력 커스텀 Repository 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class EntitySourceRecordRepositoryImpl implements EntitySourceRecordRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EntitySourceRecordEntity> findByEntityTypeAndEntityIdAsOf(
            String entityType,
            String entityId,
            LocalDateTime asOfTime) {

        QEntitySourceRecordEntity q = QEntitySourceRecordEntity.entitySourceRecordEntity;

        return queryFactory
                .selectFrom(q)
                .where(
                        q.entityType.eq(entityType),
                        q.entityId.eq(entityId),
                        q.receivedAt.loe(asOfTime)
                )
                .orderBy(q.receivedAt.desc())
                .fetch();
    }

    @Override
    public List<EntitySourceRecordEntity> findRecentByDataSourceId(String dataSourceId, int limit) {
        QEntitySourceRecordEntity q = QEntitySourceRecordEntity.entitySourceRecordEntity;

        return queryFactory
                .selectFrom(q)
                .where(q.dataSourceId.eq(dataSourceId))
                .orderBy(q.receivedAt.desc())
                .limit(limit)
                .fetch();
    }
}
