package com.itmasters.icon.engine.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 룰 탐지 결과
 */
@Getter
@Builder
public class DetectionResult {
    private DetectionContext context;
    private boolean success;
    private String errorMessage;
    private int totalRows;      // totalRecords -> totalRows로 변경 (DB 컬럼명과 일치)
    private int totalMatched;
    private int totalRules;     // 실행한 룰 개수 추가
    
    @Builder.Default
    private List<Map<String, Object>> matchedRecords = new ArrayList<>();
    
    @Builder.Default
    private List<RuleMatchDetail> matchDetails = new ArrayList<>();
    
    @Builder.Default
    private List<RuleMatchedData> matchedData = new ArrayList<>();  // 매칭된 상세 데이터 추가

    @Builder.Default
    private java.util.Set<String> matchedRuleIds = new java.util.HashSet<>();

    @Builder.Default
    private java.util.Set<String> matchedGroupKeys = new java.util.HashSet<>();
    
    /**
     * 성공 결과 생성
     */
    public static DetectionResult success(DetectionContext context,
                                         int totalRows,
                                         int totalMatched,
                                         List<Map<String, Object>> matchedRecords,
                                         java.util.Set<String> matchedRuleIds,
                                         java.util.Set<String> matchedGroupKeys) {
        return DetectionResult.builder()
                .context(context)
                .success(true)
                .totalRows(totalRows)
                .totalMatched(totalMatched)
                .matchedRecords(matchedRecords != null ? matchedRecords : new ArrayList<>())
                .matchedRuleIds(matchedRuleIds != null ? matchedRuleIds : new java.util.HashSet<>())
                .matchedGroupKeys(matchedGroupKeys != null ? matchedGroupKeys : new java.util.HashSet<>())
                .build();
    }
    
    /**
     * 실패 결과 생성
     */
    public static DetectionResult failed(DetectionContext context, String errorMessage) {
        return DetectionResult.builder()
                .context(context)
                .success(false)
                .errorMessage(errorMessage)
                .totalRows(0)
                .totalMatched(0)
                .totalRules(0)
                .matchedRuleIds(new java.util.HashSet<>())
                .matchedGroupKeys(new java.util.HashSet<>())
                .build();
    }
    
    /**
     * 빌더에서 totalRecords 사용 시 totalRows로 설정 (호환성)
     */
    public int getTotalRecords() {
        return totalRows;
    }
    
    /**
     * 룰 매치 상세 정보
     */
    @Getter
    @Builder
    public static class RuleMatchDetail {
        private String ruleId;
        private String ruleName;
        private int matchCount;
        private List<Map<String, Object>> matches;
    }
}
