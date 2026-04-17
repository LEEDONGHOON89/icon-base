package com.itmasters.icon.api.datasource.dto;

import com.itmasters.icon.common.domain.type.DataSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DataSource 관련 DTO 그룹
 * Web Layer와 Service Layer에서 사용하는 모든 DTO를 Inner Class로 관리
 */
public class DataSourceDto {
    
    /**
     * 데이터 소스 생성 요청 (Web → Service)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "데이터 소스 이름은 필수입니다")
        @Size(max = 100, message = "데이터 소스 이름은 100자를 초과할 수 없습니다")
        private String name;
        
        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
        private String description;
        
        @NotNull(message = "데이터 소스 타입은 필수입니다")
        private DataSourceType sourceType;
        
        @Size(max = 100, message = "트랜잭션 ID 필드명은 100자를 초과할 수 없습니다")
        private String transactionIdField;
        
        /**
         * Request → Command 변환
         */
        public CreateCommand toCommand() {
            return CreateCommand.builder()
                    .name(this.name)
                    .description(this.description)
                    .sourceType(this.sourceType)
                    .transactionIdField(this.transactionIdField)
                    .build();
        }
    }
    
    /**
     * 데이터 소스 수정 요청 (Web → Service)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @Size(max = 100, message = "데이터 소스 이름은 100자를 초과할 수 없습니다")
        private String name;
        
        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
        private String description;
        
        private Boolean isActive;
        
        @Size(max = 100, message = "트랜잭션 ID 필드명은 100자를 초과할 수 없습니다")
        private String transactionIdField;
        
        /**
         * Request → Command 변환
         */
        public UpdateCommand toCommand(String dataSourceId) {
            return UpdateCommand.builder()
                    .dataSourceId(dataSourceId)
                    .name(this.name)
                    .description(this.description)
                    .isActive(this.isActive)
                    .transactionIdField(this.transactionIdField)
                    .build();
        }
    }
    
    /**
     * 데이터 소스 생성 커맨드 (Service Layer)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CreateCommand {
        private String name;
        private String description;
        private DataSourceType sourceType;
        private String transactionIdField;
    }
    
    /**
     * 데이터 소스 수정 커맨드 (Service Layer)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class UpdateCommand {
        private String dataSourceId;
        private String name;
        private String description;
        private Boolean isActive;
        private String transactionIdField;
    }
    
    /**
     * 데이터 소스 정보 (Service → Web)
     * Service Layer에서 반환하는 기본 정보
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Info {
        private String dataSourceId;
        private String name;
        private String description;
        private DataSourceType sourceType;
        private Boolean isActive;
        private String transactionIdField;
        private Long ruleCount;  // 매핑된 규칙 수
        
        // Entity 변환은 DataSourceEntity.toInfo() 메서드 사용
    }
    
    /**
     * 데이터 소스 응답 (Web Layer)
     * API 응답으로 사용
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private String dataSourceId;
        private String name;
        private String description;
        private DataSourceType sourceType;
        private Boolean isActive;
        private String transactionIdField;
        private Long ruleCount;
        
        // Info → Response 변환
        public static Response from(Info info) {
            return Response.builder()
                    .dataSourceId(info.getDataSourceId())
                    .name(info.getName())
                    .description(info.getDescription())
                    .sourceType(info.getSourceType())
                    .isActive(info.getIsActive())
                    .transactionIdField(info.getTransactionIdField())
                    .ruleCount(info.getRuleCount())
                    .build();
        }
    }
    
    /**
     * 데이터 소스 검색 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class SearchRequest {
        private String name;
        private DataSourceType sourceType;
        private Boolean isActive;
    }
}