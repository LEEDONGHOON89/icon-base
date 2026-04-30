package com.itmasters.icon.api.datasource.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * [2026-04-22] 매핑 결과 조회 DTO — TODO-002
 * GET /api/v1/data-sources/{dataSourceId}/mapped-storages
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MappedStorageDto {

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

    /** 매핑 결과 단건 */
    @Getter
    @Builder
    public static class Item {
        private final Long mappedStorageId;
        private final Long landingRecordId;
        private final Long execDsMpId;
        private final String dataSourceId;
        private final String transactionId;
        private final Integer rowIndex;
        private final Map<String, Object> rowData;
        private final String processingStatus;
        private final String errorMessage;
        private final LocalDateTime regDt;
    }

    /** 필터 결과 통계 요약 카드용 */
    @Getter
    @Builder
    public static class Summary {
        private final long total;
        private final long completedCount;
        private final long failedCount;
        private final LocalDateTime lastRegDt;
    }

    /** 상세 모달 — 원본 비교 탭용 (landing_records JOIN) */
    @Getter
    @Builder
    public static class DetailWithOrigin {
        private final Item mappedStorage;
        private final Long originLandingRecordId;
        private final Map<String, Object> rawPayload;
    }
}
