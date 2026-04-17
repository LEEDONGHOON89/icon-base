package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.RelationRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Relation Rules Repository
 *
 * 관계 자동 생성 규칙 관리
 *
 * 주요 쿼리 패턴:

   * -DataSource별 활성 규칙 조회
   * -특정 엔티티 타입 조합의 규칙 조회

 *
 * @since 2025-02-01
 */
@Repository
public interface RelationRuleRepository extends JpaRepository<RelationRuleEntity, Long> {

    /**
     * DataSource별 활성 규칙 전체 조회
     *
     * 이벤트 처리 시 가장 많이 사용되는 쿼리
     *
     * @param dataSourceId DataSource ID
     * @param isActive 활성화 여부 (true)
     * @return 활성 규칙 목록
     */
    List<RelationRuleEntity> findByDataSourceIdAndIsActive(String dataSourceId, Boolean isActive);

    /**
     * DataSource별 모든 규칙 조회 (활성/비활성 포함)
     *
     * @param dataSourceId DataSource ID
     * @return 규칙 목록
     */
    List<RelationRuleEntity> findByDataSourceId(String dataSourceId);

    /**
     * From Entity Type으로 규칙 조회
     *
     * 예: "CUSTOMER로 시작하는 모든 관계 규칙"
     *
     * @param fromEntityType From 엔티티 타입
     * @param isActive 활성화 여부
     * @return 규칙 목록
     */
    List<RelationRuleEntity> findByFromEntityTypeAndIsActive(String fromEntityType, Boolean isActive);

    /**
     * Relation Type으로 규칙 조회
     *
     * 예: "OWNS 관계 규칙만"
     *
     * @param relationType 관계 타입
     * @param isActive 활성화 여부
     * @return 규칙 목록
     */
    List<RelationRuleEntity> findByRelationTypeAndIsActive(String relationType, Boolean isActive);

    /**
     * DataSource + From/To Entity Type으로 규칙 조회
     *
     * 특정 엔티티 조합의 규칙 찾기
     *
     * @param dataSourceId DataSource ID
     * @param fromEntityType From 엔티티 타입
     * @param toEntityType To 엔티티 타입
     * @param isActive 활성화 여부
     * @return 규칙 목록
     */
    List<RelationRuleEntity> findByDataSourceIdAndFromEntityTypeAndToEntityTypeAndIsActive(
            String dataSourceId,
            String fromEntityType,
            String toEntityType,
            Boolean isActive
    );
}
