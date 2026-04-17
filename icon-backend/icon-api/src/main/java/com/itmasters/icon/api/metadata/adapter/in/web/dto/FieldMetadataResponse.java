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
public class FieldMetadataResponse {
    
    private String name;           // 필드명 (예: "transaction_amount")
    private String label;          // 표시명 (예: "거래 금액")
    private String category;       // 카테고리 (예: "거래 관련", "접속/로그인 관련")
    private String description;    // 설명
    private List<ValueOption> valueOptions;  // 선택 가능한 값 목록 (enum 타입의 경우)
    private List<OperatorMetadataResponse> availableOperators;  // 이 필드에서 사용 가능한 연산자 목록
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValueOption {
        private String value;
        private String label;
    }
}