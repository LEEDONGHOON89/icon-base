package com.itmasters.icon.api.datasource.adapter.out.persistence.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.api.datasource.adapter.in.web.JsonFilterParam;
import com.itmasters.icon.api.datasource.dto.MappedStorageDto;
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
 * [2026-04-22] mapped_storages JSONB 동적 필터 쿼리 리포지토리 — TODO-002
 * - mapped_storages 에는 data_source_id 없음 → landing_records JOIN 필수
 * - row_data JSONB 확장 필터 지원
 */
@Slf4j
@Repository
public class MappedStorageQueryRepository {

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper objectMapper;

    public MappedStorageQueryRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private static final TypeReference<Map<String, Object>> JSON_MAP_TYPE = new TypeReference<>() {};

    // ── 페이지 조회 ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MappedStorageDto.PageResponse search(
            String dataSourceId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> statuses,
            String transactionId,
            String custNo,
            List<JsonFilterParam> jsonFilters,
            int page,
            int size) {

        FilterContext ctx = buildFilter(dataSourceId, startDate, endDate, statuses,
                transactionId, custNo, jsonFilters);

        // 1. 통계 요약 쿼리
        String statsSql = "SELECT COUNT(*) AS total, " +
                "SUM(CASE WHEN ms.processing_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_count, " +
                "SUM(CASE WHEN ms.processing_status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count, " +
                "MAX(ms.reg_dt) AS last_reg_dt " +
                "FROM mapped_storages ms " +
                "JOIN landing_records lr ON ms.landing_record_id = lr.landing_record_id " +
                ctx.where;

        Query statsQ = em.createNativeQuery(statsSql);
        applyParams(statsQ, ctx);
        Object[] stats = (Object[]) statsQ.getSingleResult();

        long total     = toLong(stats[0]);
        long completed = toLong(stats[1]);
        long failed    = toLong(stats[2]);
        LocalDateTime lastRegDt = toLocalDateTime(stats[3]);

        MappedStorageDto.Summary summary = MappedStorageDto.Summary.builder()
                .total(total)
                .completedCount(completed)
                .failedCount(failed)
                .lastRegDt(lastRegDt)
                .build();

        // 2. 데이터 조회 (페이지네이션)
        // [2026-04-22] ::text → CAST(... AS text) — Hibernate가 ::를 named param으로 파싱하는 버그 방지
        String dataSql = "SELECT ms.mapped_storage_id, ms.landing_record_id, ms.exec_ds_mp_id, " +
                "lr.data_source_id, ms.transaction_id, ms.row_index, " +
                "CAST(ms.row_data AS text), ms.reg_dt, ms.processing_status, ms.error_message " +
                "FROM mapped_storages ms " +
                "JOIN landing_records lr ON ms.landing_record_id = lr.landing_record_id " +
                ctx.where +
                " ORDER BY ms.mapped_storage_id DESC " +
                "LIMIT :limit OFFSET :offset";

        Query dataQ = em.createNativeQuery(dataSql);
        applyParams(dataQ, ctx);
        dataQ.setParameter("limit",  size);
        dataQ.setParameter("offset", (long) page * size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQ.getResultList();
        List<MappedStorageDto.Item> items = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            items.add(mapItem(row));
        }

        return MappedStorageDto.PageResponse.builder()
                .data(items)
                .total(total)
                .page(page)
                .size(size)
                .summary(summary)
                .build();
    }

    /**
     * [2026-04-22] 상세 모달 원본 비교 — landing_record 의 raw_payload 조회
     */
    @Transactional(readOnly = true)
    public MappedStorageDto.DetailWithOrigin findWithOrigin(Long mappedStorageId) {
        // [2026-04-22] ::text → CAST(... AS text) — Hibernate named param 파싱 충돌 방지
        String sql = "SELECT ms.mapped_storage_id, ms.landing_record_id, ms.exec_ds_mp_id, " +
                "lr.data_source_id, ms.transaction_id, ms.row_index, " +
                "CAST(ms.row_data AS text), ms.reg_dt, ms.processing_status, ms.error_message, " +
                "CAST(lr.raw_payload AS text) AS raw_payload " +
                "FROM mapped_storages ms " +
                "JOIN landing_records lr ON ms.landing_record_id = lr.landing_record_id " +
                "WHERE ms.mapped_storage_id = :id";

        Query q = em.createNativeQuery(sql);
        q.setParameter("id", mappedStorageId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        if (rows.isEmpty()) return null;

        Object[] row = rows.get(0);
        MappedStorageDto.Item item = mapItem(row);
        Map<String, Object> rawPayload = parseJson((String) row[10]);

        return MappedStorageDto.DetailWithOrigin.builder()
                .mappedStorage(item)
                .originLandingRecordId(toLong(row[1]))
                .rawPayload(rawPayload)
                .build();
    }

    // ── WHERE 조건 빌더 ──────────────────────────────────────────────────────────

    private FilterContext buildFilter(
            String dataSourceId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> statuses,
            String transactionId,
            String custNo,
            List<JsonFilterParam> jsonFilters) {

        StringBuilder where = new StringBuilder(
                "WHERE lr.data_source_id = :dataSourceId " +
                "AND ms.reg_dt >= :startDate " +
                "AND ms.reg_dt <= :endDate");

        List<String> validStatuses = new ArrayList<>();
        if (statuses != null) {
            for (String s : statuses) {
                if (s != null && !s.isBlank()) validStatuses.add(s.toUpperCase());
            }
        }
        if (!validStatuses.isEmpty()) {
            List<String> paramNames = new ArrayList<>();
            for (int i = 0; i < validStatuses.size(); i++) paramNames.add(":st" + i);
            where.append(" AND ms.processing_status IN (").append(String.join(",", paramNames)).append(")");
        }

        if (transactionId != null && !transactionId.isBlank()) {
            where.append(" AND ms.transaction_id = :transactionId");
        }

        // custNo: row_data->>'cust_no' 단축 필터
        if (custNo != null && !custNo.isBlank()) {
            where.append(" AND ms.row_data->>'cust_no' = :custNo");
        }

        // [2026-04-22] JSONB 확장 필터
        for (int i = 0; i < jsonFilters.size(); i++) {
            JsonFilterParam f = jsonFilters.get(i);
            switch (f.op()) {
                case eq   -> where.append(" AND ms.row_data->>'").append(f.key()).append("' = :jv").append(i);
                case like -> where.append(" AND ms.row_data->>'").append(f.key()).append("' ILIKE :jv").append(i);
                case neq  -> where.append(" AND ms.row_data->>'").append(f.key()).append("' != :jv").append(i);
            }
        }

        return new FilterContext(where.toString(), dataSourceId, startDate, endDate,
                validStatuses, transactionId, custNo, jsonFilters);
    }

    private void applyParams(Query q, FilterContext ctx) {
        q.setParameter("dataSourceId", ctx.dataSourceId);
        q.setParameter("startDate",    ctx.startDate);
        q.setParameter("endDate",      ctx.endDate);
        for (int i = 0; i < ctx.statuses.size(); i++) q.setParameter("st" + i, ctx.statuses.get(i));
        if (ctx.transactionId != null && !ctx.transactionId.isBlank())
            q.setParameter("transactionId", ctx.transactionId);
        if (ctx.custNo != null && !ctx.custNo.isBlank())
            q.setParameter("custNo", ctx.custNo);
        for (int i = 0; i < ctx.jsonFilters.size(); i++) {
            JsonFilterParam f = ctx.jsonFilters.get(i);
            String val = f.op() == JsonFilterParam.Op.like ? "%" + f.value() + "%" : f.value();
            q.setParameter("jv" + i, val);
        }
    }

    // ── 행 매핑 ──────────────────────────────────────────────────────────────────

    private MappedStorageDto.Item mapItem(Object[] row) {
        return MappedStorageDto.Item.builder()
                .mappedStorageId(toLong(row[0]))
                .landingRecordId(toLong(row[1]))
                .execDsMpId(toLong(row[2]))
                .dataSourceId((String) row[3])
                .transactionId((String) row[4])
                .rowIndex(row[5] != null ? ((Number) row[5]).intValue() : null)
                .rowData(parseJson((String) row[6]))
                .regDt(toLocalDateTime(row[7]))
                .processingStatus((String) row[8])
                .errorMessage((String) row[9])
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

    private record FilterContext(
            String where,
            String dataSourceId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> statuses,
            String transactionId,
            String custNo,
            List<JsonFilterParam> jsonFilters) {}
}
