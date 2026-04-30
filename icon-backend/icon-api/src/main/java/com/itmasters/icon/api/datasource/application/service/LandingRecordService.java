package com.itmasters.icon.api.datasource.application.service;

import com.itmasters.icon.api.datasource.adapter.in.web.JsonFilterParam;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.LandingRecordQueryRepository;
import com.itmasters.icon.api.datasource.dto.LandingRecordDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [2026-04-22] 수집 원본 조회 서비스 — TODO-001
 * GET /api/v1/data-sources/{dataSourceId}/landing-records
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LandingRecordService {

    private final LandingRecordQueryRepository queryRepository;

    /**
     * 데이터소스별 수집 원본 페이지 조회
     *
     * @param dataSourceId   데이터소스 ID
     * @param startDate      조회 시작 시각 (null 이면 24시간 전)
     * @param endDate        조회 종료 시각 (null 이면 현재)
     * @param statuses       수집 상태 필터 목록 (null/empty = 전체)
     * @param custNo         raw_payload->>'cust_no' 단축 필터
     * @param jsonFilterRaws key:op:value 형식 JSONB 확장 필터 목록
     * @param page           0-based 페이지 번호
     * @param size           페이지 크기 (최대 100 제한)
     */
    public LandingRecordDto.PageResponse search(
            String dataSourceId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> statuses,
            String custNo,
            List<String> jsonFilterRaws,
            int page,
            int size) {

        // [2026-04-22] 기본값 및 범위 제한 처리
        LocalDateTime resolvedStart = startDate != null ? startDate : LocalDateTime.now().minusHours(24);
        LocalDateTime resolvedEnd   = endDate   != null ? endDate   : LocalDateTime.now();
        int resolvedSize  = Math.min(Math.max(1, size), 100);
        int resolvedPage  = Math.max(0, page);

        List<JsonFilterParam> jsonFilters = JsonFilterParam.parseAll(jsonFilterRaws);
        if (jsonFilterRaws != null && jsonFilters.size() != jsonFilterRaws.stream()
                .filter(s -> s != null && !s.isBlank()).count()) {
            log.warn("[landing-records] 일부 jsonFilters 파싱 실패 — dataSourceId={}", dataSourceId);
        }

        log.debug("[landing-records] 조회 — dataSourceId={}, start={}, end={}, statuses={}, page={}, size={}",
                dataSourceId, resolvedStart, resolvedEnd, statuses, resolvedPage, resolvedSize);

        return queryRepository.search(
                dataSourceId, resolvedStart, resolvedEnd,
                statuses, custNo, jsonFilters,
                resolvedPage, resolvedSize);
    }
}
