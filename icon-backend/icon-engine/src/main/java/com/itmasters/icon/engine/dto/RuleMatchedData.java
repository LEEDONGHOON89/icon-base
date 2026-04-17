package com.itmasters.icon.engine.dto;


import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineRuleEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 룰에 매칭된 개별 데이터
 */
@Getter
@Builder
public class RuleMatchedData {
    private String ruleId;
    private String ruleName;
    private String ruleCondition;
    private Map<String, Object> mappedData;      // 매핑된 데이터
    private Map<String, Object> originalData;    // 원본 데이터
    private Integer rowNumber;                   // 원본 행 번호
    private EngineRuleEntity rule;
    private String groupKey;

    /**
     * 간단한 생성 메서드
     */
    public static RuleMatchedData of(EngineRuleEntity rule,
                                     Map<String, Object> mappedData,
                                     Map<String, Object> originalData,
                                     Integer rowNumber,
                                     String groupKey) {
        return RuleMatchedData.builder()
                .ruleId(rule.getRuleId())
                .ruleName(rule.getRuleName())
                .ruleCondition(rule.getWhereJson())
                .rule(rule)
                .mappedData(mappedData)
                .originalData(originalData)
                .rowNumber(rowNumber)
                .groupKey(groupKey)
                .build();
    }
}
