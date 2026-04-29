package com.itmasters.icon.api.analytics.adapter.out.persistence.repository;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectRuleEntity;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectScenarioEntity;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiEventStreamEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 엔티티 행적 조회 Repository
 *
 * entity_id가 group_key에 포함된 탐지 이력 및 이벤트를 조회합니다.
 * [2026-04-29] array_position(text[], ?) 타입 불일치 수정:
 *   PostgreSQL 18에서 JDBC 파라미터가 character varying으로 전달되어
 *   array_position(text[], character varying) 오버로드를 찾지 못하는 문제 발생.
 *   → CAST(:entityId AS text) = ANY(string_to_array(...)) 방식으로 전환.
 */
@Repository
public class EntityHistoryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * entity_id가 group_key에 포함된 시나리오 탐지 조회
     */
    @SuppressWarnings("unchecked")
    public List<ApiDetectScenarioEntity> findScenariosByEntityId(String entityId, int limit) {
        // [2026-04-29] array_position → CAST(:entityId AS text) = ANY() 로 변경 (타입 불일치 수정)
        String sql = """
            SELECT ds.* FROM detect_scenarios ds
            WHERE CAST(:entityId AS text) = ANY(string_to_array(ds.group_key, '|'))
            ORDER BY ds.detected_dt DESC
            LIMIT :limit
            """;

        return entityManager.createNativeQuery(sql, ApiDetectScenarioEntity.class)
                .setParameter("entityId", entityId)
                .setParameter("limit", limit)
                .getResultList();
    }

    /**
     * entity_id가 group_key에 포함된 집계(룰) 탐지 조회
     * Note: detect_rules 테이블을 사용 (ApiDetectRuleEntity 매핑)
     */
    @SuppressWarnings("unchecked")
    public List<ApiDetectRuleEntity> findAggregatesByEntityId(String entityId, int limit) {
        // [2026-04-29] array_position → CAST(:entityId AS text) = ANY() 로 변경 (타입 불일치 수정)
        String sql = """
            SELECT dr.* FROM detect_rules dr
            WHERE CAST(:entityId AS text) = ANY(string_to_array(dr.group_key, '|'))
            ORDER BY dr.detected_dt DESC
            LIMIT :limit
            """;

        return entityManager.createNativeQuery(sql, ApiDetectRuleEntity.class)
                .setParameter("entityId", entityId)
                .setParameter("limit", limit)
                .getResultList();
    }

    /**
     * entity_id가 group_key에 포함된 룰 탐지 조회
     */
    @SuppressWarnings("unchecked")
    public List<ApiDetectRuleEntity> findRulesByEntityId(String entityId, int limit) {
        // [2026-04-29] array_position → CAST(:entityId AS text) = ANY() 로 변경 (타입 불일치 수정)
        String sql = """
            SELECT dr.* FROM detect_rules dr
            WHERE CAST(:entityId AS text) = ANY(string_to_array(dr.group_key, '|'))
            ORDER BY dr.detected_dt DESC
            LIMIT :limit
            """;

        return entityManager.createNativeQuery(sql, ApiDetectRuleEntity.class)
                .setParameter("entityId", entityId)
                .setParameter("limit", limit)
                .getResultList();
    }

    /**
     * entity_id가 group_key에 포함된 이벤트 스트림 조회
     * (event_stream_groups 테이블을 통해 조회)
     */
    @SuppressWarnings("unchecked")
    public List<ApiEventStreamEntity> findEventsByEntityId(String entityId, int limit) {
        // [2026-04-29] array_position → CAST(:entityId AS text) = ANY() 로 변경 (타입 불일치 수정)
        String sql = """
            SELECT DISTINCT es.* FROM event_stream es
            INNER JOIN event_stream_groups esg ON es.event_stream_id = esg.event_stream_id
            WHERE CAST(:entityId AS text) = ANY(string_to_array(esg.group_key, '|'))
            ORDER BY es.event_dt DESC
            LIMIT :limit
            """;

        return entityManager.createNativeQuery(sql, ApiEventStreamEntity.class)
                .setParameter("entityId", entityId)
                .setParameter("limit", limit)
                .getResultList();
    }

    /**
     * entity_id 관련 시나리오 탐지 개수
     */
    public long countScenariosByEntityId(String entityId) {
        // [2026-04-29] array_position → CAST(:entityId AS text) = ANY() 로 변경 (타입 불일치 수정)
        String sql = """
            SELECT COUNT(*) FROM detect_scenarios ds
            WHERE CAST(:entityId AS text) = ANY(string_to_array(ds.group_key, '|'))
            """;

        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("entityId", entityId)
                .getSingleResult()).longValue();
    }

    /**
     * entity_id 관련 집계(룰) 탐지 개수
     */
    public long countAggregatesByEntityId(String entityId) {
        // [2026-04-29] array_position → CAST(:entityId AS text) = ANY() 로 변경 (타입 불일치 수정)
        String sql = """
            SELECT COUNT(*) FROM detect_rules dr
            WHERE CAST(:entityId AS text) = ANY(string_to_array(dr.group_key, '|'))
            """;

        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("entityId", entityId)
                .getSingleResult()).longValue();
    }

    /**
     * entity_id 관련 룰 탐지 개수
     */
    public long countRulesByEntityId(String entityId) {
        // [2026-04-29] array_position → CAST(:entityId AS text) = ANY() 로 변경 (타입 불일치 수정)
        String sql = """
            SELECT COUNT(*) FROM detect_rules dr
            WHERE CAST(:entityId AS text) = ANY(string_to_array(dr.group_key, '|'))
            """;

        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("entityId", entityId)
                .getSingleResult()).longValue();
    }

    /**
     * entity_id 관련 이벤트 개수
     */
    public long countEventsByEntityId(String entityId) {
        // [2026-04-29] array_position → CAST(:entityId AS text) = ANY() 로 변경 (타입 불일치 수정)
        String sql = """
            SELECT COUNT(DISTINCT es.event_stream_id) FROM event_stream es
            INNER JOIN event_stream_groups esg ON es.event_stream_id = esg.event_stream_id
            WHERE CAST(:entityId AS text) = ANY(string_to_array(esg.group_key, '|'))
            """;

        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("entityId", entityId)
                .getSingleResult()).longValue();
    }
}
