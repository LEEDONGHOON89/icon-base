package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QEngineEventStreamEntity;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * EventStream Repository 구현체 (QueryDSL 사용)
 */
@Repository("engineEventStreamRepositoryImpl")
@RequiredArgsConstructor
public class EventStreamRepositoryImpl implements EventStreamRepository {
    
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    QEngineEventStreamEntity qEventStream = QEngineEventStreamEntity.engineEventStreamEntity;


    @Override
    public EngineEventStreamEntity save(EngineEventStreamEntity entity) {
        if (entity.getEventStreamId() == null) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }

    @Override
    public Optional<EngineEventStreamEntity> findById(Long id) {

        EngineEventStreamEntity result = queryFactory
                .selectFrom(qEventStream)
                .where(qEventStream.eventStreamId.eq(id))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public List<EngineEventStreamEntity> saveAll(List<EngineEventStreamEntity> entities) {
        List<EngineEventStreamEntity> savedEntities = new ArrayList<>();
        for (EngineEventStreamEntity entity : entities) {
            savedEntities.add(save(entity));
        }
        entityManager.flush();
        return savedEntities;
    }

    @Override
    public void deleteById(Long id) {
        queryFactory
                .delete(qEventStream)
                .where(qEventStream.eventStreamId.eq(id))
                .execute();
    }

    @Override
    public void delete(EngineEventStreamEntity entity) {
        if (entity.getEventStreamId() != null) {
            deleteById(entity.getEventStreamId());
        }
    }

    @Override
    @Deprecated
    public List<EngineEventStreamEntity> findByGroupKeyOrderByCreatedAtDesc(String groupKey) {
        // TODO: group_key 컬럼이 제거되어 사용 불가
        // aggregate.group_by_fields 기반 JSONB 쿼리로 대체 필요
        throw new UnsupportedOperationException(
            "findByGroupKeyOrderByCreatedAtDesc() is deprecated. " +
            "Use aggregate.group_by_fields based JSONB queries instead. " +
            "See findByFieldValuesAndEventDtBetween() for reference."
        );
    }

    @Override
    @Deprecated
    public List<EngineEventStreamEntity> findByGroupKeyAndCreatedAtBetween(String groupKey,
                                                                           LocalDateTime startDate,
                                                                           LocalDateTime endDate) {
        // TODO: group_key 컬럼이 제거되어 사용 불가
        // aggregate.group_by_fields 기반 JSONB 쿼리로 대체 필요
        throw new UnsupportedOperationException(
            "findByGroupKeyAndCreatedAtBetween() is deprecated. " +
            "Use aggregate.group_by_fields based JSONB queries instead. " +
            "See findByFieldValuesAndEventDtBetween() for reference."
        );
    }

    // Removed: findByEventTypeAndCreatedAtAfter - eventType field no longer exists
    // Use JSONB-based queries instead

    @Override
    @Deprecated
    public List<EngineEventStreamEntity> findByGroupKeyAndEventDtInRange(String groupKey,
                                                                          LocalDateTime startInclusive,
                                                                          LocalDateTime endExclusive) {
        // TODO: group_key 컬럼이 제거되어 사용 불가
        // aggregate.group_by_fields 기반 JSONB 쿼리로 대체 필요
        throw new UnsupportedOperationException(
            "findByGroupKeyAndEventDtInRange() is deprecated. " +
            "Use aggregate.group_by_fields based JSONB queries instead. " +
            "See findByFieldValuesAndEventDtBetween() for reference."
        );
    }

    @Override
    public List<EngineEventStreamEntity> findByMappedDataStorageIdIsNotNull() {
        return queryFactory
                .selectFrom(qEventStream)
                .where(qEventStream.mappedDataStorageId.isNotNull())
                .orderBy(qEventStream.eventDt.desc())
                .fetch();
    }

    @Override
    public List<EngineEventStreamEntity> findLargeTransfers(BigDecimal amount, LocalDateTime afterDate) {
        // JSONB에서 transaction_amount와 event_type 모두 추출하여 비교
        NumberTemplate<BigDecimal> transactionAmount = Expressions.numberTemplate(
            BigDecimal.class, 
            "CAST(JSON_UNQUOTE(JSON_EXTRACT({0}, '$.transaction_amount')) AS DECIMAL(18,2))", 
            qEventStream.eventData
        );
        
        // JSONB에서 event_type 추출
        return queryFactory
                .selectFrom(qEventStream)
                .where(Expressions.stringTemplate("JSON_UNQUOTE(JSON_EXTRACT({0}, '$.event_type'))", qEventStream.eventData).eq("TRANSFER")
                        .and(transactionAmount.gt(amount))
                        .and(qEventStream.eventDt.goe(afterDate)))
                .orderBy(qEventStream.eventDt.desc())
                .fetch();
    }

    @Override
    @Deprecated
    public List<EngineEventStreamEntity> findTransfersAfterOTP(String groupKey, LocalDateTime startDate) {
        // TODO: group_key 컬럼이 제거되어 네이티브 쿼리 사용 불가
        // aggregate.group_by_fields 기반으로 JSONB 조인 쿼리 재작성 필요
        throw new UnsupportedOperationException(
            "findTransfersAfterOTP() is deprecated. " +
            "group_key column has been removed. " +
            "Rewrite query using aggregate.group_by_fields for JSONB-based grouping."
        );
    }

    @Override
    public Optional<EngineEventStreamEntity> findByIdWithOriginalData(Long id) {
        EngineEventStreamEntity result = queryFactory
                .selectFrom(qEventStream)
                .leftJoin(qEventStream.mappedDataStorage).fetchJoin()
                .where(qEventStream.eventStreamId.eq(id))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<EngineEventStreamEntity> findByMappedDataStorageId(Long mappedDataStorageId) {
        EngineEventStreamEntity result = queryFactory
                .selectFrom(qEventStream)
                .where(qEventStream.mappedDataStorageId.eq(mappedDataStorageId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    @Deprecated
    public List<EngineEventStreamEntity> findByGroupKeyAndCreatedAtAfterOrderByCreatedAtDesc(String groupKey,
                                                                                             LocalDateTime cutoffTime,
                                                                                             int limit) {
        // TODO: group_key 컬럼이 제거되어 사용 불가
        // aggregate.group_by_fields 기반 JSONB 쿼리로 대체 필요
        throw new UnsupportedOperationException(
            "findByGroupKeyAndCreatedAtAfterOrderByCreatedAtDesc() is deprecated. " +
            "Use aggregate.group_by_fields based JSONB queries instead. " +
            "See findByFieldValuesAndEventDtBetween() for reference."
        );
    }

    // Removed: findByGroupKeyAndEventTypeAndCreatedAtAfterOrderByCreatedAtDesc - eventType field no longer exists
    // Use JSONB-based queries instead

    @Override
    @Deprecated
    public long countByGroupKeyAndCreatedAtAfter(String groupKey, LocalDateTime cutoffTime) {
        // TODO: group_key 컬럼이 제거되어 사용 불가
        // aggregate.group_by_fields 기반 JSONB 쿼리로 대체 필요
        throw new UnsupportedOperationException(
            "countByGroupKeyAndCreatedAtAfter() is deprecated. " +
            "Use aggregate.group_by_fields based JSONB queries instead."
        );
    }

    // Removed: countByGroupKeyAndEventTypeAndCreatedAtAfter - eventType field no longer exists
    // Use JSONB-based queries instead

    @Override
    @Deprecated
    public boolean existsByGroupKeyAndCreatedAt(String groupKey, LocalDateTime timestamp) {
        // TODO: group_key 컬럼이 제거되어 사용 불가
        // aggregate.group_by_fields 기반 JSONB 쿼리로 대체 필요
        throw new UnsupportedOperationException(
            "existsByGroupKeyAndCreatedAt() is deprecated. " +
            "Use aggregate.group_by_fields based JSONB queries instead."
        );
    }

    @Override
    @Deprecated
    public List<String> findDistinctGroupKeys() {
        // TODO: group_key 컬럼이 제거되어 사용 불가
        // aggregate.group_by_fields 기반 JSONB 쿼리로 대체 필요
        throw new UnsupportedOperationException(
            "findDistinctGroupKeys() is deprecated. " +
            "Use aggregate.group_by_fields based JSONB queries instead. " +
            "See findDistinctFieldCombinations() for reference."
        );
    }

    @Override
    @Deprecated
    public java.util.List<String> findDistinctGroupKeysByExecDsMpId(Long execDsMpId) {
        // TODO: group_key 컬럼이 제거되어 사용 불가
        // aggregate.group_by_fields 기반 JSONB 쿼리로 대체 필요
        throw new UnsupportedOperationException(
            "findDistinctGroupKeysByExecDsMpId() is deprecated. " +
            "Use aggregate.group_by_fields based JSONB queries instead. " +
            "See findDistinctFieldCombinations() for reference."
        );
    }
    
    @Override
    public List<EngineEventStreamEntity> findByTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            return List.of();
        }
        
        return queryFactory
                .selectFrom(qEventStream)
                .where(qEventStream.transactionId.eq(transactionId))
                .orderBy(qEventStream.eventDt.asc())
                .fetch();
    }
    
    @Override
    public List<java.util.Map<String, String>> findDistinctFieldCombinations(
            List<String> fields,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        if (fields == null || fields.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // SELECT 절 동적 생성: jsonb_extract_path_text(event_data, 'field_name') as field_0, field_1, ...
        StringBuilder selectClause = new StringBuilder("SELECT DISTINCT ");
        StringBuilder whereClause = new StringBuilder();

        for (int i = 0; i < fields.size(); i++) {
            String fieldName = fields.get(i);
            String alias = "field_" + i;

            if (i > 0) {
                selectClause.append(", ");
                whereClause.append(" AND ");
            }

            selectClause.append("jsonb_extract_path_text(e.event_data, '")
                       .append(fieldName)
                       .append("') as ")
                       .append(alias);

            whereClause.append("jsonb_extract_path_text(e.event_data, '")
                      .append(fieldName)
                      .append("') IS NOT NULL");
        }

        // 전체 쿼리 구성 (exec_ds_mp_id 필터 제거, 기간 필터 추가)
        String sql = selectClause.toString() +
                    " FROM event_stream e" +
                    " WHERE " + whereClause.toString() +
                    " AND e.event_dt >= :startDate" +
                    " AND e.event_dt <= :endDate";

        // 네이티브 쿼리 실행 - raw List로 받기 (단일 필드면 String, 다중 필드면 Object[] 반환)
        @SuppressWarnings("unchecked")
        List<Object> results = entityManager.createNativeQuery(sql)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();

        // 결과 → Map<String, String> 변환
        List<java.util.Map<String, String>> combinations = new ArrayList<>();
        for (Object result : results) {
            java.util.Map<String, String> combination = new java.util.HashMap<>();

            if (fields.size() == 1) {
                // 단일 필드: result는 String
                String fieldName = fields.get(0);
                String value = result != null ? result.toString() : null;
                combination.put(fieldName, value);
            } else {
                // 다중 필드: result는 Object[]
                Object[] row = (Object[]) result;
                for (int i = 0; i < fields.size(); i++) {
                    String fieldName = fields.get(i);
                    String value = row[i] != null ? row[i].toString() : null;
                    combination.put(fieldName, value);
                }
            }

            combinations.add(combination);
        }

        return combinations;
    }

    @Override
    public List<EngineEventStreamEntity> findByFieldValuesAndEventDtBetween(
            java.util.Map<String, String> groupKeyValues,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        if (groupKeyValues == null || groupKeyValues.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // WHERE 절 동적 생성: jsonb_extract_path_text(event_data, 'field_name') = 'value' for each field
        StringBuilder whereClause = new StringBuilder();
        int paramIndex = 0;

        for (java.util.Map.Entry<String, String> entry : groupKeyValues.entrySet()) {
            String fieldName = entry.getKey();
            // SQL 인젝션 방지를 위해 필드명은 직접 임베드하고, 값은 파라미터로 바인딩
            if (paramIndex > 0) {
                whereClause.append(" AND ");
            }
            whereClause.append("jsonb_extract_path_text(e.event_data, '")
                      .append(fieldName)
                      .append("') = :value")
                      .append(paramIndex);
            paramIndex++;
        }

        // 전체 쿼리 구성
        String sql = "SELECT e.* FROM event_stream e " +
                    "WHERE " + whereClause.toString() +
                    " AND e.event_dt >= :startDate" +
                    " AND e.event_dt <= :endDate" +
                    " ORDER BY e.event_dt ASC";

        // 네이티브 쿼리 생성 및 파라미터 바인딩
        jakarta.persistence.Query query = entityManager.createNativeQuery(sql, EngineEventStreamEntity.class)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate);

        // 필드값 파라미터 바인딩
        paramIndex = 0;
        for (java.util.Map.Entry<String, String> entry : groupKeyValues.entrySet()) {
            query.setParameter("value" + paramIndex, entry.getValue());
            paramIndex++;
        }

        // 쿼리 실행
        @SuppressWarnings("unchecked")
        List<EngineEventStreamEntity> results = query.getResultList();

        return results == null ? java.util.Collections.emptyList() : results;
    }

    @Override
    public Optional<LocalDateTime> findLatestEventTime() {
        LocalDateTime result = queryFactory
                .select(qEventStream.eventDt.max())
                .from(qEventStream)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<LocalDateTime> findLatestEventTimeByExecDsMpId(Long execDsMpId) {
        if (execDsMpId == null) {
            return Optional.empty();
        }

        // mapped_storages와 JOIN하여 exec_ds_mp_id로 필터링된 event_stream의 MAX(event_dt) 조회
        String sql = "SELECT MAX(e.event_dt) " +
                    "FROM event_stream e " +
                    "INNER JOIN mapped_storages m ON e.mapped_storage_id = m.mapped_storage_id " +
                    "WHERE m.exec_ds_mp_id = :execDsMpId";

        Object result = entityManager.createNativeQuery(sql)
                .setParameter("execDsMpId", execDsMpId)
                .getSingleResult();

        if (result == null) {
            return Optional.empty();
        }

        // PostgreSQL은 timestamp를 java.sql.Timestamp로 반환
        if (result instanceof java.sql.Timestamp) {
            return Optional.of(((java.sql.Timestamp) result).toLocalDateTime());
        }

        return Optional.empty();
    }

    @Override
    public List<EngineEventStreamEntity> findByExecDsMpIdAndGroupKeyValues(
            Long execDsMpId,
            java.util.Map<String, String> groupKeyValues) {

        if (execDsMpId == null || groupKeyValues == null || groupKeyValues.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // 1. mapped_storages에서 exec_ds_mp_id로 mapped_storage_id 목록 조회
        String mappedStorageQuery = "SELECT mapped_storage_id FROM mapped_storages WHERE exec_ds_mp_id = :execDsMpId";
        @SuppressWarnings("unchecked")
        List<Object> rawResults = entityManager.createNativeQuery(mappedStorageQuery)
                .setParameter("execDsMpId", execDsMpId)
                .getResultList();

        List<Long> mappedStorageIds = rawResults.stream()
                .map(id -> ((Number) id).longValue())
                .collect(java.util.stream.Collectors.toList());

        if (mappedStorageIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // 2. event_stream에서 mapped_storage_id + groupKeyValues 조건으로 필터링
        StringBuilder sql = new StringBuilder("SELECT * FROM event_stream WHERE mapped_storage_id IN (:mappedStorageIds)");

        // groupKeyValues 조건 추가
        int paramIndex = 0;
        for (java.util.Map.Entry<String, String> entry : groupKeyValues.entrySet()) {
            sql.append(" AND event_data->>'").append(entry.getKey()).append("' = :value").append(paramIndex);
            paramIndex++;
        }

        sql.append(" ORDER BY event_dt DESC");

        // 쿼리 생성
        jakarta.persistence.Query query = entityManager.createNativeQuery(sql.toString(), EngineEventStreamEntity.class);
        query.setParameter("mappedStorageIds", mappedStorageIds);

        // 파라미터 바인딩
        paramIndex = 0;
        for (java.util.Map.Entry<String, String> entry : groupKeyValues.entrySet()) {
            query.setParameter("value" + paramIndex, entry.getValue());
            paramIndex++;
        }

        // 쿼리 실행
        @SuppressWarnings("unchecked")
        List<EngineEventStreamEntity> results = query.getResultList();

        return results == null ? java.util.Collections.emptyList() : results;
    }

    @Override
    public List<EngineEventStreamEntity> findByExecDsMpId(Long execDsMpId) {
        // 1. mapped_storages에서 exec_ds_mp_id로 mapped_storage_id 목록 조회
        String mappedStorageQuery = "SELECT mapped_storage_id FROM mapped_storages WHERE exec_ds_mp_id = :execDsMpId";
        @SuppressWarnings("unchecked")
        List<Object> rawResults = entityManager.createNativeQuery(mappedStorageQuery)
                .setParameter("execDsMpId", execDsMpId)
                .getResultList();

        List<Long> mappedStorageIds = rawResults.stream()
                .map(id -> ((Number) id).longValue())
                .collect(java.util.stream.Collectors.toList());

        if (mappedStorageIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // 2. event_stream에서 mapped_storage_id로 필터링
        String sql = "SELECT * FROM event_stream WHERE mapped_storage_id IN (:mappedStorageIds) ORDER BY event_dt DESC";

        // 쿼리 실행
        @SuppressWarnings("unchecked")
        List<EngineEventStreamEntity> results = entityManager.createNativeQuery(sql, EngineEventStreamEntity.class)
                .setParameter("mappedStorageIds", mappedStorageIds)
                .getResultList();

        return results == null ? java.util.Collections.emptyList() : results;
    }

    @Override
    public List<EngineEventStreamEntity> findByMappedStorageIdAndGroupKeyValues(
            Long mappedStorageId,
            java.util.Map<String, String> groupKeyValues) {

        if (mappedStorageId == null || groupKeyValues == null || groupKeyValues.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // mapped_storage_id를 직접 사용하여 event_stream 조회
        StringBuilder sql = new StringBuilder("SELECT * FROM event_stream WHERE mapped_storage_id = :mappedStorageId");

        // groupKeyValues 조건 추가
        int paramIndex = 0;
        for (java.util.Map.Entry<String, String> entry : groupKeyValues.entrySet()) {
            sql.append(" AND event_data->>'").append(entry.getKey()).append("' = :value").append(paramIndex);
            paramIndex++;
        }

        sql.append(" ORDER BY event_dt DESC");

        // 쿼리 생성
        jakarta.persistence.Query query = entityManager.createNativeQuery(sql.toString(), EngineEventStreamEntity.class);
        query.setParameter("mappedStorageId", mappedStorageId);

        // 파라미터 바인딩
        paramIndex = 0;
        for (java.util.Map.Entry<String, String> entry : groupKeyValues.entrySet()) {
            query.setParameter("value" + paramIndex, entry.getValue());
            paramIndex++;
        }

        // 쿼리 실행
        @SuppressWarnings("unchecked")
        List<EngineEventStreamEntity> results = query.getResultList();

        return results == null ? java.util.Collections.emptyList() : results;
    }
}
