package com.itmasters.icon.api.rule.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.api.rule.adapter.persistence.entity.SensorEntity;
import com.itmasters.icon.api.common.domain.RuleCategory;
import com.itmasters.icon.common.domain.rule.condition.RuleCondition;
import com.itmasters.icon.common.domain.RuleDomain;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Sensor 관련 DTO 그룹
 * Web Layer와 Service Layer에서 사용하는 모든 DTO를 Inner Class로 관리
 */
public class SensorDto {

    /**
     * 센서 생성 요청 (Web → Service)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "센서 ID는 필수입니다")
        @Size(max = 100, message = "센서 ID는 100자를 초과할 수 없습니다")
        private String sensorId; // 사용자 정의 센서 ID (예: S_LOGIN_FAIL)

        @NotBlank(message = "센서 이름은 필수입니다")
        @Size(max = 255, message = "센서 이름은 255자를 초과할 수 없습니다")
        private String sensorName;

        private RuleCategory category; // v4: optional, kept for legacy compatibility
        
        
        // 새로운 2단계 탭 구조 지원
        private RuleDomain domain; // v4: optional; can be derived from field or where_json
        
        private RuleOperator operator; // optional when where_json provided

        private String fieldName; // optional when where_json provided
        private Object value;

        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
        private String description;

        // New schema (optional): anchor predicate + where_json conditions
        private String anchor;    // JSON string: {"fieldName","operator","value"}
        private String whereJson; // JSON string: predicate or [predicate,...]

        /**
         * Request → Command 변환
         */
        public CreateCommand toCommand() {
            return CreateCommand.builder()
                    .sensorId(this.sensorId)
                    .sensorName(this.sensorName)
                    .category(this.category)
                    .domain(this.domain)
                    .operator(this.operator)
                    .fieldName(fieldName)
                    .value(value)
                    .description(this.description)
                    .anchor(this.anchor)
                    .whereJson(this.whereJson)
                    .build();
        }
    }

    /**
     * 센서 수정 요청 (Web → Service)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @Size(max = 255, message = "센서 이름은 255자를 초과할 수 없습니다")
        private String sensorName;

        private RuleCategory category;
        
        
        // 새로운 2단계 탭 구조 지원
        private RuleDomain domain;
        
        private RuleOperator operator;

        private String fieldName;
        private Object value;

        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
        private String description;

        private Boolean isActive;

        // New schema (optional)
        private String anchor;
        private String whereJson;

        /**
         * Request → Command 변환
         */
        public UpdateCommand toCommand(String ruleId) {
            return UpdateCommand.builder()
                    .ruleId(ruleId)
                    .sensorName(this.sensorName)
                    .category(this.category)
                    .domain(this.domain)
                    .operator(this.operator)
                    .fieldName(this.fieldName)
                    .value(value)
                    .description(this.description)
                    .isActive(this.isActive)
                    .anchor(this.anchor)
                    .whereJson(this.whereJson)
                    .build();
        }
    }

    /**
     * 규칙 생성 커맨드 (Service Layer)
     */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class CreateCommand {
        private String sensorId; // 사용자 정의 센서 ID
        private String sensorName;
        private RuleCategory category;
        private RuleDomain domain;
        private RuleOperator operator;
        private String fieldName;
        private Object value;
        private String description;
        private String anchor;
        private String whereJson;
    }

    /**
     * 규칙 수정 커맨드 (Service Layer)
     */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class UpdateCommand {
        private String ruleId;
        private String sensorName;
        private RuleCategory category;
        private RuleDomain domain;
        private RuleOperator operator;
        private String fieldName;
        private Object value;
        private String description;
        private Boolean isActive;
        private String anchor;
        private String whereJson;
    }

    /**
     * 센서 정보 (Service → Web)
     * Service Layer에서 반환하는 기본 정보
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Info {
        private String sensorId;
        private String sensorName;
        private RuleCategory category;
        private RuleDomain domain;
        private RuleOperator operator;
        private RuleCondition condition;
        private String description;
        private Boolean isActive;
        private String whereJson;

        // Entity → DTO 변환
        public static Info from(SensorEntity entity, ObjectMapper objectMapper) {
            RuleDomain derivedDomain = null;
            RuleOperator derivedOperator = null;
            // 1) where_json에서 파싱 시도
            try {
                String w = entity.getWhereJson();
                if (w != null && !w.isBlank()) {
                    com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(w);
                    if (node.isArray() && node.size() > 0) node = node.get(0);
                    if (node.has("operator")) {
                        String op = node.get("operator").asText(null);
                        if (op != null) derivedOperator = RuleOperator.valueOf(op);
                    }
                    String field = null;
                    if (node.has("fieldName")) {
                        field = node.get("fieldName").asText(null);
                    } else if (node.has("field")) {
                        field = node.get("field").asText(null);
                    }
                    derivedDomain = deriveDomainFromField(field);
                }
            } catch (Exception ignore) { }
            // 2) 레거시 conditionData에서 보강
            if (derivedOperator == null) {
                com.itmasters.icon.common.domain.rule.condition.RuleCondition cond = entity.getConditionAsObject(objectMapper);
                if (cond != null) {
                    try { derivedOperator = RuleOperator.valueOf(cond.getOperator().name()); } catch (Exception ignore) {}
                    if (derivedDomain == null) derivedDomain = deriveDomainFromField(cond.getFieldName());
                }
            }

            return Info.builder()
                    .sensorId(entity.getSensorId())
                    .sensorName(entity.getSensorName())
                    .category(entity.getCategory())
                    .domain(derivedDomain)
                    .operator(derivedOperator)
                    .condition(entity.getConditionAsObject(objectMapper))
                    .description(entity.getDescription())
                    .isActive(entity.getIsActive())
                    .whereJson(entity.getWhereJson())
                    .build();
        }

        private static RuleDomain deriveDomainFromField(String fieldName) {
            if (fieldName == null) return null;
            try {
                com.itmasters.icon.common.domain.rule.RuleField rf = com.itmasters.icon.common.domain.rule.RuleField.valueOf(fieldName.toUpperCase());
                com.itmasters.icon.common.domain.rule.FieldCategory c = rf.getCategory();
                return switch (c) {
                    case TRANSACTION, OPEN_BANKING, LOAN, BLACKLIST -> RuleDomain.FINANCIAL_TRANSACTION;
                    case ACCESS, AUTH, SECURITY, ACTIVITY -> RuleDomain.LOGIN;
                    case DEVICE -> RuleDomain.DEVICE_SECURITY;
                    case ACCOUNT -> RuleDomain.ACCOUNT;
                    case CUSTOMER -> RuleDomain.CUSTOMER;
                    case ATM -> RuleDomain.ATM;
                    default -> null;
                };
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    /**
     * 규칙 응답 (Web Layer)
     * API 응답으로 사용
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private String sensorId;
        private String sensorName;
        private RuleCategory category;
        private String categoryLabel;
        private RuleDomain domain;
        private String domainLabel;
        private RuleOperator operator;
        private String operatorLabel;
        private Object condition;  // JSON으로 직렬화된 조건
        private String description;
        private Integer version;
        private Boolean isActive;
        private String whereJson;

        // Info → Response 변환
        public static Response from(Info info) {
            RuleCategory cat = info.getCategory() != null ? info.getCategory() : com.itmasters.icon.api.common.domain.RuleCategory.CUSTOM;
            return Response.builder()
                    .sensorId(info.getSensorId())
                    .sensorName(info.getSensorName())
                    .category(cat)
                    .categoryLabel(cat != null ? cat.getLabel() : null)
                    .domain(info.getDomain())
                    .domainLabel(info.getDomain() != null ? info.getDomain().getLabel() : null)
                    .operator(info.getOperator())
                    .operatorLabel(info.getOperator() != null ? info.getOperator().getLabel() : null)
                    .condition(info.getCondition())  // 실제로는 JSON 변환 필요
                    .description(info.getDescription())
                    .isActive(info.getIsActive())
                    .whereJson(info.getWhereJson())
                    .build();
        }
    }

    /**
     * 규칙 간단 정보
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Simple {
        private String sensorId;
        private String sensorName;
        private RuleCategory category;
        private RuleDomain domain;
        private RuleOperator operator;
        private Boolean isActive;

        // Entity → DTO 변환
        public static Simple from(SensorEntity entity) {
            RuleCategory cat = entity.getCategory() != null ? entity.getCategory() : com.itmasters.icon.api.common.domain.RuleCategory.CUSTOM;
            return Simple.builder()
                    .sensorId(entity.getSensorId())
                    .sensorName(entity.getSensorName())
                    .category(cat)
                    .domain(entity.getDomain())
                    .operator(entity.getOperator())
                    .isActive(entity.getIsActive())
                    .build();
        }
    }
    
    /**
     * 규칙 검색 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class SearchRequest {
        private String sensorName;
        private RuleCategory category;
        private RuleDomain domain;
        private RuleOperator operator;
        private Boolean isActive;
    }
}
