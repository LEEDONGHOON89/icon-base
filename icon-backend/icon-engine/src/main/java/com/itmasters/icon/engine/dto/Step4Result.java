package com.itmasters.icon.engine.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

/**
 * Step4 실행 결과 (Event Stream 저장)
 * Step5에서 활용할 Event Stream 저장 결과 정보
 */
@Getter
@Builder
public class Step4Result {

    private final Step1Result step1Result;
    private final EventStreamResult streamResult;
    private final Set<StreamKey> activeStreamKeys;
    private final long executionTime;            // 실행 시간 (ms)

    /**
     * 빈 결과 생성 (데이터가 없는 경우)
     */
    public static Step4Result empty(Step1Result step1Result) {
        return Step4Result.builder()
                .step1Result(step1Result)
                .streamResult(null)
                .activeStreamKeys(Set.of())
                .executionTime(System.currentTimeMillis())
                .build();
    }

    /**
     * Step5에서 활용할 기본 정보 반환
     */
    public String getDataSourceId() {
        return step1Result.getDataSourceId();
    }

    public Long getExecDsMpId() {
        return step1Result.getExecDsMpId();
    }

    public int getTotalRows() {
        return step1Result.getTotalRows();
    }

    /**
     * Event Stream 저장 성공 여부
     */
    public boolean isSuccess() {
        return streamResult != null && streamResult.isSuccess();
    }

    /**
     * 저장된 이벤트 수
     */
    public int getTotalEvents() {
        return streamResult != null ? streamResult.getTotalEvents() : 0;
    }
}
