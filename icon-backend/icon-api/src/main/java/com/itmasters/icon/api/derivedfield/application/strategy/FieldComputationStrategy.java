package com.itmasters.icon.api.derivedfield.application.strategy;

import com.itmasters.icon.api.derivedfield.application.strategy.context.ComputationContext;

/**
 * 파생 필드 계산 Strategy 인터페이스
 *
 * computation_type별로 구현체를 만들어 다양한 계산 방식을 지원합니다.
 * - FIELD_COMPARISON: FieldComparisonStrategy
 * - ENTITY_RELATION_LOOKUP: EntityRelationLookupStrategy
 * - EVENT_STREAM_LOOKUP: EventStreamLookupStrategy
 * - ENTITY_ATTRIBUTE_LOOKUP: EntityAttributeLookupStrategy
 * - FORMULA: FormulaStrategy
 */
public interface FieldComputationStrategy {

    /**
     * 파생 필드 값을 계산합니다
     *
     * @param context 계산에 필요한 모든 컨텍스트
     * @return 계산된 값 (Boolean, String, Number 등)
     */
    Object compute(ComputationContext context);

    /**
     * 이 Strategy가 처리할 수 있는 computation_type 반환
     *
     * @return computation_type 문자열
     */
    String getSupportedComputationType();
}
