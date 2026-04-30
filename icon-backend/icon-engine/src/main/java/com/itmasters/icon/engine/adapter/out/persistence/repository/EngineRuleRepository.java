package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineRuleEntity;

import java.util.List;

/**
 * 엔진 룰 저장소 인터페이스
 * 프로파일별 룰 조회 및 관리
 */
public interface EngineRuleRepository {
    
    /**
     * 프로파일 ID로 활성화된 룰 목록 조회
     * 
     * @param profileId 프로파일 ID
     * @return 활성화된 룰 목록
     */
    List<EngineRuleEntity> findActiveRulesByProfileId(String profileId);
    
    /**
     * 특정 룰 ID로 룰 조회
     * 
     * @param ruleId 룰 ID
     * @return 룰 정보
     */
    EngineRuleEntity findById(String ruleId);
    
    /**
     * 프로파일의 모든 룰 조회 (활성/비활성 포함)
     * 
     * @param profileId 프로파일 ID
     * @return 전체 룰 목록
     */
    List<EngineRuleEntity> findAllByProfileId(String profileId);

    /**
     * 시스템의 활성 룰 전체 조회 (프로파일 무관)
     */
    java.util.List<EngineRuleEntity> findAllActive();
}
