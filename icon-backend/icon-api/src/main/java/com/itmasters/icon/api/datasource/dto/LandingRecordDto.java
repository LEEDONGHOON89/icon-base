package com.itmasters.icon.api.datasource.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * [2026-04-22] 수집 원본 조회 DTO — TODO-001
 * GET /api/v1/data-sources/{dataSourceId}/landing-records
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LandingRecordDto {

    /** 페이지 응답 (목록 + 통계 요약) */
    @Getter
    @Builder
    public static class PageResponse {
        private final List<Item> data;
        private final long total;
        private final int page;
        private final int size;
        private final Summary summary;
    }

    /** 수집 원본 레코드 단건 */
    @Getter
    @Builder
    public static class Item {
        private final Long landingRecordId;
        private final Long execDsMpId;
        private final String dataSourceId;
        private final String sourceType;
        private final Integer rowIndex;
        private final Map<String, Object> rawPayload;
        private final String ingestionStatus;
        private final String ingestionMessage;
        private final LocalDateTime extractedAt;
    }

    /** 필터 결과 통계 요약 카드용 */
    @Getter
    @Builder
    public static class Summary {
        private final long total;
        private final long transformedCount;
        private final long failedCount;
        private final LocalDateTime lastExtractedAt;
    }
}
