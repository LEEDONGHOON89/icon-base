package com.itmasters.icon.api.rulesnapshot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 학습용 룰 스냅샷 DTO
 */
public class RuleSnapshotDto {

    /**
     * 웹훅 요청 바디
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @JsonProperty("rules_json")
        private List<RuleData> rulesJson;
    }

    /**
     * 개별 룰 데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleData {
        private String id;
        private String name;
        private String description;

        @JsonProperty("where_json")
        private String whereJson;
    }

    /**
     * 스냅샷 결과 응답
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private boolean success;
        private String message;
        private int ruleCount;
    }
}
