package com.itmasters.icon.common.domain.scenario;

/**
 * 시나리오 내 규칙 간 논리 연산자
 */
public enum ScenarioOperator {
    AND("AND", "그리고"),
    OR("OR", "또는"),
    ANCHOR("ANCHOR", "앵커");
    
    private final String code;
    private final String displayName;
    
    ScenarioOperator(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
