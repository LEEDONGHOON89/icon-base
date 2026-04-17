package com.itmasters.icon.engine.dto;

/**
 * 공통 파이프라인 실행 결과

 * executeFromStep1() 메서드의 반환 타입으로 사용
 * SingleIngestService가 이 결과를 SingleRunResponse로 변환
 */
public record PipelineResult(
        Long execDsMpId,
        int savedEvents,               // DET-1 결과: Event Stream 저장 건수
        int updatedEntityAttributes,   // PREP-2 결과: Entity Attributes 업데이트 건수
        int savedSensors,              // DET-2-2 결과: 센서 탐지 저장 건수 (predicateSensorId)
        int savedRules,                // DET-2-2 결과: 룰 탐지 저장 건수 (threshold 초과 & dedup 통과)
        int savedScenarios             // DET-2-3 결과: 시나리오 평가 탐지 건수
) {
    /**
     * 빈 결과 생성 (실행 실패 시)
     */
    public static PipelineResult empty(Long execDsMpId) {
        return new PipelineResult(execDsMpId, 0, 0, 0, 0, 0);
    }
}
