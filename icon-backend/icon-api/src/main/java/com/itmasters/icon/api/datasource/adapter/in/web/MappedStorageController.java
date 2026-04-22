package com.itmasters.icon.api.datasource.adapter.in.web;

import com.itmasters.icon.api.datasource.application.service.MappedStorageService;
import com.itmasters.icon.api.datasource.dto.MappedStorageDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [2026-04-22] 매핑 결과 조회 REST API — TODO-002
 * GET /api/v1/data-sources/{dataSourceId}/mapped-storages
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/data-sources")
@Tag(name = "매핑 결과 조회 API", description = "mapped_storages 조회 — landing_records JOIN, JSONB 필터 지원")
public class MappedStorageController {

    private final MappedStorageService mappedStorageService;

    /**
     * 데이터소스별 매핑 결과 목록 조회
     *
     * @param dataSourceId    데이터소스 ID
     * @param startDate       조회 시작 시각 (ISO datetime, 기본값: 24시간 전)
     * @param endDate         조회 종료 시각 (ISO datetime, 기본값: 현재)
     * @param processingStatus 처리 상태 필터 — NEW | PROCESSING | COMPLETED | FAILED (반복 가능)
     * @param transactionId   거래 ID 완전 일치 필터
     * @param custNo          row_data->>'cust_no' 단축 필터
     * @param jsonFilters     JSONB 확장 필터 — key:op:value (반복 가능)
     * @param page            0-based 페이지 번호 (기본값: 0)
     * @param size            페이지 크기 (기본값: 20, 최대: 100)
     */
    @GetMapping("/{dataSourceId}/mapped-storages")
    @Operation(
            summary = "매핑 결과 목록 조회",
            description = "mapped_storages 조회. landing_records JOIN으로 data_source_id 필터링. JSONB 확장 필터(jsonFilters) 지원.")
    public MappedStorageDto.PageResponse getMappedStorages(
            @Parameter(description = "데이터소스 ID") @PathVariable String dataSourceId,
            @Parameter(description = "조회 시작 시각 (ISO datetime)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "조회 종료 시각 (ISO datetime)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "처리 상태 (반복 가능): NEW | PROCESSING | COMPLETED | FAILED")
            @RequestParam(name = "processingStatus", required = false) List<String> processingStatus,
            @Parameter(description = "거래 ID (완전 일치)")
            @RequestParam(required = false) String transactionId,
            @Parameter(description = "cust_no 단축 필터 (row_data->>'cust_no')")
            @RequestParam(required = false) String custNo,
            @Parameter(description = "JSONB 확장 필터 (반복 가능): key:op:value")
            @RequestParam(name = "jsonFilters", required = false) List<String> jsonFilters,
            @Parameter(description = "페이지 번호 (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 100)") @RequestParam(defaultValue = "20") int size) {

        return mappedStorageService.search(
                dataSourceId, startDate, endDate,
                processingStatus, transactionId, custNo, jsonFilters,
                page, size);
    }

    /**
     * [2026-04-22] 상세 모달 원본 비교 — mappedStorageId 기준 단건 + raw_payload 조회
     */
    @GetMapping("/mapped-storages/{mappedStorageId}/with-origin")
    @Operation(
            summary = "매핑 결과 상세 + 수집 원본 비교",
            description = "mapped_storage 단건 조회 + landing_records.raw_payload JOIN. 상세 모달 원본 비교 탭용.")
    public MappedStorageDto.DetailWithOrigin getMappedStorageWithOrigin(
            @Parameter(description = "매핑 스토리지 ID") @PathVariable Long mappedStorageId) {
        return mappedStorageService.findWithOrigin(mappedStorageId);
    }
}
