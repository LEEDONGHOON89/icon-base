package com.itmasters.icon.engine.dto;

/**
 * 단일 Aggregate 실행 결과

 * 책임:
 * - RuleProcessor.execute() 한 번의 실행 결과 반환
 * - 센서 탐지: predicateSensorId에 매칭된 이벤트 중 저장된 건수
 * - 룰 탐지: 현재 집계가 저장되었는지 여부
 *
 * @param savedSensors 이번 실행에서 저장된 detect_rules 건수 (센서 탐지)
 * @param savedRules   이번 실행에서 detect_aggregate가 저장되었는지 (0 또는 1, 룰 탐지)
 */
public record RuleExecutionResult(
        int savedSensors,
        int savedRules
) {
    public static RuleExecutionResult empty() {
        return new RuleExecutionResult(0, 0);
    }

    public static RuleExecutionResult sensors(int count) {
        return new RuleExecutionResult(count, 0);
    }

    public static RuleExecutionResult rule() {
        return new RuleExecutionResult(0, 1);
    }

    public static RuleExecutionResult both(int sensorCount) {
        return new RuleExecutionResult(sensorCount, 1);
    }
}
