package com.itmasters.icon.engine.dto;

/**
 * DET-2-2 집계 평가 결과

 * 책임:
 * - 집계 평가 프로세서가 실제 저장한 탐지 건수 반환
 * - 센서 탐지 건수: 집계의 predicateSensorId에 매칭된 이벤트 수
 * - 룰 탐지 건수: threshold를 넘고 dedup 조건을 통과한 집계 수
 *
 * @param savedSensors 실제 저장된 detect_rules 건수 (센서 탐지)
 * @param savedRules   실제 저장된 detect_aggregates 건수 (룰 탐지)
 */
public record RuleEvaluationResult(
        int savedSensors,
        int savedRules
) {
    public static RuleEvaluationResult empty() {
        return new RuleEvaluationResult(0, 0);
    }
}
