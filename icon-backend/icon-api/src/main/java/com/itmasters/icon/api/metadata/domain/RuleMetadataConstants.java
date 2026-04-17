package com.itmasters.icon.api.metadata.domain;

import com.itmasters.icon.api.metadata.adapter.in.web.dto.FieldMetadataResponse;
import com.itmasters.icon.api.metadata.adapter.in.web.dto.OperatorMetadataResponse;
import com.itmasters.icon.common.domain.rule.RuleField;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 룰 메타데이터 상수 정의
 * 생성일시: 2025-01-16 17:00:00
 * 수정일시: 2025-01-18 10:00:00
 * 
 * 룰 생성 시 사용되는 필드, 연산자, 카테고리 등의 메타데이터를 중앙에서 관리합니다.
 * Enum 기반으로 리팩토링하여 타입 안전성과 유지보수성을 향상시켰습니다.
 */
public class RuleMetadataConstants {
    
    /**
     * 사용 가능한 필드 목록
     * RuleField enum에서 FieldMetadataResponse로 변환
     */
    public static List<FieldMetadataResponse> getAvailableFields() {
        return Arrays.stream(RuleField.values())
            .map(field -> FieldMetadataResponse.builder()
                .name(field.getName())
                .label(field.getLabel())
                .category(field.getCategory().getLabel())
                .description(field.getDescription())
                .valueOptions(field.getValueOptions().stream()
                    .map(opt -> FieldMetadataResponse.ValueOption.builder()
                        .value(opt.getValue())
                        .label(opt.getLabel())
                        .build())
                    .collect(Collectors.toList()))
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * 사용 가능한 연산자 목록
     * RuleOperator enum에서 OperatorMetadataResponse로 변환
     */
    public static List<OperatorMetadataResponse> getAvailableOperators() {
        return Arrays.stream(RuleOperator.values())
            .map(operator -> OperatorMetadataResponse.builder()
                .value(operator.name())
                .label(operator.getLabel())
                .category(operator.getCategory())
                .description(operator.getDescription())
                .supportedTypes(operator.getSupportedTypeStrings())
                .requiresParameters(operator.requiresParameters())
                .parameterFormat(operator.getParameterFormat())
                .isAggregateOperator(operator.isAggregateOperator())
                .build())
            .collect(Collectors.toList());
    }
}