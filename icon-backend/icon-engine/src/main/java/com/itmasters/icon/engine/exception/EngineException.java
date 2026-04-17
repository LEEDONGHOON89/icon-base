package com.itmasters.icon.engine.exception;

/**
 * 엔진 실행 중 발생하는 예외
 * 치명적인 오류(DB 에러, 스키마 불일치 등)에 사용
 */
public class EngineException extends RuntimeException {

    private final String errorCode;
    private final Long execDsMpId;

    public EngineException(String message) {
        super(message);
        this.errorCode = "ENGINE_ERROR";
        this.execDsMpId = null;
    }

    public EngineException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.execDsMpId = null;
    }

    public EngineException(String errorCode, String message, Long execDsMpId) {
        super(message);
        this.errorCode = errorCode;
        this.execDsMpId = execDsMpId;
    }

    public EngineException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.execDsMpId = null;
    }

    public EngineException(String errorCode, String message, Long execDsMpId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.execDsMpId = execDsMpId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Long getExecDsMpId() {
        return execDsMpId;
    }

    // 자주 사용되는 에러 팩토리 메서드
    public static EngineException dataSourceNotFound(String dataSourceId) {
        return new EngineException("DS_NOT_FOUND",
            "데이터소스를 찾을 수 없습니다: " + dataSourceId);
    }

    public static EngineException profileNotFound(String profileId) {
        return new EngineException("PROFILE_NOT_FOUND",
            "프로파일을 찾을 수 없습니다: " + profileId);
    }

    public static EngineException dataProcessingFailed(Long execDsMpId, String reason) {
        return new EngineException("DATA_PROCESSING_FAILED",
            "데이터 처리 실패: " + reason, execDsMpId);
    }

    public static EngineException databaseError(Long execDsMpId, String detail, Throwable cause) {
        return new EngineException("DATABASE_ERROR",
            "데이터베이스 오류: " + detail, execDsMpId, cause);
    }

    public static EngineException schemaError(String detail) {
        return new EngineException("SCHEMA_ERROR",
            "스키마 오류: " + detail);
    }

    public static EngineException validationFailed(String detail) {
        return new EngineException("VALIDATION_FAILED",
            "검증 실패: " + detail);
    }
}
