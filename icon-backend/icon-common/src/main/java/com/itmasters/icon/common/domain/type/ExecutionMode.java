package com.itmasters.icon.common.domain.type;

/**
 * 실행 모드 enum
 */
public enum ExecutionMode {
    MANUAL("수동 실행"),
    SCHEDULED("스케줄 실행"),
    AUTO("자동 실행"),
    TEST("테스트 실행");

    private final String description;

    ExecutionMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}