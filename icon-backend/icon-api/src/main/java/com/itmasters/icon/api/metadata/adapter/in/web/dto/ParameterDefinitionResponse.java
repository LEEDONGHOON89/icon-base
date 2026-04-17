package com.itmasters.icon.api.metadata.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParameterDefinitionResponse {
    
    private String name;           // 파라미터 이름 (예: "시간윈도우", "임계값")
    private String type;           // 파라미터 타입 (number, string, time, boolean)
    private boolean required;      // 필수 여부
    private String description;    // 파라미터 설명
    private String placeholder;    // 입력 힌트 (예: "예: 30", "예: 1000000")
    private String unit;          // 단위 (예: "분", "원", "개")
    private ValidationRule validation;  // 유효성 검사 규칙
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationRule {
        private Double min;          // 최소값 (number 타입)
        private Double max;          // 최대값 (number 타입)
        private Integer minLength;   // 최소 길이 (string 타입)
        private Integer maxLength;   // 최대 길이 (string 타입)
        private String pattern;      // 정규식 패턴 (string 타입)
        private String format;       // 형식 (예: "HH:mm", "YYYY-MM-DD")
    }
}