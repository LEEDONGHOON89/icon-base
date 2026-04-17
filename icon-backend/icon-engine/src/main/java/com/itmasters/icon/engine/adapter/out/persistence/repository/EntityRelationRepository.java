package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Entity Relations Repository
 *
 * entity_attributes 간 상태 관계 조회/저장
 *
 * 주요 쿼리 패턴:

   * -특정 엔티티의 모든 관계 조회 (From/To 방향)
   * -특정 관계 타입만 조회 (OWNS, USES 등)
   * -UPSERT를 위한 중복 체크

 *
 * QueryDSL 기반 복잡한 쿼리는 EntityRelationRepositoryCustom에서 구현
 *
 * @since 2025-02-01
 */
@Repository
public interface EntityRelationRepository extends JpaRepository<EntityRelationEntity, Long>, EntityRelationRepositoryCustom {

    /**
     * From Entity 기준 모든 관계 조회
     *
     * 예: "CUSTOMER C001의 모든 관계 (OWNS, USES 등)"
     *
     * @param fromEntityType From 엔티티 타입
     * @param fromEntityId From 엔티티 ID
     * @return 관계 목록
     */
    List<EntityRelationEntity> findByFromEntityTypeAndFromEntityId(
            String fromEntityType,
            String fromEntityId
    );

    /**
     * From Entity + 관계 타입으로 조회
     *
     * 예: "CUSTOMER C001의 OWNS 관계만"
     *
     * @param fromEntityType From 엔티티 타입
     * @param fromEntityId From 엔티티 ID
     * @param relationType 관계 타입 (OWNS, USES, ACCESSES)
     * @return 관계 목록
     */
    List<EntityRelationEntity> findByFromEntityTypeAndFromEntityIdAndRelationType(
            String fromEntityType,
            String fromEntityId,
            String relationType
    );

    /**
     * To Entity 기준 모든 관계 조회 (역방향)
     *
     * 예: "ACCOUNT A1234를 소유한 CUSTOMER 찾기"
     *
     * @param toEntityType To 엔티티 타입
     * @param toEntityId To 엔티티 ID
     * @return 관계 목록
     */
    List<EntityRelationEntity> findByToEntityTypeAndToEntityId(
            String toEntityType,
            String toEntityId
    );

    /**
     * To Entity + 관계 타입으로 조회 (역방향)
     *
     * 예: "ACCOUNT A1234를 OWNS하는 CUSTOMER 찾기"
     *
     * @param toEntityType To 엔티티 타입
     * @param toEntityId To 엔티티 ID
     * @param relationType 관계 타입
     * @return 관계 목록
     */
    List<EntityRelationEntity> findByToEntityTypeAndToEntityIdAndRelationType(
            String toEntityType,
            String toEntityId,
            String relationType
    );

    /**
     * 특정 관계 존재 여부 확인 (UPSERT 전 체크)
     *
     * UNIQUE 제약조건과 동일한 조건으로 조회
     *
     * @param fromEntityType From 엔티티 타입
     * @param fromEntityId From 엔티티 ID
     * @param relationType 관계 타입
     * @param toEntityType To 엔티티 타입
     * @param toEntityId To 엔티티 ID
     * @return 기존 관계 (있으면 Optional.of, 없으면 Optional.empty)
     */
    Optional<EntityRelationEntity> findByFromEntityTypeAndFromEntityIdAndRelationTypeAndToEntityTypeAndToEntityId(
            String fromEntityType,
            String fromEntityId,
            String relationType,
            String toEntityType,
            String toEntityId
    );

    /**
     * 특정 관계 타입의 모든 관계 조회
     *
     * 예: "모든 USES 관계" (디바이스 공유 분석 등)
     *
     * @param relationType 관계 타입
     * @return 관계 목록
     */
    List<EntityRelationEntity> findByRelationType(String relationType);

    /**
     * 디바이스 공유 탐지 (같은 디바이스를 사용하는 고객들) - QueryDSL로 구현
     *
     * USES 관계에서 같은 to_entity_id(DEVICE)를 가진 관계들을 찾습니다.
     * 서브쿼리 with GROUP BY and HAVING 사용
     */
    // @Query 제거됨 - EntityRelationRepositoryCustom 인터페이스에서 선언, EntityRelationRepositoryImpl에서 구현
    // List<EntityRelationEntity> findSharedDevices(String relationType, String toEntityType);

    /**
     * From Entity의 특정 타입 To Entity들 조회
     *
     * 예: "CUSTOMER C001이 소유한 모든 ACCOUNT"
     *
     * @param fromEntityType From 엔티티 타입
     * @param fromEntityId From 엔티티 ID
     * @param relationType 관계 타입
     * @param toEntityType To 엔티티 타입
     * @return 관계 목록
     */
    List<EntityRelationEntity> findByFromEntityTypeAndFromEntityIdAndRelationTypeAndToEntityType(
            String fromEntityType,
            String fromEntityId,
            String relationType,
            String toEntityType
    );

    /**
     * 특정 관계 존재 여부 확인 (파생 필드 계산용)
     *
     * 예: "EMPLOYEE가 계좌 220-333-444555를 OWNS하는가?"
     *
     * @param fromEntityType From 엔티티 타입 (EMPLOYEE, CORPORATE 등)
     * @param relationType 관계 타입 (OWNS)
     * @param toEntityType To 엔티티 타입 (ACCOUNT)
     * @param toEntityId To 엔티티 ID (계좌번호)
     * @return 관계가 존재하면 true
     */
    boolean existsByFromEntityTypeAndRelationTypeAndToEntityTypeAndToEntityId(
            String fromEntityType,
            String relationType,
            String toEntityType,
            String toEntityId
    );
}
