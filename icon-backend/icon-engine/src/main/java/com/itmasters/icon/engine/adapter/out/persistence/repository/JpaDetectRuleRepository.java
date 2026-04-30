package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA 인터페이스
 * QueryDSL 기반 복잡한 쿼리는 DetectRuleRepositoryCustom에서 구현
 */
public interface JpaDetectRuleRepository extends JpaRepository<DetectRuleEntity, Long>{
    // save 메소드는 JpaRepository에 이미 정의되어 있으므로 추가할 필요 없음

    /**
     * rule_id로 조회
     */
    List<DetectRuleEntity> findByRuleId(String ruleId);

    /**
     * mapped_storage_id로 카운트
     */
    long countByMappedStorageId(long mappedStorageId);

    /**
     * 최근 윈도우에서 group_key와 rule_id로 최신 히트 1건 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - DetectRuleRepositoryCustom 인터페이스에서 선언, DetectRuleRepositoryImpl에서 구현

    /**
     * exec_ds_mp_id 기준으로 탐지된 group_key 목록 조회 (중복 제거) - QueryDSL로 구현
     */
    // @Query 제거됨 - DetectRuleRepositoryCustom 인터페이스에서 선언, DetectRuleRepositoryImpl에서 구현

    /**
     * execDsMpId + groupKey 기준 최신 detected_at 반환 - QueryDSL로 구현
     */
    // @Query 제거됨 - DetectRuleRepositoryCustom 인터페이스에서 선언, DetectRuleRepositoryImpl에서 구현

    boolean existsByGroupKeyAndRuleIdAndDetectedDt(String groupKey, String ruleId, LocalDateTime detectedAt);

    boolean existsByMappedStorageIdAndRuleId(Long mappedStorageId, String ruleId);

    /**
     * 시간 기반 dedup 체크: dedup_minutes 내에 같은 (group_key, rule_id)로 탐지된 적 있는지 확인
     */
    boolean existsByGroupKeyAndRuleIdAndDetectedDtAfter(String groupKey, String ruleId, LocalDateTime afterTime);

    /**
     * 트랜잭션 ID로 조회
     */
    List<DetectRuleEntity> findByTransactionId(String transactionId);
}
