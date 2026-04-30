package com.itmasters.icon.api.dataprofile.application.service;

import com.itmasters.icon.api.dataprofile.adapter.in.web.dto.DataProfileDto;
import com.itmasters.icon.api.dataprofile.application.port.in.DataProfileUseCase;
import com.itmasters.icon.api.dataprofile.application.port.out.DataProfileRepository;
import com.itmasters.icon.api.dataprofile.adapter.out.persistence.entity.ProfileEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DataSourceEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.DataSourceJpaRepository;
import com.itmasters.icon.common.domain.EntityType;
import com.itmasters.icon.common.domain.IdGenerator;
import com.itmasters.icon.common.domain.type.ProfilePurpose;
import com.itmasters.icon.common.domain.type.GroupKeyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 데이터 프로파일 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DataProfileService implements DataProfileUseCase {

    private final DataSourceJpaRepository dataSourceRepository;
    private final DataProfileRepository profileRepository;
    private final IdGenerator idGenerator;
    
    @Override
    public DataProfileDto.Response createProfile(String dataSourceId, String profileName, 
                                     ProfilePurpose profilePurpose, 
                                     String description, int displayOrder) {
        
        // 프로파일명 중복 체크
        if (profileRepository.existsByDataSourceIdAndProfileName(dataSourceId, profileName)) {
            throw new IllegalArgumentException("이미 존재하는 프로파일명입니다: " + profileName);
        }

        DataSourceEntity dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("데이터소스를 찾을 수 없습니다: " + dataSourceId));

        // 프로파일 생성
        ProfileEntity profile = ProfileEntity.builder()
                .profileId(idGenerator.generateId(EntityType.PROFILE_SCHEMA))
                .dataSource(dataSource)
                .profileName(profileName)
                .profilePurpose(profilePurpose)
                .description(description)
                .displayOrder(displayOrder)
                .isActive(true)
                .schemaCount(0)
                .ruleCount(0)
                .build();
        
        ProfileEntity savedProfile = profileRepository.save(profile);
        return DataProfileDto.Response.from(savedProfile);
    }
    
    /**
     * detect_key를 포함한 프로파일 생성
     */
    public DataProfileDto.Response createProfile(DataProfileDto.CreateRequest request) {
        
        // 프로파일명 중복 체크
        if (profileRepository.existsByDataSourceIdAndProfileName(request.getDataSourceId(), request.getProfileName())) {
            throw new IllegalArgumentException("이미 존재하는 프로파일명입니다: " + request.getProfileName());
        }

        DataSourceEntity dataSource = dataSourceRepository.findById(request.getDataSourceId())
                .orElseThrow(() -> new IllegalArgumentException("데이터소스를 찾을 수 없습니다: " + request.getDataSourceId()));

        // detect_key 검증 및 타입 자동 추론
        GroupKeyType groupKeyType = null;
        if (request.getDetectKey() != null && !request.getDetectKey().isEmpty()) {
            groupKeyType = request.getGroupKeyTypeEnum();
            if (groupKeyType == null) {
                groupKeyType = GroupKeyType.inferType(request.getDetectKey());
            }
            log.info("프로파일 생성 - detectKey: {}, type: {}", request.getDetectKey(), groupKeyType);
            
            // detect_key 유효성 검증
            validateDetectKey(request.getDetectKey(), groupKeyType);
        }

        // 프로파일 생성
        ProfileEntity profile = ProfileEntity.builder()
                .profileId(idGenerator.generateId(EntityType.PROFILE_SCHEMA))
                .dataSource(dataSource)
                .profileName(request.getProfileName())
                .profilePurpose(request.getProfilePurposeEnum())
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrderValue())
                .groupKey(request.getDetectKey())
                .groupKeyType(groupKeyType)
                .isActive(true)
                .schemaCount(0)
                .ruleCount(0)
                .build();
        
        ProfileEntity savedProfile = profileRepository.save(profile);
        log.info("프로파일 생성 완료 - profileId: {}, detectKey: {}", savedProfile.getProfileId(), savedProfile.getGroupKey());
        
        return DataProfileDto.Response.from(savedProfile);
    }
    
    @Override
    public DataProfileDto.Response updateProfile(String profileId, String profileName, 
                                     ProfilePurpose profilePurpose, 
                                     String description, int displayOrder) {
        
        // 기존 프로파일 조회
        ProfileEntity existingProfile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("프로파일을 찾을 수 없습니다: " + profileId));
        
        // 프로파일명 중복 체크 (자신 제외)
        if (!existingProfile.getProfileName().equals(profileName) &&
            profileRepository.existsByDataSourceIdAndProfileNameExcluding(
                    existingProfile.getDataSource().getDataSourceId(), profileName, profileId)) {
            throw new IllegalArgumentException("이미 존재하는 프로파일명입니다: " + profileName);
        }
        
        // 프로파일 수정
        existingProfile.setProfileName(profileName);
        existingProfile.setProfilePurpose(profilePurpose);
        existingProfile.setDescription(description);
        existingProfile.setDisplayOrder(displayOrder);

        ProfileEntity savedProfile = profileRepository.save(existingProfile);
        return DataProfileDto.Response.from(savedProfile);
    }
    
    /**
     * detect_key를 포함한 프로파일 수정
     */
    public DataProfileDto.Response updateProfile(String profileId, DataProfileDto.UpdateRequest request) {
        
        // 기존 프로파일 조회
        ProfileEntity existingProfile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("프로파일을 찾을 수 없습니다: " + profileId));
        
        // 프로파일명 중복 체크 (자신 제외)
        if (request.getProfileName() != null && 
            !existingProfile.getProfileName().equals(request.getProfileName()) &&
            profileRepository.existsByDataSourceIdAndProfileNameExcluding(
                    existingProfile.getDataSource().getDataSourceId(), request.getProfileName(), profileId)) {
            throw new IllegalArgumentException("이미 존재하는 프로파일명입니다: " + request.getProfileName());
        }
        
        // detect_key 검증 및 타입 자동 추론
        if (request.getDetectKey() != null) {
            GroupKeyType groupKeyType = null;
            if (!request.getDetectKey().isEmpty()) {
                groupKeyType = request.getGroupKeyTypeEnum();
                if (groupKeyType == null) {
                    groupKeyType = GroupKeyType.inferType(request.getDetectKey());
                }
                log.info("프로파일 수정 - profileId: {}, detectKey: {}, type: {}", 
                        profileId, request.getDetectKey(), groupKeyType);
                
                // detect_key 유효성 검증
                validateDetectKey(request.getDetectKey(), groupKeyType);
            }
            existingProfile.setGroupKey(request.getDetectKey());
            existingProfile.setGroupKeyType(groupKeyType);
        }
        
        // 프로파일 수정
        if (request.getProfileName() != null) {
            existingProfile.setProfileName(request.getProfileName());
        }
        if (request.getProfilePurpose() != null) {
            existingProfile.setProfilePurpose(request.getProfilePurposeEnum());
        }
        if (request.getDescription() != null) {
            existingProfile.setDescription(request.getDescription());
        }
        if (request.getDisplayOrder() != null) {
            existingProfile.setDisplayOrder(request.getDisplayOrderValue());
        }

        // Entity Attributes 필드 저장
        if (request.getDestinationType() != null) {
            com.itmasters.icon.api.common.domain.type.DestinationType destType =
                    com.itmasters.icon.api.common.domain.type.DestinationType.valueOf(request.getDestinationType());
            existingProfile.setDestinationType(destType);

            // [2026-04-23] EVENT_STREAM destination 선택 시 timestampKey 필수 검증
            boolean needsTimestamp = destType == com.itmasters.icon.api.common.domain.type.DestinationType.EVENT_STREAM
                    || destType == com.itmasters.icon.api.common.domain.type.DestinationType.BOTH;
            if (needsTimestamp) {
                String tsKey = request.getTimestampKey();
                if (tsKey == null || tsKey.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "이벤트 스트림 저장 방식 선택 시 타임스탬프 필드는 필수입니다.");
                }
            }
        }
        if (request.getEntityType() != null) {
            existingProfile.setEntityType(
                com.itmasters.icon.common.domain.DomainEntityType.valueOf(request.getEntityType())
            );
        }
        if (request.getEntityIdField() != null) {
            existingProfile.setEntityIdField(request.getEntityIdField());
        }
        if (request.getStoreFields() != null) {
            existingProfile.setStoreFields(request.getStoreFields());
        }
        // [2026-04-23] timestampKey 저장
        if (request.getTimestampKey() != null) {
            existingProfile.setTimestampKey(request.getTimestampKey().trim().isEmpty()
                    ? null : request.getTimestampKey().trim());
        }

        ProfileEntity savedProfile = profileRepository.save(existingProfile);
        log.info("프로파일 수정 완료 - profileId: {}, detectKey: {}, timestampKey: {}",
                savedProfile.getProfileId(), savedProfile.getGroupKey(), savedProfile.getTimestampKey());
        
        return DataProfileDto.Response.from(savedProfile);
    }
    
    @Override
    @Transactional(readOnly = true)
    public DataProfileDto.Response getProfile(String profileId) {
        ProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("프로파일을 찾을 수 없습니다: " + profileId));
        return DataProfileDto.Response.from(profile);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<DataProfileDto.Response> getProfilesByDataSource(String dataSourceId) {
        List<ProfileEntity> profiles = profileRepository.findByDataSourceId(dataSourceId);
        return profiles.stream()
                .map(DataProfileDto.Response::from)
                .collect(Collectors.toList());
    }
    
    @Override
    public DataProfileDto.Response toggleProfile(String profileId) {
        ProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("프로파일을 찾을 수 없습니다: " + profileId));
        
        profile.setIsActive(!profile.getIsActive());
        ProfileEntity savedProfile = profileRepository.save(profile);
        return DataProfileDto.Response.from(savedProfile);
    }
    
    @Override
    public void deleteProfile(String profileId) {
        if (!profileRepository.existsById(profileId)) {
            throw new IllegalArgumentException("프로파일을 찾을 수 없습니다: " + profileId);
        }
        
        // TODO: 프로파일에 연결된 스키마가 있는지 확인하고 적절히 처리
        // 현재는 단순 삭제 (CASCADE로 스키마도 함께 삭제됨)
        
        profileRepository.delete(profileId);
    }
    
    /**
     * detect_key 유효성 검증
     */
    private void validateDetectKey(String detectKey, GroupKeyType groupKeyType) {
        if (detectKey == null || detectKey.trim().isEmpty()) {
            return;
        }
        
        // detect_key 길이 검증 (최대 500자)
        if (detectKey.length() > 500) {
            throw new IllegalArgumentException("detect_key는 500자를 초과할 수 없습니다.");
        }
        
        // 타입별 검증
        switch (groupKeyType) {
            case SINGLE:
                // 단일 키는 쉼표를 포함하면 안됨
                if (detectKey.contains(",")) {
                    throw new IllegalArgumentException("SINGLE 타입의 detect_key는 쉼표를 포함할 수 없습니다.");
                }
                // [2026-04-23] 표준 필드명 형식 검증: 대소문자+숫자+언더스코어 허용 (CUS_ID 등 대문자 표준필드 지원)
                if (!detectKey.matches("^[a-zA-Z0-9_]+$")) {
                    throw new IllegalArgumentException("SINGLE 타입의 detect_key는 영문자, 숫자, 언더스코어만 포함할 수 있습니다.");
                }
                break;

            case COMPOSITE:
                // 복합 키는 쉼표로 구분된 여러 필드
                String[] fields = detectKey.split(",");
                if (fields.length < 2) {
                    throw new IllegalArgumentException("COMPOSITE 타입의 detect_key는 최소 2개 이상의 필드를 포함해야 합니다.");
                }
                // [2026-04-23] 각 필드명 검증: 대소문자+숫자+언더스코어 허용 (CUS_ID 등 대문자 표준필드 지원)
                for (String field : fields) {
                    String trimmedField = field.trim();
                    if (!trimmedField.matches("^[a-zA-Z0-9_]+$")) {
                        throw new IllegalArgumentException("COMPOSITE 타입의 각 필드는 영문자, 숫자, 언더스코어만 포함할 수 있습니다: " + trimmedField);
                    }
                }
                break;
                
            case CUSTOM:
                // 커스텀 키는 특별한 제약 없음 (길이 제약만 적용)
                break;
                
            default:
                throw new IllegalArgumentException("지원하지 않는 detect_key 타입입니다: " + groupKeyType);
        }
        
        log.debug("detect_key 검증 완료 - key: {}, type: {}", detectKey, groupKeyType);
    }
}
