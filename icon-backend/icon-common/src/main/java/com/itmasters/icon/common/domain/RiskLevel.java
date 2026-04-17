package com.itmasters.icon.common.domain;

import java.util.Arrays;

/**
 * 위험수준 정의
 * DB risk_levels 테이블과 매핑되는 enum
 */
public enum RiskLevel {

    MONITOR("MONITOR", 10, "모니터링", "ALERT"),
    INTENSIVE("INTENSIVE", 20, "집중모니터링", "ALERT"),
    REVIEW("REVIEW", 30, "심사", "HOLD"),
    BLOCK("BLOCK", 40, "차단", "BLOCK");

    private final String id;
    private final int levelCode;
    private final String displayName;
    private final String actionType;

    RiskLevel(String id, int levelCode, String displayName, String actionType) {
        this.id = id;
        this.levelCode = levelCode;
        this.displayName = displayName;
        this.actionType = actionType;
    }

    public String getId() {
        return id;
    }

    public int getLevelCode() {
        return levelCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getActionType() {
        return actionType;
    }

    /**
     * 현재 레벨이 지정된 레벨 이상인지 확인
     * 예: BLOCK.isAtLeast(REVIEW) → true
     */
    public boolean isAtLeast(RiskLevel other) {
        return this.levelCode >= other.levelCode;
    }

    /**
     * 현재 레벨이 지정된 레벨보다 높은지 확인
     */
    public boolean isHigherThan(RiskLevel other) {
        return this.levelCode > other.levelCode;
    }

    /**
     * 차단 수준인지 확인
     */
    public boolean isBlocking() {
        return "BLOCK".equals(this.actionType);
    }

    /**
     * 알림이 필요한 수준인지 확인 (INTENSIVE 이상)
     */
    public boolean requiresNotification() {
        return this.levelCode >= INTENSIVE.levelCode;
    }

    /**
     * ID로 RiskLevel 찾기
     */
    public static RiskLevel fromId(String id) {
        if (id == null) {
            return MONITOR;
        }
        return Arrays.stream(values())
            .filter(r -> r.id.equals(id))
            .findFirst()
            .orElse(MONITOR);
    }

    /**
     * levelCode로 RiskLevel 찾기
     */
    public static RiskLevel fromLevelCode(int levelCode) {
        return Arrays.stream(values())
            .filter(r -> r.levelCode == levelCode)
            .findFirst()
            .orElse(MONITOR);
    }
}
