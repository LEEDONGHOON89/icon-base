package com.itmasters.icon.api.scenario.domain;

import com.itmasters.icon.entity.Auditable;
import com.itmasters.icon.common.domain.scenario.ScenarioOperator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시나리오-원자규칙 매핑 도메인
 * 시나리오와 원자규칙 간의 N:M 관계를 관리
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioRule extends Auditable {
    private String scenarioRuleId; // 매핑 고유 ID (시퀀스)
    private String scenarioId; // 시나리오 ID (부모) - TSID
    private String ruleId; // 룰 ID (참조) - TSID
    private Integer orderNo; // 적용 순서(옵션)
    private ScenarioOperator operator = ScenarioOperator.AND; // 논리 연산자 (기본값 AND)

    /**
     * 시나리오-규칙 매핑 생성 Factory 메서드
     */
    public static ScenarioRule of(String scenarioId, String ruleId) {
        ScenarioRule scenarioRule = new ScenarioRule();
        scenarioRule.scenarioId = scenarioId;
        scenarioRule.ruleId = ruleId;
        return scenarioRule;
    }

    /**
     * 시나리오-규칙 매핑 생성 (순서 포함)
     */
    public static ScenarioRule of(String scenarioId, String ruleId, Integer orderNo) {
        ScenarioRule scenarioRule = new ScenarioRule();
        scenarioRule.scenarioId = scenarioId;
        scenarioRule.ruleId = ruleId;
        scenarioRule.orderNo = orderNo;
        return scenarioRule;
    }

    /**
     * 시나리오-규칙 매핑 생성 (순서 및 연산자 포함)
     */
    public static ScenarioRule of(String scenarioId, String ruleId, Integer orderNo,
                                  ScenarioOperator operator) {
        ScenarioRule domain = new ScenarioRule();
        domain.scenarioId = scenarioId;
        domain.ruleId = ruleId;
        domain.orderNo = orderNo;
        domain.operator = operator != null ? operator : ScenarioOperator.AND;
        return domain;
    }
    
    /**
     * ID 할당 (Repository에서 사용)
     */
    public void assignId(String id) {
        if (this.scenarioRuleId != null) {
            throw new IllegalStateException("ID가 이미 할당되었습니다");
        }
        this.scenarioRuleId = id;
    }

    /**
     * 순서 설정
     */
    public void setOrder(Integer orderNo) {
        this.orderNo = orderNo;
    }

    /**
     * 연산자 설정
     */
    public void setOperator(ScenarioOperator operator) {
        this.operator = operator != null ? operator : ScenarioOperator.AND;
    }

    /**
     * 매핑 검증
     */
    public void validate() {
        if (scenarioId == null) {
            throw new IllegalArgumentException("시나리오 ID는 필수입니다.");
        }
        if (ruleId == null) {
            throw new IllegalArgumentException("규칙 ID는 필수입니다.");
        }
        if (orderNo != null && orderNo < 0) {
            throw new IllegalArgumentException("순서는 0 이상이어야 합니다.");
        }
    }

}
