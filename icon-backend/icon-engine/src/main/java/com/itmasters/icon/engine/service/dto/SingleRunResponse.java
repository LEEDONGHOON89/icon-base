package com.itmasters.icon.engine.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
public class SingleRunResponse {
    private Long execDsMpId;
    private int savedEventStreams;
    private int updatedEntityAttributes; // Entity Attributes 업데이트 개수
    private int savedSensors;            // 센서 탐지 저장 건수 (predicateSensorId)
    private int savedRules;              // 룰 탐지 저장 건수 (threshold 초과 & dedup 통과)
    private int savedScenarios;          // 시나리오 탐지 저장 건수
    private boolean success;             // 실행 성공 여부
    private String errorMessage;         // 에러 메시지 (실패 시)

    /**
     * 에러 응답 생성
     */
    public static SingleRunResponse error(String errorMessage) {
        return new SingleRunResponse(
            null,    // execDsMpId
            0,       // savedEventStreams
            0,       // updatedEntityAttributes
            0,       // savedSensors
            0,       // savedRules
            0,       // savedScenarios
            false,   // success
            errorMessage
        );
    }

    /**
     * 성공 응답 생성 (기존 of 메서드 대체)
     */
    public static SingleRunResponse success(
        Long execDsMpId,
        int savedEventStreams,
        int updatedEntityAttributes,
        int savedSensors,
        int savedRules,
        int savedScenarios
    ) {
        return new SingleRunResponse(
            execDsMpId,
            savedEventStreams,
            updatedEntityAttributes,
            savedSensors,
            savedRules,
            savedScenarios,
            true,    // success
            null     // errorMessage
        );
    }
}
