package com.itmasters.icon.engine.datasource;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 스키마 검증 결과
 */
@Getter
public class ValidationResult {
    
    private final boolean valid;
    private final String message;
    private final List<String> errors;
    private final List<String> warnings;
    
    private ValidationResult(boolean valid, String message, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.message = message;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }
    
    /**
     * 성공 결과 생성
     */
    public static ValidationResult success(String message) {
        return new ValidationResult(true, message, null, null);
    }
    
    /**
     * 경고와 함께 성공 결과 생성
     */
    public static ValidationResult successWithWarnings(String message, List<String> warnings) {
        return new ValidationResult(true, message, null, warnings);
    }
    
    /**
     * 실패 결과 생성
     */
    public static ValidationResult failure(String message, List<String> errors) {
        return new ValidationResult(false, message, errors, null);
    }
    
    /**
     * 오류와 경고를 모두 포함한 실패 결과 생성
     */
    public static ValidationResult failure(String message, List<String> errors, List<String> warnings) {
        return new ValidationResult(false, message, errors, warnings);
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
    
    /**
     * 상세 메시지 생성
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        
        if (hasErrors()) {
            sb.append("\n오류:");
            errors.forEach(error -> sb.append("\n  - ").append(error));
        }
        
        if (hasWarnings()) {
            sb.append("\n경고:");
            warnings.forEach(warning -> sb.append("\n  - ").append(warning));
        }
        
        return sb.toString();
    }
}