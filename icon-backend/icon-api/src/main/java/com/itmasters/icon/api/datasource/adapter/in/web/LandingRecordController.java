package com.itmasters.icon.api.datasource.adapter.in.web;

import com.itmasters.icon.api.datasource.application.service.LandingRecordService;
import com.itmasters.icon.api.datasource.dto.LandingRecordDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [2026-04-22] 수집 원본 조회 REST API — TODO-001
 * GET /api/v1/data-sources/{dataSourceId}/landing-records
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/data-sources")
@Tag(name = "수집 원본 조회 API", description = "landing_records 조회 — 기간/상태/JSONB 필터 지원")
public class LandingRecordController {

    private final LandingRecordService landingRecordService;

    /**
     * 데이터소스별 수집 원본 목록 조회
     *
     * @param dataSourceId    데이터소스 ID
     * @param startDate       조회 시작 시각 (ISO datetime, 기본값: 24시간 전)
     * @param endDate         조회 종료 시각 (ISO datetime, 기본값: 현재)
     * @param ingestionStatus 수집 상태 필터 — NEW | TRANSFORMED | FAILED (반복 가능)
     * @param custNo          raw_payload->>'cust_no' 단축 필터
     * @param jsonFilters     JSONB 확장 필터 — key:op:value 형식 (반복 가능)
     *                        op: eq | like | neq
     * @param page            0-based 페이지 번호 (기본값: 0)
     * @param size            페이지 크기 (기본값: 20, 최대: 100)
     */
    @GetMapping("/{dataSourceId}/landing-records")
    @Operation(
            summary = "수집 원본 목록 조회",
            description = "landing_records 테이블 조회. 기간·상태 기본 필터와 JSONB 확장 필터(jsonFilters) 지원.")
    public LandingRecordDto.PageResponse getLandingRecords(
            @Parameter(description = "데이터소스 ID") @PathVariable String dataSourceId,
            @Parameter(description = "조회 시작 시각 (ISO datetime)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "조회 종료 시각 (ISO datetime)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "수집 상태 (반복 가능): NEW | TRANSFORMED | FAILED")
            @RequestParam(name = "ingestionStatus", required = false) List<String> ingestionStatus,
            @Parameter(description = "cust_no 단축 필터 (raw_payload->>'cust_no')")
            @RequestParam(required = false) String custNo,
            @Parameter(description = "JSONB 확장 필터 (반복 가능): key:op:value")
            @RequestParam(name = "jsonFilters", required = false) List<String> jsonFilters,
            @Parameter(description = "페이지 번호 (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 100)") @RequestParam(defaultValue = "20") int size) {

        return landingRecordService.search(
                dataSourceId, startDate, endDate,
                ingestionStatus, custNo, jsonFilters,
                page, size);
    }
}
