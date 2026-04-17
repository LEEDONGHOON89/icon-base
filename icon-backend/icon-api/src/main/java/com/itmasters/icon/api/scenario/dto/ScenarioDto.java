package com.itmasters.icon.api.scenario.dto;

import com.itmasters.icon.common.domain.scenario.ScenarioOperator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scenario 관련 DTO 그룹
 * Toss 스타일의 Inner Class로 관련 DTO들을 관리
 */
public class ScenarioDto {
    
    /**
     * 시나리오 생성 요청 (Web → Service)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "시나리오명은 필수입니다")
        private String scenarioName;

        private String description;

        private String entityFilterJson;

        private String riskLevelId;

        private String detectionAreaId;

        private String primaryEntityType;

        // 엔진 설정 필드
        private Integer dedupMinutes;       // 중복 제거 창 (분)

        @Valid
        private List<ScenarioRuleRequest> rules;

        /**
         * Request → Command 변환
         */
        public CreateCommand toCommand() {
            List<CreateCommand.ScenarioRuleCommand> ruleCommands = null;
            if (this.rules != null) {
                ruleCommands = this.rules.stream()
                        .map(rule -> CreateCommand.ScenarioRuleCommand.builder()
                                .ruleId(rule.getRuleId())
                                .orderNo(rule.getOrderNo())
                                .operator(rule.getOperator())
                                .build())
                        .collect(Collectors.toList());
            }
            
            return CreateCommand.builder()
                    .scenarioName(this.scenarioName)
                    .description(this.description)
                    .entityFilterJson(this.entityFilterJson)
                    .riskLevelId(this.riskLevelId)
                    .detectionAreaId(this.detectionAreaId)
                    .primaryEntityType(this.primaryEntityType)
                    .dedupMinutes(this.dedupMinutes)
                    .rules(ruleCommands)
                    .build();
        }
    }

    /**
     * 시나리오 수정 요청 (Web → Service)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "시나리오명은 필수입니다")
        private String scenarioName;

        private String description;

        private String entityFilterJson;

        private String riskLevelId;

        private String detectionAreaId;

        private String primaryEntityType;

        private Boolean isActive;

        // 엔진 설정 필드
        private Integer dedupMinutes;       // 중복 제거 창 (분)

        @Valid
        private List<ScenarioRuleRequest> rules;

        /**
         * Request → Command 변환
         */
        public UpdateCommand toCommand(String scenarioId) {
            List<UpdateCommand.ScenarioRuleCommand> ruleCommands = null;
            if (this.rules != null) {
                ruleCommands = this.rules.stream()
                        .map(rule -> UpdateCommand.ScenarioRuleCommand.builder()
                                .ruleId(rule.getRuleId())
                                .orderNo(rule.getOrderNo())
                                .operator(rule.getOperator())
                                .build())
                        .collect(Collectors.toList());
            }

            return UpdateCommand.builder()
                    .scenarioId(scenarioId)
                    .scenarioName(this.scenarioName)
                    .description(this.description)
                    .entityFilterJson(this.entityFilterJson)
                    .riskLevelId(this.riskLevelId)
                    .detectionAreaId(this.detectionAreaId)
                    .primaryEntityType(this.primaryEntityType)
                    .isActive(this.isActive)
                    .dedupMinutes(this.dedupMinutes)
                    .rules(ruleCommands)
                    .build();
        }
    }

    /**
     * 시나리오-규칙 매핑 요청 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioRuleRequest {
        @NotNull(message = "규칙 ID는 필수입니다")
        private String ruleId;
        
        private Integer orderNo;
        
        private ScenarioOperator operator = ScenarioOperator.AND;
    }
    
    /**
     * 시나리오 생성 커맨드 (Service Layer)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CreateCommand {
        private String scenarioName;
        private String description;
        private String entityFilterJson;
        private String riskLevelId;
        private String detectionAreaId;
        private String primaryEntityType;
        // 엔진 설정 필드
        private Integer dedupMinutes;
        private List<ScenarioRuleCommand> rules;

        /**
         * 시나리오-규칙 매핑 Command
         */
        @Getter
        @Builder
        @AllArgsConstructor
        public static class ScenarioRuleCommand {
            private String ruleId;
            private Integer orderNo;
            private ScenarioOperator operator;
        }
    }
    
    /**
     * 시나리오 수정 커맨드 (Service Layer)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class UpdateCommand {
        private String scenarioId;
        private String scenarioName;
        private String description;
        private String entityFilterJson;
        private String riskLevelId;
        private String detectionAreaId;
        private String primaryEntityType;
        private Boolean isActive;
        // 엔진 설정 필드
        private Integer dedupMinutes;
        private List<ScenarioRuleCommand> rules;

        /**
         * 시나리오-규칙 매핑 Command
         */
        @Getter
        @Builder
        @AllArgsConstructor
        public static class ScenarioRuleCommand {
            private String ruleId;
            private Integer orderNo;
            private ScenarioOperator operator;
        }
    }
    
    /**
     * 규칙 추가 요청 (Web → Service)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddRuleRequest {
        @NotNull(message = "규칙 ID는 필수입니다")
        private String ruleId;
        
        private Integer orderNo;
        
        private ScenarioOperator operator = ScenarioOperator.AND;
    }
    
    /**
     * 시나리오 정보 (Service → Web)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Info {
        private String scenarioId;
        private String scenarioName;
        private String description;
        private String riskLevelId;
        private String riskLevelName;
        private String detectionAreaId;
        private String detectionAreaName;
        private String primaryEntityType;
        private Boolean isActive;
        // 엔진 설정 필드
        private Integer dedupMinutes;
        private List<ScenarioRuleInfo> rules;

        /**
         * 시나리오-규칙 매핑 정보
         */
        @Getter
        @Builder
        @AllArgsConstructor
        public static class ScenarioRuleInfo {
            private String ruleId;
            private String ruleName;
            private Integer orderNo;
            private ScenarioOperator operator;
        }
        
        // Note: Domain → DTO 변환은 ScenarioServiceImpl.getScenarioWithRules()에서 처리
        // 이 메서드는 사용하지 않고, Service 레이어에서 직접 Builder로 생성
    }
    
    /**
     * 시나리오 응답 (Web Layer)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private String scenarioId;
        private String scenarioName;
        private String description;
        private String entityFilterJson;
        private String riskLevelId;
        private String riskLevelName;
        private String detectionAreaId;
        private String detectionAreaName;
        private String primaryEntityType;
        private LocalDateTime regDt;
        private Boolean isActive;
        // 엔진 설정 필드
        private Integer dedupMinutes;
        private List<ScenarioRuleResponse> rules;

        /**
         * 시나리오-규칙 응답 DTO
         */
        @Getter
        @Builder
        @AllArgsConstructor
        public static class ScenarioRuleResponse {
            private String ruleId;
            private String ruleName;
            private Integer orderNo;
            private ScenarioOperator operator;

            // 집계 상세 정보
            private java.math.BigDecimal thresholdCount;
            private java.math.BigDecimal thresholdAmount;
            private String aggregateOperator;  // SUM_WITHIN, COUNT_WITHIN, etc.
        }
    }

    // ========== 세분화된 수정 API용 DTO ==========

    /**
     * 기본정보 수정 요청
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateBasicInfoRequest {
        @NotBlank(message = "시나리오명은 필수입니다")
        private String scenarioName;

        private String description;

        private String riskLevelId;

        private String detectionAreaId;

        private String primaryEntityType;

        private Boolean isActive;

        // 엔진 설정 필드
        private Integer dedupMinutes;

        public UpdateCommand toCommand(String scenarioId) {
            return UpdateCommand.builder()
                    .scenarioId(scenarioId)
                    .scenarioName(this.scenarioName)
                    .description(this.description)
                    .riskLevelId(this.riskLevelId)
                    .detectionAreaId(this.detectionAreaId)
                    .primaryEntityType(this.primaryEntityType)
                    .isActive(this.isActive)
                    .dedupMinutes(this.dedupMinutes)
                    .build();
        }
    }

    /**
     * 엔티티 필터 수정 요청
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateEntityFilterRequest {
        private String entityFilterJson;

        public UpdateCommand toCommand(String scenarioId) {
            return UpdateCommand.builder()
                    .scenarioId(scenarioId)
                    .entityFilterJson(this.entityFilterJson)
                    .build();
        }
    }

    /**
     * 집계구성 수정 요청
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRulesRequest {
        @Valid
        @NotNull(message = "집계 목록은 필수입니다")
        private List<ScenarioRuleRequest> rules;

        public UpdateCommand toCommand(String scenarioId) {
            List<UpdateCommand.ScenarioRuleCommand> ruleCommands = this.rules.stream()
                    .map(rule -> UpdateCommand.ScenarioRuleCommand.builder()
                            .ruleId(rule.getRuleId())
                            .orderNo(rule.getOrderNo())
                            .operator(rule.getOperator())
                            .build())
                    .collect(Collectors.toList());

            return UpdateCommand.builder()
                    .scenarioId(scenarioId)
                    .rules(ruleCommands)
                    .build();
        }
    }

    /**
     * 시나리오 시각화 응답 - Rule + Sensor 정보 포함
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisualizationResponse {
        private String scenarioId;
        private String scenarioName;
        private List<RuleWithSensor> rules;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RuleWithSensor {
            private String ruleId;
            private String ruleName;
            private String operator;
            private Integer windowMinutes;  // Rule의 집계 윈도우 (분)
            private Integer orderNo;
            private ScenarioOperator scenarioOperator;

            // Sensor 정보
            private String predicateSensorId;
            private String predicateSensorName;
        }
    }
}
