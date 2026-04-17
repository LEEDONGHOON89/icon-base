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
 * PostgreSQL의 array_position() 함수를 활용합니다.
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
        String sql = """
            SELECT ds.* FROM detect_scenarios ds
            WHERE array_position(string_to_array(ds.group_key, '|'), :entityId) IS NOT NULL
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
        String sql = """
            SELECT dr.* FROM detect_rules dr
            WHERE array_position(string_to_array(dr.group_key, '|'), :entityId) IS NOT NULL
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
        String sql = """
            SELECT dr.* FROM detect_rules dr
            WHERE array_position(string_to_array(dr.group_key, '|'), :entityId) IS NOT NULL
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
        String sql = """
            SELECT DISTINCT es.* FROM event_stream es
            INNER JOIN event_stream_groups esg ON es.event_stream_id = esg.event_stream_id
            WHERE array_position(string_to_array(esg.group_key, '|'), :entityId) IS NOT NULL
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
        String sql = """
            SELECT COUNT(*) FROM detect_scenarios ds
            WHERE array_position(string_to_array(ds.group_key, '|'), :entityId) IS NOT NULL
            """;

        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("entityId", entityId)
                .getSingleResult()).longValue();
    }

    /**
     * entity_id 관련 집계(룰) 탐지 개수
     */
    public long countAggregatesByEntityId(String entityId) {
        String sql = """
            SELECT COUNT(*) FROM detect_rules dr
            WHERE array_position(string_to_array(dr.group_key, '|'), :entityId) IS NOT NULL
            """;

        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("entityId", entityId)
                .getSingleResult()).longValue();
    }

    /**
     * entity_id 관련 룰 탐지 개수
     */
    public long countRulesByEntityId(String entityId) {
        String sql = """
            SELECT COUNT(*) FROM detect_rules dr
            WHERE array_position(string_to_array(dr.group_key, '|'), :entityId) IS NOT NULL
            """;

        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("entityId", entityId)
                .getSingleResult()).longValue();
    }

    /**
     * entity_id 관련 이벤트 개수
     */
    public long countEventsByEntityId(String entityId) {
        String sql = """
            SELECT COUNT(DISTINCT es.event_stream_id) FROM event_stream es
            INNER JOIN event_stream_groups esg ON es.event_stream_id = esg.event_stream_id
            WHERE array_position(string_to_array(esg.group_key, '|'), :entityId) IS NOT NULL
            """;

        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("entityId", entityId)
                .getSingleResult()).longValue();
    }
}
