package com.itmasters.icon.api.analytics.adapter.in.web;

import com.itmasters.icon.api.analytics.application.AnalyticsService;
import com.itmasters.icon.api.analytics.application.ExecutionAnalyticsService;
import com.itmasters.icon.api.analytics.dto.DetectRuleDto;
import com.itmasters.icon.api.analytics.dto.DetectScenarioDto;
import com.itmasters.icon.api.analytics.dto.EntityGraphDto;
import com.itmasters.icon.api.analytics.dto.EntityHistoryDto;
import com.itmasters.icon.api.analytics.dto.EntityProfileDto;
import com.itmasters.icon.api.analytics.dto.EventStreamDto;
import com.itmasters.icon.api.analytics.dto.ExecutionDetailDto;
import com.itmasters.icon.api.analytics.dto.ExecutionPageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ExecutionAnalyticsService executionAnalyticsService;

    @GetMapping("/executions")
    public ExecutionPageDto getExecutions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 20 : size;
        return executionAnalyticsService.getExecutions(safePage - 1, safeSize);
    }

    @GetMapping("/executions/{landingRecordId}")
    public ExecutionDetailDto getExecutionDetail(@PathVariable Long landingRecordId) {
        return executionAnalyticsService.getExecutionDetail(landingRecordId);
    }

    @GetMapping("/event-stream")
    public List<EventStreamDto> getEventStream(
            @RequestParam String groupKey,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false, defaultValue = "200") int limit
    ) {
        return analyticsService.getEventStream(groupKey, start, end, limit);
    }

    @GetMapping("/aggregates")
    public List<DetectRuleDto> getAggregates(
            @RequestParam(required = false) String groupKey,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) Integer limit
    ) {
        // 우선순위: date 파라미터가 있으면 해당 일자 00:00:00 ~ 23:59:59로 변환, 없으면 datetime 사용
        LocalDateTime s = start;
        LocalDateTime e = end;
        if (startDate != null) {
            s = startDate.atStartOfDay();
        }
        if (endDate != null) {
            e = endDate.atTime(23, 59, 59);
        }
        return analyticsService.getAggregates(groupKey, s, e, limit);
    }

    @GetMapping("/scenarios")
    public List<DetectScenarioDto> getScenarios(
            @RequestParam(required = false) String groupKey,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) Integer limit
    ) {
        LocalDateTime s = start;
        LocalDateTime e = end;
        if (startDate != null) s = startDate.atStartOfDay();
        if (endDate != null) e = endDate.atTime(23, 59, 59);
        return analyticsService.getScenarios(groupKey, s, e, limit);
    }

    @GetMapping("/scenarios/detail")
    public AnalyticsService.ScenarioDetailDto getScenarioDetail(
            @RequestParam String groupKey,
            @RequestParam String scenarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime detectedAt
    ) {
        return analyticsService.getScenarioDetail(groupKey, scenarioId, detectedAt);
    }

    @GetMapping("/aggregates/detail")
    public AnalyticsService.AggregateDetailDto getAggregateDetail(
            @RequestParam String groupKey,
            @RequestParam String ruleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime anchor) {
        return analyticsService.getAggregateDetail(groupKey, ruleId, anchor);
    }

    /**
     * 엔티티 프로필 조회 (entity_attributes + event_stream)
     *
     * @param groupKey 조회할 그룹 키 (엔티티 ID)
     * @param limit 이벤트 스트림 조회 제한 (기본 100건)
     * @return 엔티티 프로필 정보
     */
    @GetMapping("/entities/{groupKey}")
    public EntityProfileDto getEntityProfile(
            @PathVariable String groupKey,
            @RequestParam(required = false, defaultValue = "100") Integer limit
    ) {
        return analyticsService.getEntityProfile(groupKey, limit);
    }

    /**
     * 엔티티 관계 그래프 조회 (React 그래프 라이브러리용)
     *
     * @param entityType 엔티티 타입 (예: "ACCOUNT", "CUSTOMER")
     * @param entityId   엔티티 ID
     * @param depth      탐색 깊이 (1: 직접 연결, 2: 2단계 연결, 기본: 1)
     * @param limit      각 노드별 최대 연결 개수 (기본: 10)
     * @return 노드와 엣지로 구성된 그래프 데이터
     */
    @GetMapping("/entities/{entityType}/{entityId}/graph")
    public EntityGraphDto getEntityGraph(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(required = false, defaultValue = "1") Integer depth,
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        return analyticsService.getEntityGraph(entityType, entityId, depth, limit);
    }

    /**
     * 엔티티 행적 조회
     *
     * entity_id가 group_key에 포함된 모든 탐지 이력과 활동 로그를 조회합니다.
     * 예: EMP004가 포함된 group_key(EMP004|330-444-555666)의 모든 탐지 이력
     *
     * @param entityId        조회할 엔티티 ID (예: EMP004)
     * @param detectionLimit  탐지 이력 조회 제한 (기본 50건)
     * @param activityLimit   활동 로그 조회 제한 (기본 100건)
     * @return 엔티티 행적 정보 (통계, 탐지이력, 활동로그)
     */
    @GetMapping("/entity-history/{entityId}")
    public EntityHistoryDto getEntityHistory(
            @PathVariable String entityId,
            @RequestParam(required = false, defaultValue = "50") Integer detectionLimit,
            @RequestParam(required = false, defaultValue = "100") Integer activityLimit
    ) {
        return analyticsService.getEntityHistory(entityId, detectionLimit, activityLimit);
    }
}
