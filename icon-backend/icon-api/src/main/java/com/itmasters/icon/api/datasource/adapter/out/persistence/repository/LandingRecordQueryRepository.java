package com.itmasters.icon.api.datasource.adapter.out.persistence.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.api.datasource.adapter.in.web.JsonFilterParam;
import com.itmasters.icon.api.datasource.dto.LandingRecordDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * [2026-04-22] landing_records JSONB 동적 필터 쿼리 리포지토리 — TODO-001
 * - data_source_id + 기간 + 상태 + JSONB 확장 필터
 * - 통계 요약 쿼리(CASE SUM) 포함
 */
@Slf4j
@Repository
public class LandingRecordQueryRepository {

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper objectMapper;

    public LandingRecordQueryRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private static final TypeReference<Map<String, Object>> JSON_MAP_TYPE = new TypeReference<>() {};

    // ── 페이지 조회 ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LandingRecordDto.PageResponse search(
            String dataSourceId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> statuses,
            String custNo,
            List<JsonFilterParam> jsonFilters,
            int page,
            int size) {

        // WHERE 조건 + 파라미터 컨테이너
        FilterContext ctx = buildFilter(dataSourceId, startDate, endDate, statuses, custNo, jsonFilters);

        // 1. 통계 요약 쿼리 (total + 상태별 카운트 + 최신 시각)
        String statsSql = "SELECT COUNT(*) AS total, " +
                "SUM(CASE WHEN lr.ingestion_status = 'TRANSFORMED' THEN 1 ELSE 0 END) AS transformed_count, " +
                "SUM(CASE WHEN lr.ingestion_status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count, " +
                "MAX(lr.extracted_at) AS last_extracted_at " +
                "FROM landing_records lr " + ctx.where;

        Query statsQ = em.createNativeQuery(statsSql);
        applyParams(statsQ, ctx);
        Object[] stats = (Object[]) statsQ.getSingleResult();

        long total        = toLong(stats[0]);
        long transformed  = toLong(stats[1]);
        long failed       = toLong(stats[2]);
        LocalDateTime lastExtractedAt = toLocalDateTime(stats[3]);

        LandingRecordDto.Summary summary = LandingRecordDto.Summary.builder()
                .total(total)
                .transformedCount(transformed)
                .failedCount(failed)
                .lastExtractedAt(lastExtractedAt)
                .build();

        // 2. 데이터 조회 (페이지네이션)
        // [2026-04-22] ::text → CAST(... AS text) — Hibernate가 ::를 named param으로 파싱하는 버그 방지
        String dataSql = "SELECT lr.landing_record_id, lr.exec_ds_mp_id, lr.data_source_id, " +
                "lr.source_type, CAST(lr.raw_payload AS text), lr.row_index, " +
                "lr.extracted_at, lr.ingestion_status, lr.ingestion_message " +
                "FROM landing_records lr " + ctx.where +
                " ORDER BY lr.landing_record_id DESC " +
                "LIMIT :limit OFFSET :offset";

        Query dataQ = em.createNativeQuery(dataSql);
        applyParams(dataQ, ctx);
        dataQ.setParameter("limit",  size);
        dataQ.setParameter("offset", (long) page * size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQ.getResultList();
        List<LandingRecordDto.Item> items = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            items.add(mapItem(row));
        }

        return LandingRecordDto.PageResponse.builder()
                .data(items)
                .total(total)
                .page(page)
                .size(size)
                .summary(summary)
                .build();
    }

    // ── WHERE 조건 빌더 ──────────────────────────────────────────────────────────

    private FilterContext buildFilter(
            String dataSourceId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> statuses,
            String custNo,
            List<JsonFilterParam> jsonFilters) {

        StringBuilder where = new StringBuilder(
                "WHERE lr.data_source_id = :dataSourceId " +
                "AND lr.extracted_at >= :startDate " +
                "AND lr.extracted_at <= :endDate");

        List<String> validStatuses = new ArrayList<>();
        if (statuses != null) {
            for (String s : statuses) {
                if (s != null && !s.isBlank()) validStatuses.add(s.toUpperCase());
            }
        }
        if (!validStatuses.isEmpty()) {
            // [2026-04-22] 상태 필터 — 동적 IN 절 (named param 리스트)
            List<String> paramNames = new ArrayList<>();
            for (int i = 0; i < validStatuses.size(); i++) paramNames.add(":st" + i);
            where.append(" AND lr.ingestion_status IN (").append(String.join(",", paramNames)).append(")");
        }

        // custNo: raw_payload->>'cust_no' 단축 필터
        if (custNo != null && !custNo.isBlank()) {
            where.append(" AND lr.raw_payload->>'cust_no' = :custNo");
        }

        // [2026-04-22] JSONB 확장 필터 — key 는 VALID_KEY 정규식 검증 통과한 값만 사용
        for (int i = 0; i < jsonFilters.size(); i++) {
            JsonFilterParam f = jsonFilters.get(i);
            switch (f.op()) {
                case eq   -> where.append(" AND lr.raw_payload->>'").append(f.key()).append("' = :jv").append(i);
                case like -> where.append(" AND lr.raw_payload->>'").append(f.key()).append("' ILIKE :jv").append(i);
                case neq  -> where.append(" AND lr.raw_payload->>'").append(f.key()).append("' != :jv").append(i);
            }
        }

        return new FilterContext(where.toString(), dataSourceId, startDate, endDate,
                validStatuses, custNo, jsonFilters);
    }

    private void applyParams(Query q, FilterContext ctx) {
        q.setParameter("dataSourceId", ctx.dataSourceId);
        q.setParameter("startDate",    ctx.startDate);
        q.setParameter("endDate",      ctx.endDate);
        for (int i = 0; i < ctx.statuses.size(); i++) q.setParameter("st" + i, ctx.statuses.get(i));
        if (ctx.custNo != null && !ctx.custNo.isBlank()) q.setParameter("custNo", ctx.custNo);
        for (int i = 0; i < ctx.jsonFilters.size(); i++) {
            JsonFilterParam f = ctx.jsonFilters.get(i);
            String val = f.op() == JsonFilterParam.Op.like ? "%" + f.value() + "%" : f.value();
            q.setParameter("jv" + i, val);
        }
    }

    // ── 행 매핑 ──────────────────────────────────────────────────────────────────

    private LandingRecordDto.Item mapItem(Object[] row) {
        return LandingRecordDto.Item.builder()
                .landingRecordId(toLong(row[0]))
                .execDsMpId(toLong(row[1]))
                .dataSourceId((String) row[2])
                .sourceType((String) row[3])
                .rawPayload(parseJson((String) row[4]))
                .rowIndex(row[5] != null ? ((Number) row[5]).intValue() : null)
                .extractedAt(toLocalDateTime(row[6]))
                .ingestionStatus((String) row[7])
                .ingestionMessage((String) row[8])
                .build();
    }

    // ── 유틸 ──────────────────────────────────────────────────────────────────────

    private Map<String, Object> parseJson(String json) {
        if (json == null) return Map.of();
        try {
            return objectMapper.readValue(json, JSON_MAP_TYPE);
        } catch (Exception e) {
            log.warn("JSON 파싱 실패: {}", e.getMessage());
            return Map.of();
        }
    }

    private static long toLong(Object val) {
        if (val == null) return 0L;
        return ((Number) val).longValue();
    }

    private static LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof Timestamp ts) return ts.toLocalDateTime();
        if (val instanceof LocalDateTime ldt) return ldt;
        return null;
    }

    // ── 내부 파라미터 컨테이너 ─────────────────────────────────────────────────────

    private record FilterContext(
            String where,
            String dataSourceId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> statuses,
            String custNo,
            List<JsonFilterParam> jsonFilters) {}
}
