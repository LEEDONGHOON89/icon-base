package com.itmasters.icon.api.analytics.adapter.out.persistence.repository;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiEventStreamEntity;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.QApiEventStreamEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;

@Repository("apiEventStreamRepositoryImpl")
@RequiredArgsConstructor
public class EventStreamRepositoryImpl implements EventStreamRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public List<ApiEventStreamEntity> findByGroupKeyBetween(String groupKey, LocalDateTime startInclusive, LocalDateTime endInclusive, int limit) {
        // event_stream_groups 테이블을 통해 group_key로 event_stream_id 조회 후, event_stream 조회
        String sql = """
            SELECT DISTINCT es.*
            FROM event_stream es
            INNER JOIN event_stream_groups esg ON es.event_stream_id = esg.event_stream_id
            WHERE esg.group_key = :groupKey
              AND es.event_dt >= :startInclusive
              AND es.event_dt <= :endInclusive
            ORDER BY es.event_dt ASC
            LIMIT :limit
            """;

        return entityManager.createNativeQuery(sql, ApiEventStreamEntity.class)
                .setParameter("groupKey", groupKey)
                .setParameter("startInclusive", startInclusive)
                .setParameter("endInclusive", endInclusive)
                .setParameter("limit", limit > 0 ? limit : 100)
                .getResultList();
    }
}

