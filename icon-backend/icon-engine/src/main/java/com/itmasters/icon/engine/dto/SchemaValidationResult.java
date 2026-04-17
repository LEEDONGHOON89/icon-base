package com.itmasters.icon.engine.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 스키마 검증 결과 DTO
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SchemaValidationResult {
    
    private final boolean isValid;
    private final String message;
    private final List<String> errors;
    private final List<String> warnings;
    
    /**
     * 검증 성공
     */
    public static SchemaValidationResult success(String message) {
        return new SchemaValidationResult(true, message, List.of(), List.of());
    }
    
    /**
     * 검증 성공 (경고 포함)
     */
    public static SchemaValidationResult successWithWarnings(String message, List<String> warnings) {
        return new SchemaValidationResult(true, message, List.of(), warnings);
    }
    
    /**
     * 경고만 있는 경우 (경고가 있어도 검증은 성공)
     */
    public static SchemaValidationResult warning(String message) {
        return new SchemaValidationResult(true, message, List.of(), List.of(message));
    }
    
    /**
     * 검증 실패
     */
    public static SchemaValidationResult failure(String message, List<String> errors, List<String> warnings) {
        return new SchemaValidationResult(false, message, errors, warnings);
    }
    
    /**
     * 오류가 있는지 확인
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    /**
     * 경고가 있는지 확인
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
}