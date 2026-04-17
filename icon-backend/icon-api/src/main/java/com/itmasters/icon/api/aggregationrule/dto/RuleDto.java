package com.itmasters.icon.api.aggregationrule.dto;

import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;
import com.itmasters.icon.common.domain.aggregate.AggregateOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * Rule 관련 DTO 그룹
 */
public class RuleDto {

    /**
     * 룰 생성 요청
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "룰 ID는 필수입니다")
        @Size(max = 50, message = "룰 ID는 50자를 초과할 수 없습니다")
        private String ruleId;

        @NotBlank(message = "룰 이름은 필수입니다")
        @Size(max = 255, message = "룰 이름은 255자를 초과할 수 없습니다")
        private String name;

        @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
        private String description;

        // WINDOW 모드에서는 필수, SINGLE_ROW에서는 선택사항
        private String operator;

        private String predicateSensorId;
        private String prevSensorId;
        private String nextSensorId;
        private String anchorSensorId;
        private Integer windowMinutes;
        private BigDecimal thresholdCount;
        private BigDecimal thresholdAmount;
        private Integer dedupMinutes;
        private String[] groupByFields;
        private String aggregationField;

        private String whereJson;
        private String evaluationMode;
    }

    /**
     * 룰 수정 요청
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @Size(max = 255, message = "룰 이름은 255자를 초과할 수 없습니다")
        private String name;

        @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
        private String description;

        private String operator;
        private String predicateSensorId;
        private String prevSensorId;
        private String nextSensorId;
        private String anchorSensorId;
        private Integer windowMinutes;
        private BigDecimal thresholdCount;
        private BigDecimal thresholdAmount;
        private Integer dedupMinutes;
        private Boolean isActive;
        private String[] groupByFields;
        private String aggregationField;
        private String whereJson;
        private String evaluationMode;
    }

    /**
     * 룰 응답
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private String ruleId;
        private String name;
        private String description;
        private String operator;
        private String predicateSensorId;
        private String prevSensorId;
        private String nextSensorId;
        private String anchorSensorId;
        private Integer windowMinutes;
        private BigDecimal thresholdCount;
        private BigDecimal thresholdAmount;
        private Integer dedupMinutes;
        private Boolean isActive;
        private String[] groupByFields;
        private String aggregationField;
        private String whereJson;
        private String evaluationMode;

        public static Response from(RuleEntity entity) {
            return Response.builder()
                    .ruleId(entity.getRuleId())
                    .name(entity.getName())
                    .description(entity.getDescription())
                    .operator(entity.getOperator() != null ? entity.getOperator().name() : null)
                    .predicateSensorId(entity.getPredicateSensorId())
                    .prevSensorId(entity.getPrevSensorId())
                    .nextSensorId(entity.getNextSensorId())
                    .anchorSensorId(entity.getAnchorSensorId())
                    .windowMinutes(entity.getWindowMinutes())
                    .thresholdCount(entity.getThresholdCount())
                    .thresholdAmount(entity.getThresholdAmount())
                    .dedupMinutes(entity.getDedupMinutes())
                    .isActive(entity.getIsActive())
                    .groupByFields(entity.getGroupByFields())
                    .aggregationField(entity.getAggregationField())
                    .whereJson(entity.getWhereJson())
                    .evaluationMode(entity.getEvaluationMode())
                    .build();
        }
    }
}
