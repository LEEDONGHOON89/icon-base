package com.itmasters.icon.api.dataprofile.application.port.out;

import com.itmasters.icon.api.dataprofile.adapter.out.persistence.entity.ProfileEntity;

import java.util.List;
import java.util.Optional;

/**
 * 데이터 프로파일 Repository 포트
 */
public interface DataProfileRepository {
    
    /**
     * 프로파일 저장
     */
    ProfileEntity save(ProfileEntity profile);
    
    /**
     * 프로파일 ID로 조회
     */
    Optional<ProfileEntity> findById(String profileId);
    
    /**
     * 데이터소스별 프로파일 목록 조회 (활성화된 것만)
     */
    List<ProfileEntity> findByDataSourceId(String dataSourceId);
    
    /**
     * 데이터소스별 프로파일 목록 조회 (전체)
     */
    List<ProfileEntity> findAllByDataSourceId(String dataSourceId);
    
    /**
     * DEFAULT 프로파일 조회
     */
    Optional<ProfileEntity> findDefaultProfileByDataSourceId(String dataSourceId);
    
    /**
     * 데이터소스ID와 프로파일명으로 조회
     */
    List<ProfileEntity> findByDataSourceIdAndProfileName(String dataSourceId, String profileName);
    
    /**
     * 프로파일명 중복 체크
     */
    boolean existsByDataSourceIdAndProfileName(String dataSourceId, String profileName);
    
    /**
     * 프로파일명 중복 체크 (특정 프로파일 제외)
     */
    boolean existsByDataSourceIdAndProfileNameExcluding(String dataSourceId, String profileName, String excludeProfileId);
    
    /**
     * 프로파일 삭제
     */
    void delete(String profileId);
    
    /**
     * 프로파일 존재 여부 확인
     */
    boolean existsById(String profileId);
}