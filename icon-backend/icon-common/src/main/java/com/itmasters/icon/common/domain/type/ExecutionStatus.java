package com.itmasters.icon.common.domain.type;

/**
 * 실행 상태 enum
 */
public enum ExecutionStatus {
    RUNNING("실행 중"),
    SUCCESS("성공"),
    FAILED("실패"),
    CANCELLED("취소됨");

    private final String description;

    ExecutionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}