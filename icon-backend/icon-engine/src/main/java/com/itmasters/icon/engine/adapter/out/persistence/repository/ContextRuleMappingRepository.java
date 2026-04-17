package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.ContextRuleMappingEntity;

import java.util.List;
import java.util.Optional;

/**
 * 컨텍스트-룰 매핑 저장소 인터페이스
 */
public interface ContextRuleMappingRepository {
    
    /**
     * 매핑 저장
     */
    ContextRuleMappingEntity save(ContextRuleMappingEntity entity);
    
    /**
     * 매핑 ID로 조회
     */
    Optional<ContextRuleMappingEntity> findById(Long contextRuleMappingId);
    
    /**
     * 컨텍스트별 매핑 목록 조회
     */
    List<ContextRuleMappingEntity> findByDetectionContextId(Long detectionContextId);
    
    /**
     * 룰별 매핑 목록 조회
     */
    List<ContextRuleMappingEntity> findByRuleId(String ruleId);
    
    /**
     * 컨텍스트와 룰로 매핑 조회
     */
    Optional<ContextRuleMappingEntity> findByDetectionContextIdAndRuleId(Long detectionContextId, String ruleId);
    
    /**
     * 활성 매핑 조회
     */
    List<ContextRuleMappingEntity> findActiveByDetectionContextId(Long detectionContextId);
    
    /**
     * 배치 저장
     */
    List<ContextRuleMappingEntity> saveAll(List<ContextRuleMappingEntity> entities);
    
    /**
     * 매핑 삭제
     */
    void deleteById(Long contextRuleMappingId);
    
    /**
     * 컨텍스트에 속한 매핑 모두 삭제
     */
    int deleteByDetectionContextId(Long detectionContextId);
}