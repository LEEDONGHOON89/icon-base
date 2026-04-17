package com.itmasters.icon.engine.dto;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineProfileEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Set;

/**
 * Step2 실행 결과
 * Step3에서 활용할 Event Stream 저장 결과 정보
 */
@Getter
@Builder
public class Step2Result {
    
    private final Step1Result step1Result;
    private final List<EventStreamResult> streamResults;
    private final List<EngineProfileEntity> processedProfiles;
    private final int totalStreamEvents;
    private final long executionTime;
    
    // Event Stream 저장 통계 정보
    private final Set<StreamKey> activeStreamKeys;     // 활성화된 StreamKey 조합
    private final int totalDataRows;                   // 총 처리된 데이터 행 수
    private final int filteredRows;                    // 필터링으로 제외된 행 수
    private final int duplicateRows;                   // 중복으로 제외된 행 수
    private final int actualSavedEvents;               // 실제 저장된 이벤트 수
    
    /**
     * 빈 결과 생성 (프로파일이 없는 경우)
     */
    public static Step2Result empty(Step1Result step1Result) {
        return Step2Result.builder()
                .step1Result(step1Result)
                .streamResults(List.of())
                .processedProfiles(List.of())
                .totalStreamEvents(0)
                .executionTime(System.currentTimeMillis())
                .activeStreamKeys(Set.of())
                .totalDataRows(0)
                .filteredRows(0)
                .duplicateRows(0)
                .actualSavedEvents(0)
                .build();
    }
    
    /**
     * Step3에서 활용할 기본 정보 반환
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
     * 성공한 프로파일만 반환
     */
    public List<EventStreamResult> getSuccessfulResults() {
        return streamResults.stream()
                .filter(EventStreamResult::isSuccess)
                .toList();
    }
    
    /**
     * 실패한 프로파일만 반환  
     */
    public List<EventStreamResult> getFailedResults() {
        return streamResults.stream()
                .filter(result -> !result.isSuccess())
                .toList();
    }
    
    public boolean hasFailures() {
        return streamResults.stream().anyMatch(result -> !result.isSuccess());
    }
    
    public boolean hasSuccesses() {
        return streamResults.stream().anyMatch(EventStreamResult::isSuccess);
    }
    
    /**
     * Event Stream 저장 효율성 계산
     */
    public double getStorageEfficiency() {
        if (totalDataRows == 0) return 0.0;
        return (double) actualSavedEvents / totalDataRows * 100.0;
    }
    
    /**
     * 중복 제거 효과 계산
     */
    public double getDuplicationReductionRate() {
        if (totalDataRows == 0) return 0.0;
        return (double) duplicateRows / totalDataRows * 100.0;
    }
    
    /**
     * StreamKey 조합별 통계 정보 반환
     */
    public String getStreamKeysSummary() {
        if (activeStreamKeys == null || activeStreamKeys.isEmpty()) {
            return "No StreamKeys";
        }
        
        return activeStreamKeys.stream()
                .map(StreamKey::getGroupKey)
                .reduce((a, b) -> a + ", " + b)
                .orElse("None");
    }
    
    /**
     * 상세 통계 로깅용 문자열
     */
    public String getDetailedStats() {
        return String.format(
            "Event Stream 저장 통계:\n" +
            "  - 활성 StreamKey: %d 개 (%s)\n" +
            "  - 총 데이터 행: %,d 개\n" +
            "  - 실제 저장: %,d 개 (%.1f%%)\n" +
            "  - 필터링 제외: %,d 개\n" +
            "  - 중복 제거: %,d 개 (%.1f%%)\n" +
            "  - 실행 시간: %,d ms",
            activeStreamKeys != null ? activeStreamKeys.size() : 0,
            getStreamKeysSummary(),
            totalDataRows,
            actualSavedEvents,
            getStorageEfficiency(),
            filteredRows,
            duplicateRows,
            getDuplicationReductionRate(),
            executionTime
        );
    }
}