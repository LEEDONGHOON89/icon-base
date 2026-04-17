package com.itmasters.icon.engine.dto;

import com.itmasters.icon.common.domain.type.ExecutionMode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 룰 탐지 실행 컨텍스트
 */
@Getter
@Builder
public class DetectionContext {
    private String executionId;
    private String dataSourceId;
    private String profileId;
    private LocalDateTime startTime;
    private ExecutionMode executionMode;
    private String executedBy;
    
    @Setter
    private LocalDateTime endTime;
    
    /**
     * 실행 완료 처리
     */
    public void complete() {
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * 실행 시간(밀리초) 반환
     */
    public long getExecutionTimeMillis() {
        if (startTime == null) {
            return 0;
        }
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return ChronoUnit.MILLIS.between(startTime, end);
    }
    
    /**
     * 실패 결과 생성
     */
    public static DetectionResult failed(DetectionContext context, String errorMessage) {
        return DetectionResult.builder()
                .context(context)
                .success(false)
                .errorMessage(errorMessage)
                .totalMatched(0)
                .totalRules(0)
                .build();
    }
}