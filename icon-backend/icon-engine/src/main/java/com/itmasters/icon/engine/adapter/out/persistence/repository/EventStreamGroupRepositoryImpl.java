package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.QEventStreamGroupEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event Stream Groups 커스텀 Repository 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class EventStreamGroupRepositoryImpl implements EventStreamGroupRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public List<Long> findEventStreamIdsByAggregateIdAndGroupKey(String ruleId, String groupKey) {
        QEventStreamGroupEntity q = QEventStreamGroupEntity.eventStreamGroupEntity;

        return queryFactory
                .select(q.eventStreamId)
                .from(q)
                .where(
                        q.ruleId.eq(ruleId),
                        q.groupKey.eq(groupKey)
                )
                .fetch();
    }

    @Override
    public List<Long> findEventStreamIdsByAggregateAndGroupKeyAndDateRange(
            String ruleId,
            String groupKey,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        // event_stream 테이블과 JOIN하므로 Native SQL 유지
        String sql = "SELECT g.event_stream_id FROM event_stream_groups g " +
                     "INNER JOIN event_stream e ON g.event_stream_id = e.event_stream_id " +
                     "WHERE g.aggregate_id = :ruleId " +
                     "AND g.group_key = :groupKey " +
                     "AND e.event_dt >= :startDate " +
                     "AND e.event_dt <= :endDate";

        @SuppressWarnings("unchecked")
        List<Long> results = entityManager.createNativeQuery(sql)
                .setParameter("ruleId", ruleId)
                .setParameter("groupKey", groupKey)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();

        return results.stream()
                .map(obj -> obj instanceof Number ? ((Number) obj).longValue() : (Long) obj)
                .toList();
    }

    @Override
    public void deleteByEventStreamId(Long eventStreamId) {
        QEventStreamGroupEntity q = QEventStreamGroupEntity.eventStreamGroupEntity;

        queryFactory
                .delete(q)
                .where(q.eventStreamId.eq(eventStreamId))
                .execute();
    }

    @Override
    public List<String> findDistinctGroupKeysByAggregateId(String ruleId) {
        QEventStreamGroupEntity q = QEventStreamGroupEntity.eventStreamGroupEntity;

        return queryFactory
                .select(q.groupKey)
                .distinct()
                .from(q)
                .where(q.ruleId.eq(ruleId))
                .fetch();
    }

    @Override
    public List<String> findDistinctGroupKeysByAggregateIdAndDateRange(
            String ruleId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        // event_stream 테이블과 JOIN하므로 Native SQL 유지
        String sql = "SELECT DISTINCT g.group_key FROM event_stream_groups g " +
                     "INNER JOIN event_stream e ON g.event_stream_id = e.event_stream_id " +
                     "WHERE g.aggregate_id = :ruleId " +
                     "AND e.event_dt >= :startDate " +
                     "AND e.event_dt <= :endDate";

        @SuppressWarnings("unchecked")
        List<String> results = entityManager.createNativeQuery(sql)
                .setParameter("ruleId", ruleId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();

        return results;
    }

    @Override
    public List<String> findDistinctGroupKeysByExecDsMpIdAndAggregateId(Long execDsMpId, String ruleId) {
        // event_stream, mapped_storages 테이블과 JOIN하므로 Native SQL 유지
        String sql = "SELECT DISTINCT g.group_key FROM event_stream_groups g " +
                     "INNER JOIN event_stream e ON g.event_stream_id = e.event_stream_id " +
                     "INNER JOIN mapped_storages m ON e.mapped_storage_id = m.mapped_storage_id " +
                     "WHERE g.aggregate_id = :ruleId " +
                     "AND m.exec_ds_mp_id = :execDsMpId";

        @SuppressWarnings("unchecked")
        List<String> results = entityManager.createNativeQuery(sql)
                .setParameter("ruleId", ruleId)
                .setParameter("execDsMpId", execDsMpId)
                .getResultList();

        return results;
    }

    @Override
    public List<String> findDistinctGroupKeysByMappedStorageIdAndAggregateId(Long mappedStorageId, String ruleId) {
        // mapped_storage_id를 직접 사용하여 exec_ds_mp_id 변환 없이 조회
        String sql = "SELECT DISTINCT g.group_key FROM event_stream_groups g " +
                     "INNER JOIN event_stream e ON g.event_stream_id = e.event_stream_id " +
                     "WHERE g.aggregate_id = :ruleId " +
                     "AND e.mapped_storage_id = :mappedStorageId";

        @SuppressWarnings("unchecked")
        List<String> results = entityManager.createNativeQuery(sql)
                .setParameter("ruleId", ruleId)
                .setParameter("mappedStorageId", mappedStorageId)
                .getResultList();

        return results;
    }
}
