package com.itmasters.icon.api.dashboard.adapter.out.persistence;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.QApiDetectActionEntity;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.QApiDetectScenarioEntity;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.QApiMappedDataStorageEntity;
import com.itmasters.icon.api.dashboard.dto.DashboardStatsDto;
import com.itmasters.icon.api.scenario.adapter.persistence.entity.QScenarioEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QDetectionAreaEntity;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 대시보드 통계 QueryDSL Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class DashboardQueryRepositoryImpl implements DashboardQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 내부 DTO: RiskLevel별 카운트
     */
    @Getter
    @AllArgsConstructor
    public static class RiskLevelCount {
        private String riskLevel;
        private Long count;
    }

    /**
     * 내부 DTO: RiskLevel + ActionStatus별 카운트
     */
    @Getter
    @AllArgsConstructor
    public static class RiskLevelActionCount {
        private String riskLevel;
        private String actionStatus;
        private Long count;
    }

    @Override
    public long countTodayTransactions(LocalDate date) {
        QApiMappedDataStorageEntity m = QApiMappedDataStorageEntity.apiMappedDataStorageEntity;

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        Long count = queryFactory
                .select(m.mappedStorageId.count())
                .from(m)
                .where(m.regDt.goe(startOfDay)
                        .and(m.regDt.lt(endOfDay)))
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public long countTodayDetections(LocalDate date) {
        QApiDetectScenarioEntity d = QApiDetectScenarioEntity.apiDetectScenarioEntity;

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        Long count = queryFactory
                .select(d.detectScenarioId.count())
                .from(d)
                .where(d.detectedDt.goe(startOfDay)
                        .and(d.detectedDt.lt(endOfDay)))
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public Map<String, Long> countTodayDetectionsByRiskLevel(LocalDate date) {
        QApiDetectScenarioEntity d = QApiDetectScenarioEntity.apiDetectScenarioEntity;
        QScenarioEntity s = QScenarioEntity.scenarioEntity;

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        // QueryDSL Projections 사용 - 타입 안전하게 DTO로 변환
        List<RiskLevelCount> counts = queryFactory
                .select(Projections.constructor(RiskLevelCount.class,
                        s.riskLevelId,
                        d.detectScenarioId.count()))
                .from(d)
                .join(s).on(d.scenarioId.eq(s.scenarioId))
                .where(d.detectedDt.goe(startOfDay)
                        .and(d.detectedDt.lt(endOfDay)))
                .groupBy(s.riskLevelId)
                .fetch();

        // 기본값 맵 생성
        Map<String, Long> result = new HashMap<>();
        result.put("BLOCK", 0L);
        result.put("REVIEW", 0L);
        result.put("INTENSIVE", 0L);
        result.put("MONITOR", 0L);

        // 실제 조회 결과로 덮어쓰기
        counts.forEach(count ->
            result.put(count.getRiskLevel(), count.getCount())
        );

        return result;
    }

    @Override
    public Map<String, DashboardStatsDto.ActionStatusStats> countTodayActionsByRiskLevel(LocalDate date) {
        QApiDetectActionEntity a = QApiDetectActionEntity.apiDetectActionEntity;

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        // QueryDSL Projections 사용 - risk_level + action_status별 카운트
        List<RiskLevelActionCount> counts = queryFactory
                .select(Projections.constructor(RiskLevelActionCount.class,
                        a.riskLevel,
                        a.actionStatus,
                        a.detectActionId.count()))
                .from(a)
                .where(a.requestedAt.goe(startOfDay)
                        .and(a.requestedAt.lt(endOfDay)))
                .groupBy(a.riskLevel, a.actionStatus)
                .fetch();

        // Map<RiskLevel, ActionStatusStats> 구조로 변환
        Map<String, DashboardStatsDto.ActionStatusStats> result = new HashMap<>();
        result.put("BLOCK", DashboardStatsDto.ActionStatusStats.empty());
        result.put("REVIEW", DashboardStatsDto.ActionStatusStats.empty());
        result.put("INTENSIVE", DashboardStatsDto.ActionStatusStats.empty());
        result.put("MONITOR", DashboardStatsDto.ActionStatusStats.empty());

        // risk_level별로 그룹화
        Map<String, List<RiskLevelActionCount>> groupedByRiskLevel = counts.stream()
                .collect(Collectors.groupingBy(RiskLevelActionCount::getRiskLevel));

        // 각 risk_level별로 ActionStatusStats 생성
        for (Map.Entry<String, List<RiskLevelActionCount>> entry : groupedByRiskLevel.entrySet()) {
            String riskLevel = entry.getKey();
            List<RiskLevelActionCount> statusCounts = entry.getValue();

            Long pending = 0L;
            Long approved = 0L;
            Long rejected = 0L;
            Long completed = 0L;

            for (RiskLevelActionCount statusCount : statusCounts) {
                switch (statusCount.getActionStatus()) {
                    case "PENDING" -> pending = statusCount.getCount();
                    case "APPROVED" -> approved = statusCount.getCount();
                    case "REJECTED" -> rejected = statusCount.getCount();
                    case "COMPLETED" -> completed = statusCount.getCount();
                }
            }

            DashboardStatsDto.ActionStatusStats stats = DashboardStatsDto.ActionStatusStats.builder()
                    .pending(pending)
                    .approved(approved)
                    .rejected(rejected)
                    .completed(completed)
                    .build();

            result.put(riskLevel, stats);
        }

        return result;
    }

    @Override
    public List<DashboardStatsDto.DetectionAreaStats> findDetectionAreaStats(LocalDate date) {
        QApiDetectScenarioEntity d = QApiDetectScenarioEntity.apiDetectScenarioEntity;
        QScenarioEntity s = QScenarioEntity.scenarioEntity;
        QDetectionAreaEntity da = QDetectionAreaEntity.detectionAreaEntity;

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<DashboardStatsDto.DetectionAreaStats> result = new ArrayList<>();

        // 탐지영역별 탐지 카운트 조회
        List<Tuple> areaCounts = queryFactory
                .select(da.detectionAreaId, da.areaName, da.color, d.detectScenarioId.count())
                .from(d)
                .join(s).on(d.scenarioId.eq(s.scenarioId))
                .join(da).on(s.detectionAreaId.eq(da.detectionAreaId))
                .where(d.detectedDt.goe(startOfDay)
                        .and(d.detectedDt.lt(endOfDay)))
                .groupBy(da.detectionAreaId, da.areaName, da.color)
                .orderBy(d.detectScenarioId.count().desc())
                .fetch();

        for (Tuple row : areaCounts) {
            String detectionAreaId = row.get(da.detectionAreaId);
            String detectionAreaName = row.get(da.areaName);
            String colorCode = row.get(da.color);
            Long detectionCount = row.get(d.detectScenarioId.count());

            // 해당 탐지영역의 상위 5개 시나리오 조회
            List<DashboardStatsDto.ScenarioSummary> topScenarios = findTopScenariosByDetectionArea(date, detectionAreaId, 5);

            DashboardStatsDto.DetectionAreaStats areaStats = DashboardStatsDto.DetectionAreaStats.builder()
                    .detectionAreaId(detectionAreaId)
                    .detectionAreaName(detectionAreaName)
                    .colorCode(colorCode)
                    .detectionCount(detectionCount != null ? detectionCount : 0L)
                    .topScenarios(topScenarios)
                    .build();

            result.add(areaStats);
        }

        return result;
    }

    /**
     * 특정 탐지영역의 상위 N개 탐지 시나리오 조회
     */
    private List<DashboardStatsDto.ScenarioSummary> findTopScenariosByDetectionArea(LocalDate date, String detectionAreaId, int limit) {
        QApiDetectScenarioEntity d = QApiDetectScenarioEntity.apiDetectScenarioEntity;
        QScenarioEntity s = QScenarioEntity.scenarioEntity;

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<DashboardStatsDto.ScenarioSummary> result = new ArrayList<>();

        List<Tuple> scenarios = queryFactory
                .select(s.scenarioId, s.scenarioName, s.riskLevelId, d.detectScenarioId.count())
                .from(d)
                .join(s).on(d.scenarioId.eq(s.scenarioId))
                .where(d.detectedDt.goe(startOfDay)
                        .and(d.detectedDt.lt(endOfDay))
                        .and(s.detectionAreaId.eq(detectionAreaId)))
                .groupBy(s.scenarioId, s.scenarioName, s.riskLevelId)
                .orderBy(d.detectScenarioId.count().desc())
                .limit(limit)
                .fetch();

        for (Tuple row : scenarios) {
            String scenarioId = row.get(s.scenarioId);
            String scenarioName = row.get(s.scenarioName);
            String riskLevelId = row.get(s.riskLevelId);
            Long count = row.get(d.detectScenarioId.count());

            DashboardStatsDto.ScenarioSummary summary = DashboardStatsDto.ScenarioSummary.builder()
                    .scenarioId(scenarioId)
                    .scenarioName(scenarioName)
                    .riskLevelId(riskLevelId)
                    .count(count != null ? count : 0L)
                    .build();

            result.add(summary);
        }

        return result;
    }

    @Override
    public List<DashboardStatsDto.HourlyStats> findHourlyStats(LocalDate date) {
        QApiMappedDataStorageEntity m = QApiMappedDataStorageEntity.apiMappedDataStorageEntity;
        QApiDetectScenarioEntity d = QApiDetectScenarioEntity.apiDetectScenarioEntity;

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        // 시간대별 거래 카운트
        Map<Integer, Long> transactionsByHour = new HashMap<>();
        List<Tuple> transactionCounts = queryFactory
                .select(m.regDt.hour(), m.mappedStorageId.count())
                .from(m)
                .where(m.regDt.goe(startOfDay).and(m.regDt.lt(endOfDay)))
                .groupBy(m.regDt.hour())
                .fetch();

        for (Tuple row : transactionCounts) {
            Integer hour = row.get(m.regDt.hour());
            Long count = row.get(m.mappedStorageId.count());
            if (hour != null) {
                transactionsByHour.put(hour, count != null ? count : 0L);
            }
        }

        // 시간대별 탐지 카운트
        Map<Integer, Long> detectionsByHour = new HashMap<>();
        List<Tuple> detectionCounts = queryFactory
                .select(d.detectedDt.hour(), d.detectScenarioId.count())
                .from(d)
                .where(d.detectedDt.goe(startOfDay).and(d.detectedDt.lt(endOfDay)))
                .groupBy(d.detectedDt.hour())
                .fetch();

        for (Tuple row : detectionCounts) {
            Integer hour = row.get(d.detectedDt.hour());
            Long count = row.get(d.detectScenarioId.count());
            if (hour != null) {
                detectionsByHour.put(hour, count != null ? count : 0L);
            }
        }

        // 0-23시까지 모든 시간대 데이터 생성
        List<DashboardStatsDto.HourlyStats> result = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            result.add(DashboardStatsDto.HourlyStats.builder()
                    .hour(hour)
                    .transactionCount(transactionsByHour.getOrDefault(hour, 0L))
                    .detectionCount(detectionsByHour.getOrDefault(hour, 0L))
                    .build());
        }

        return result;
    }
}
