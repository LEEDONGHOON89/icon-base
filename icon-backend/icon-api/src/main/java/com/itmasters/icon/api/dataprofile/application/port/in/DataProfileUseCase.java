package com.itmasters.icon.api.dataprofile.application.port.in;

import com.itmasters.icon.common.domain.type.ProfilePurpose;
import com.itmasters.icon.api.dataprofile.adapter.in.web.dto.DataProfileDto;

import java.util.List;

/**
 * 데이터 프로파일 Use Case
 */
public interface DataProfileUseCase {
    
    /**
     * 프로파일 생성
     */
    DataProfileDto.Response createProfile(String dataSourceId, String profileName, 
                               ProfilePurpose profilePurpose, 
                               String description, int displayOrder);
    
    /**
     * 프로파일 수정
     */
    DataProfileDto.Response updateProfile(String profileId, String profileName, 
                               ProfilePurpose profilePurpose, 
                               String description, int displayOrder);
    
    /**
     * 프로파일 조회
     */
    DataProfileDto.Response getProfile(String profileId);
    
    /**
     * 데이터소스별 프로파일 목록 조회
     */
    List<DataProfileDto.Response> getProfilesByDataSource(String dataSourceId);
    
    /**
     * 프로파일 활성화/비활성화
     */
    DataProfileDto.Response toggleProfile(String profileId);
    
    /**
     * 프로파일 삭제
     */
    void deleteProfile(String profileId);
}