package com.itmasters.icon.api.dashboard.application;

import com.itmasters.icon.api.dashboard.adapter.out.persistence.DashboardQueryRepository;
import com.itmasters.icon.api.dashboard.dto.DashboardStatsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 통계 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final DashboardQueryRepository dashboardQueryRepository;

    /**
     * 금일 대시보드 통계 조회
     */
    public DashboardStatsDto getTodayStats() {
        LocalDate today = LocalDate.now();

        log.info("Fetching dashboard stats for date: {}", today);

        // 금일 전체거래 카운트
        long totalTransactions = dashboardQueryRepository.countTodayTransactions(today);
        log.debug("Total transactions: {}", totalTransactions);

        // 금일 탐지거래 카운트
        long detectedTransactions = dashboardQueryRepository.countTodayDetections(today);
        log.debug("Detected transactions: {}", detectedTransactions);

        // 금일 위험수준별 탐지 카운트
        Map<String, Long> riskLevelCounts = dashboardQueryRepository.countTodayDetectionsByRiskLevel(today);
        log.debug("Risk level counts: {}", riskLevelCounts);

        // 금일 위험수준별 조치 상태별 카운트
        Map<String, DashboardStatsDto.ActionStatusStats> actionStatsMap = dashboardQueryRepository.countTodayActionsByRiskLevel(today);
        log.debug("Action stats: {}", actionStatsMap);

        // 금일 탐지영역별 탐지 통계
        List<DashboardStatsDto.DetectionAreaStats> detectionAreaStats = dashboardQueryRepository.findDetectionAreaStats(today);
        log.debug("Detection area stats count: {}", detectionAreaStats.size());

        // 통계 계산 및 반환
        return DashboardStatsDto.calculate(totalTransactions, detectedTransactions, riskLevelCounts, actionStatsMap, detectionAreaStats);
    }

    /**
     * 금일 시간대별 탐지 통계 조회
     */
    public List<DashboardStatsDto.HourlyStats> getHourlyStats() {
        LocalDate today = LocalDate.now();
        log.info("Fetching hourly stats for date: {}", today);
        return dashboardQueryRepository.findHourlyStats(today);
    }
}
