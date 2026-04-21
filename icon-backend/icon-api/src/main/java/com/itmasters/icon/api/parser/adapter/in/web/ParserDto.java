package com.itmasters.icon.api.parser.adapter.in.web;

import com.itmasters.icon.common.domain.type.ParserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

// [2026-04-20] 파서 CRUD DTO
// [2026-04-20] 재설계: sourceField/configJson 파서레벨 추가, 규칙은 출력필드명만 관리
// [2026-04-21] 파서 규칙에서 targetStandardFieldId 제거
public class ParserDto {

    // ─── 파서 생성 요청 ──────────────────────────────────────────────────────
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

        @NotBlank(message = "파싱 대상 필드명은 필수입니다")
        @Schema(description = "파싱 대상 원본 필드명", example = "line")
        private String sourceField;

        /**
         * 파서 공통 설정 JSON
         * DELIMITER  : {"delimiter":"|"}
         * FIXED_WIDTH: null
         * REGEX      : null
         */
        @Schema(description = "파서 공통 설정 JSON (DELIMITER: {\"delimiter\":\"|\"})")
        private String configJson;

        @Schema(description = "설명")
        private String description;

        @NotNull(message = "규칙 목록은 필수입니다")
        @Schema(description = "파서 규칙 목록 (출력 필드 정의)")
        private List<RuleItem> rules;
    }

    // ─── 파서 수정 요청 ──────────────────────────────────────────────────────
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "파서 수정 요청")
    public static class UpdateRequest {

        @Schema(description = "파서명")
        private String parserName;

        @Schema(description = "파싱 대상 원본 필드명")
        private String sourceField;

        @Schema(description = "파서 공통 설정 JSON")
        private String configJson;

        @Schema(description = "설명")
        private String description;

        @Schema(description = "활성화 여부")
        private Boolean isActive;

        @Schema(description = "파서 규칙 목록 (전체 교체)")
        private List<RuleItem> rules;
    }

    // ─── 파서 규칙 항목 ──────────────────────────────────────────────────────
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "파서 규칙 항목 (출력 필드 하나)")
    public static class RuleItem {

        @Schema(description = "적용 순서 (DELIMITER: split 인덱스, FIXED_WIDTH: 바이트 순서)", example = "0")
        private int ruleOrder;

        /**
         * 규칙별 설정 JSON
         * DELIMITER  : null
         * FIXED_WIDTH: {"byteLength":4}
         * REGEX      : {"pattern":"^(\\w+)","group":1}
         */
        @Schema(description = "규칙별 설정 JSON (FIXED_WIDTH: {\"byteLength\":4}, REGEX: {\"pattern\":\"...\",\"group\":1})")
        private String configJson;

        @NotBlank(message = "출력 필드명은 필수입니다")
        @Schema(description = "출력 필드명", example = "COLUMN1")
        private String targetFieldName;
    }

    // ─── 파서 응답 ───────────────────────────────────────────────────────────
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "파서 상세 응답")
    public static class Response {

        @Schema(description = "파서 ID")
        private String parserId;

        @Schema(description = "파서명")
        private String parserName;

        @Schema(description = "파서 타입")
        private ParserType parserType;

        @Schema(description = "파싱 대상 원본 필드명")
        private String sourceField;

        @Schema(description = "파서 공통 설정 JSON")
        private String configJson;

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

    // ─── 파서 규칙 응답 ──────────────────────────────────────────────────────
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

        @Schema(description = "규칙별 설정 JSON")
        private String configJson;

        @Schema(description = "출력 필드명")
        private String targetFieldName;
    }

    // ─── 파서 목록 요약 응답 ──────────────────────────────────────────────────
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

        @Schema(description = "파싱 대상 원본 필드명")
        private String sourceField;

        @Schema(description = "설명")
        private String description;

        @Schema(description = "활성화 여부")
        private Boolean isActive;

        @Schema(description = "규칙 수")
        private int ruleCount;
    }

    // ─── 데이터소스-파서 연결 요청 ────────────────────────────────────────────
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "데이터소스-파서 연결 요청")
    public static class LinkRequest {

        @NotBlank(message = "파서 ID는 필수입니다")
        @Schema(description = "연결할 파서 ID")
        private String parserId;

        @Schema(description = "파서 적용 순서", example = "0")
        private int parserOrder;
    }

    // ─── 데이터소스-파서 연결 응답 ────────────────────────────────────────────
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "데이터소스-파서 연결 응답")
    public static class LinkResponse {

        @Schema(description = "연결 ID")
        private String dataSourceParserId;

        @Schema(description = "데이터소스 ID")
        private String dataSourceId;

        @Schema(description = "파서 정보")
        private SummaryResponse parser;

        @Schema(description = "파서 적용 순서")
        private int parserOrder;

        @Schema(description = "활성 여부")
        private boolean isActive;
    }
}
