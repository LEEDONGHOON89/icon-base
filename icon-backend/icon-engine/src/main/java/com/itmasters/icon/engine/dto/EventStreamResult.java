package com.itmasters.icon.engine.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event Stream 저장 결과
 */
@Getter
@Builder
public class EventStreamResult {
    
    private final String profileId;
    private final boolean success;
    private final int totalEvents;
    private final List<Long> savedEventStreamIds;
    private final String errorMessage;
    private final LocalDateTime processedAt;
    private final long processingTimeMillis;
    
    // 추가 통계 정보
    private final int processedRows;       // 처리된 총 행 수
    private final int filteredRows;        // 필터링으로 제외된 행 수  
    private final int duplicateRows;       // 중복으로 제외된 행 수
    
    /**
     * 성공 결과 생성 (통계 정보 포함)
     */
    public static EventStreamResult success(String profileId, 
                                          List<Long> savedEventStreamIds,
                                          int processedRows,
                                          int filteredRows,
                                          int duplicateRows) {
        return EventStreamResult.builder()
                .profileId(profileId)
                .success(true)
                .totalEvents(savedEventStreamIds.size())
                .savedEventStreamIds(savedEventStreamIds)
                .processedAt(LocalDateTime.now())
                .processedRows(processedRows)
                .filteredRows(filteredRows)
                .duplicateRows(duplicateRows)
                .build();
    }
    
    /**
     * 성공 결과 생성 (간단 버전, 하위 호환성)
     */
    public static EventStreamResult success(String profileId, 
                                          List<Long> savedEventStreamIds) {
        return success(profileId, savedEventStreamIds, 0, 0, 0);
    }
    
    /**
     * 실패 결과 생성
     */
    public static EventStreamResult failed(String profileId, String errorMessage) {
        return EventStreamResult.builder()
                .profileId(profileId)
                .success(false)
                .totalEvents(0)
                .savedEventStreamIds(List.of())
                .errorMessage(errorMessage)
                .processedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 저장된 이벤트가 있는지 확인
     */
    public boolean hasEvents() {
        return totalEvents > 0;
    }
    
    /**
     * 저장된 첫 번째 이벤트 ID 반환
     */
    public Long getFirstEventStreamId() {
        return savedEventStreamIds.isEmpty() ? null : savedEventStreamIds.get(0);
    }
    
    /**
     * 저장된 마지막 이벤트 ID 반환
     */
    public Long getLastEventStreamId() {
        return savedEventStreamIds.isEmpty() ? null : savedEventStreamIds.get(savedEventStreamIds.size() - 1);
    }
}