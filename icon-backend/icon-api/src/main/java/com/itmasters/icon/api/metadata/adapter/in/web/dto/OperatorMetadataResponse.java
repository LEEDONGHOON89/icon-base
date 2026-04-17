package com.itmasters.icon.api.metadata.adapter.in.web.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorMetadataResponse {
    
    private String value;          // 연산자 이름 (예: "EQUALS", "GREATER_THAN", "IN")
    private String symbol;         // 연산자 기호 (예: "=", ">", "in")
    private String label;          // 표시명 (예: "같음", "초과", "포함")
    private String category;       // 카테고리 (예: "기본 비교", "포함 관련")
    private String description;    // 설명
    private String[] supportedTypes; // 지원하는 데이터 타입 (예: ["number", "string"])
    
    // 파라미터 관련 필드 추가
    private boolean requiresParameters; // 파라미터가 필요한지 여부
    private String parameterFormat;     // 파라미터 형식 (예: "시간윈도우(분),임계값")
    private boolean isAggregateOperator; // 집계 연산자인지 여부
    private List<ParameterDefinitionResponse> parameterDefinitions; // 파라미터 정의 목록
}