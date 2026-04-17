package com.itmasters.icon.api.dataprofile.adapter.out.persistence.repository;


import com.itmasters.icon.api.dataprofile.adapter.out.persistence.entity.ProfileEntity;

import java.util.List;
import java.util.Optional;

/**
 * 데이터 프로파일 Custom Repository 인터페이스
 * QueryDSL을 사용한 커스텀 쿼리 메서드 정의
 */
public interface DataProfileQueryDslRepository {
    
    /**
     * 데이터소스별 프로파일 목록 조회 (활성화된 것만)
     */
    List<ProfileEntity> findByDataSourceIdAndIsActiveTrue(String dataSourceId);
    
    /**
     * 데이터소스별 프로파일 목록 조회 (전체)
     */
    List<ProfileEntity> findByDataSourceId(String dataSourceId);
    
    /**
     * 데이터소스별 프로파일명 중복 체크
     */
    Optional<ProfileEntity> findByDataSourceIdAndProfileName(String dataSourceId, String profileName);
    
    /**
     * 데이터소스별 프로파일명 중복 체크 (특정 프로파일 제외)
     */
    Optional<ProfileEntity> findByDataSourceIdAndProfileNameAndProfileIdNot(
            String dataSourceId, String profileName, String excludeProfileId);
    
    /**
     * 프로파일 ID로 조회 (데이터소스 정보 포함)
     */
    Optional<ProfileEntity> findByIdWithDataSource(String profileId);
}