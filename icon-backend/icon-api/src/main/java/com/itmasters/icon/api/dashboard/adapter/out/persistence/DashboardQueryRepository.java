package com.itmasters.icon.api.dashboard.adapter.out.persistence;

import com.itmasters.icon.api.dashboard.dto.DashboardStatsDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 통계 QueryDSL Repository
 */
public interface DashboardQueryRepository {

    /**
     * 금일 전체거래 카운트 (mapped_storages)
     */
    long countTodayTransactions(LocalDate date);

    /**
     * 금일 탐지거래 카운트 (detect_scenarios)
     */
    long countTodayDetections(LocalDate date);

    /**
     * 금일 위험수준별 탐지 카운트
     */
    Map<String, Long> countTodayDetectionsByRiskLevel(LocalDate date);

    /**
     * 금일 위험수준별 조치 상태별 카운트 (detect_actions)
     */
    Map<String, DashboardStatsDto.ActionStatusStats> countTodayActionsByRiskLevel(LocalDate date);

    /**
     * 금일 탐지영역별 탐지 통계
     */
    List<DashboardStatsDto.DetectionAreaStats> findDetectionAreaStats(LocalDate date);

    /**
     * 금일 시간대별 탐지 통계 (0-23시)
     */
    List<DashboardStatsDto.HourlyStats> findHourlyStats(LocalDate date);
}
