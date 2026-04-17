package com.itmasters.icon.api.dataprofile.adapter.in.web.dto;

import com.itmasters.icon.common.domain.type.ProfilePurpose;
import com.itmasters.icon.common.domain.type.GroupKeyType;
import com.itmasters.icon.api.dataprofile.adapter.out.persistence.entity.ProfileEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 데이터 프로파일 DTO
 */
public class DataProfileDto {
    
    /**
     * API 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String profileId;
        private String dataSourceId;
        private String profileName;
        private String profilePurpose;
        private String profilePurposeDescription;
        private String description;
        private int displayOrder;
        private int schemaCount;
        private int ruleCount;
        private boolean isActive;
        private String detectKey;
        private String detectKeyType;
        // Entity Attributes 필드
        private String destinationType;
        private String entityType;
        private String entityIdField;
        private java.util.List<String> storeFields;

        public static Response from(ProfileEntity profile) {
            return Response.builder()
                    .profileId(profile.getProfileId())
                    .dataSourceId(profile.getDataSource().getDataSourceId())
                    .profileName(profile.getProfileName())
                    .profilePurpose(profile.getProfilePurpose() != null ? profile.getProfilePurpose().name() : null)
                    .profilePurposeDescription(profile.getProfilePurpose() != null ? profile.getProfilePurpose().getDescription() : null)
                    .description(profile.getDescription())
                    .displayOrder(profile.getDisplayOrder() != null ? profile.getDisplayOrder() : 0)
                    .schemaCount(profile.getSchemaCount() != null ? profile.getSchemaCount() : 0)
                    .ruleCount(profile.getRuleCount() != null ? profile.getRuleCount() : 0)
                    .isActive(profile.getIsActive() != null ? profile.getIsActive() : false)
                    .detectKey(profile.getGroupKey())
                    .detectKeyType(profile.getGroupKeyType() != null ? profile.getGroupKeyType().name() : null)
                    // Entity Attributes 필드
                    .destinationType(profile.getDestinationType() != null ? profile.getDestinationType().name() : null)
                    .entityType(profile.getEntityType() != null ? profile.getEntityType().name() : null)
                    .entityIdField(profile.getEntityIdField())
                    .storeFields(profile.getStoreFields())
                    .build();
        }
    }
    
    /**
     * 프로파일 생성 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String dataSourceId;
        private String profileName;
        private String profilePurpose; // ProfilePurpose enum name
        private String description;
        private Integer displayOrder;
        private String detectKey;
        private String detectKeyType;
        // Entity Attributes 필드
        private String destinationType;
        private String entityType;
        private String entityIdField;
        private java.util.List<String> storeFields;
        
        public ProfilePurpose getProfilePurposeEnum() {
            // profilePurpose가 없으면 null 반환 (선택사항)
            return profilePurpose != null ? ProfilePurpose.valueOf(profilePurpose) : null;
        }
        
        public int getDisplayOrderValue() {
            return displayOrder != null ? displayOrder : 0;
        }
        
        public GroupKeyType getGroupKeyTypeEnum() {
            if (detectKeyType == null || detectKeyType.isEmpty()) {
                // detectKey가 있으면 자동 유형 추론
                if (detectKey != null && !detectKey.isEmpty()) {
                    return GroupKeyType.inferType(detectKey);
                }
                return null;
            }
            return GroupKeyType.valueOf(detectKeyType);
        }
    }
    
    /**
     * 프로파일 수정 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String profileName;
        private String profilePurpose; // ProfilePurpose enum name
        private String description;
        private Integer displayOrder;
        private String detectKey;
        private String detectKeyType;
        // Entity Attributes 필드
        private String destinationType;
        private String entityType;
        private String entityIdField;
        private java.util.List<String> storeFields;
        
        public ProfilePurpose getProfilePurposeEnum() {
            // profilePurpose가 없으면 null 반환 (선택사항)
            return profilePurpose != null ? ProfilePurpose.valueOf(profilePurpose) : null;
        }
        
        public int getDisplayOrderValue() {
            return displayOrder != null ? displayOrder : 0;
        }
        
        public GroupKeyType getGroupKeyTypeEnum() {
            if (detectKeyType == null || detectKeyType.isEmpty()) {
                // detectKey가 있으면 자동 유형 추론
                if (detectKey != null && !detectKey.isEmpty()) {
                    return GroupKeyType.inferType(detectKey);
                }
                return null;
            }
            return GroupKeyType.valueOf(detectKeyType);
        }
    }
}