package com.itmasters.icon.api.datasource.application.service;

import com.itmasters.icon.api.datasource.adapter.in.web.JsonFilterParam;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.MappedStorageQueryRepository;
import com.itmasters.icon.api.datasource.dto.MappedStorageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [2026-04-22] 매핑 결과 조회 서비스 — TODO-002
 * GET /api/v1/data-sources/{dataSourceId}/mapped-storages
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MappedStorageService {

    private final MappedStorageQueryRepository queryRepository;

    /**
     * 데이터소스별 매핑 결과 페이지 조회
     * mapped_storages 에는 data_source_id 없음 → landing_records JOIN 내부 처리
     */
    public MappedStorageDto.PageResponse search(
            String dataSourceId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> statuses,
            String transactionId,
            String custNo,
            List<String> jsonFilterRaws,
            int page,
            int size) {

        LocalDateTime resolvedStart = startDate != null ? startDate : LocalDateTime.now().minusHours(24);
        LocalDateTime resolvedEnd   = endDate   != null ? endDate   : LocalDateTime.now();
        int resolvedSize  = Math.min(Math.max(1, size), 100);
        int resolvedPage  = Math.max(0, page);

        List<JsonFilterParam> jsonFilters = JsonFilterParam.parseAll(jsonFilterRaws);
        if (jsonFilterRaws != null && jsonFilters.size() != jsonFilterRaws.stream()
                .filter(s -> s != null && !s.isBlank()).count()) {
            log.warn("[mapped-storages] 일부 jsonFilters 파싱 실패 — dataSourceId={}", dataSourceId);
        }

        log.debug("[mapped-storages] 조회 — dataSourceId={}, start={}, end={}, statuses={}, page={}, size={}",
                dataSourceId, resolvedStart, resolvedEnd, statuses, resolvedPage, resolvedSize);

        return queryRepository.search(
                dataSourceId, resolvedStart, resolvedEnd,
                statuses, transactionId, custNo, jsonFilters,
                resolvedPage, resolvedSize);
    }

    /**
     * [2026-04-22] 상세 모달 원본 비교 — mappedStorageId 기준 단건 + raw_payload 조회
     */
    public MappedStorageDto.DetailWithOrigin findWithOrigin(Long mappedStorageId) {
        return queryRepository.findWithOrigin(mappedStorageId);
    }
}
