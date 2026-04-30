package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineScenarioEntity;

import java.util.List;

/**
 * 시나리오 Repository 인터페이스
 */
public interface EngineScenarioRepository {
    
    /**
     * 활성화된 모든 시나리오 조회
     * @return 활성 시나리오 목록
     */
    List<EngineScenarioEntity> findActiveScenarios();
    
    /**
     * 특정 프로파일과 연관된 활성 시나리오 조회
     * (프로파일의 룰들이 포함된 시나리오들을 찾음)
     * @param profileId 프로파일 ID
     * @return 활성 시나리오 목록
     */
    List<EngineScenarioEntity> findActiveScenariosByProfileId(String profileId);
    
    /**
     * 시나리오 ID로 단일 조회
     * @param scenarioId 시나리오 ID
     * @return 시나리오 엔티티
     */
    EngineScenarioEntity findByScenarioId(String scenarioId);
}