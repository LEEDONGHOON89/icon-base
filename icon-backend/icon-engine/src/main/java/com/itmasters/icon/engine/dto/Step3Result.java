package com.itmasters.icon.engine.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Step3 실행 결과 (파생 필드 계산)
 * Step4에서 활용할 파생 필드 계산 결과 정보
 */
@Getter
@Builder
public class Step3Result {

    private final Step1Result step1Result;
    private final int calculatedFieldsCount;     // 파생 필드가 계산된 행 수
    private final long executionTime;            // 실행 시간 (ms)

    /**
     * 빈 결과 생성 (데이터가 없는 경우)
     */
    public static Step3Result empty(Step1Result step1Result) {
        return Step3Result.builder()
                .step1Result(step1Result)
                .calculatedFieldsCount(0)
                .executionTime(System.currentTimeMillis())
                .build();
    }

    /**
     * Step4에서 활용할 기본 정보 반환
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
     * 파생 필드 계산 성공 여부
     */
    public boolean isSuccess() {
        return calculatedFieldsCount >= 0;
    }
}
