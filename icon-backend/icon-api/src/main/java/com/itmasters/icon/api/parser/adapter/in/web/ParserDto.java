package com.itmasters.icon.api.parser.adapter.in.web;

import com.itmasters.icon.common.domain.type.ParserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

// [2026-04-20] 파서 CRUD DTO
public class ParserDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "파서 생성 요청")
    public static class CreateRequest {

        @NotBlank(message = "파서명은 필수입니다")
        @Schema(description = "파서명", example = "로그인 로그 파서")
        private String parserName;

        @NotNull(message = "파서 타입은 필수입니다")
        @Schema(description = "파서 타입: DELIMITER, FIXED_WIDTH, REGEX")
        private ParserType parserType;

        @Schema(description = "설명")
        private String description;

        @NotNull(message = "규칙 목록은 필수입니다")
        @Schema(description = "파서 규칙 목록")
        private List<RuleItem> rules;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "파서 수정 요청")
    public static class UpdateRequest {

        @Schema(description = "파서명")
        private String parserName;

        @Schema(description = "설명")
        private String description;

        @Schema(description = "활성화 여부")
        private Boolean isActive;

        @Schema(description = "파서 규칙 목록 (전체 교체)")
        private List<RuleItem> rules;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "파서 규칙 항목")
    public static class RuleItem {

        @Schema(description = "적용 순서", example = "0")
        private int ruleOrder;

        @NotBlank(message = "config_json은 필수입니다")
        @Schema(description = "파서 설정 JSON",
                example = "{\"delimiter\":\"|\",\"index\":0}")
        private String configJson;

        @Schema(description = "추출 결과를 저장할 표준 필드 ID")
        private String targetStandardFieldId;

        @Schema(description = "표준 필드 없을 때 사용할 커스텀 필드명")
        private String targetFieldName;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "파서 응답")
    public static class Response {

        @Schema(description = "파서 ID")
        private String parserId;

        @Schema(description = "파서명")
        private String parserName;

        @Schema(description = "파서 타입")
        private ParserType parserType;

        @Schema(description = "설명")
        private String description;

        @Schema(description = "활성화 여부")
        private Boolean isActive;

        @Schema(description = "파서 규칙 목록")
        private List<RuleResponse> rules;

        @Schema(description = "생성일시")
        private LocalDateTime createdAt;

        @Schema(description = "수정일시")
        private LocalDateTime updatedAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "파서 규칙 응답")
    public static class RuleResponse {

        @Schema(description = "규칙 ID")
        private String parserRuleId;

        @Schema(description = "적용 순서")
        private int ruleOrder;

        @Schema(description = "파서 설정 JSON")
        private String configJson;

        @Schema(description = "표준 필드 ID")
        private String targetStandardFieldId;

        @Schema(description = "커스텀 필드명")
        private String targetFieldName;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "파서 목록 요약 응답")
    public static class SummaryResponse {

        @Schema(description = "파서 ID")
        private String parserId;

        @Schema(description = "파서명")
        private String parserName;

        @Schema(description = "파서 타입")
        private ParserType parserType;

        @Schema(description = "설명")
        private String description;

        @Schema(description = "활성화 여부")
        private Boolean isActive;

        @Schema(description = "규칙 수")
        private int ruleCount;
    }
}
