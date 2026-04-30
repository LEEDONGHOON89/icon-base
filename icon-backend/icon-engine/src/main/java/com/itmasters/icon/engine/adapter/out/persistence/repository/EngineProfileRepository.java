package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineProfileEntity;

import java.util.List;
import java.util.Optional;

/**
 * 엔진 프로파일 저장소 인터페이스
 * 데이터소스별 프로파일 조회 및 관리
 */
public interface EngineProfileRepository {
    
    /**
     * 데이터소스 ID로 활성화된 프로파일 목록 조회
     * 
     * @param dataSourceId 데이터소스 ID
     * @return 활성화된 프로파일 목록
     */
    List<EngineProfileEntity> findActiveProfilesByDataSourceId(String dataSourceId);
    
    /**
     * 프로파일 ID로 프로파일 조회
     * 
     * @param profileId 프로파일 ID
     * @return 프로파일 정보 (없으면 Optional.empty())
     */
    Optional<EngineProfileEntity> findById(String profileId);
}