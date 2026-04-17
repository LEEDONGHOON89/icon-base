package com.itmasters.icon.api.dashboard.adapter.in.web;

import com.itmasters.icon.api.dashboard.application.DashboardService;
import com.itmasters.icon.api.dashboard.dto.DashboardStatsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 대시보드 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "대시보드 통계 API")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 금일 대시보드 통계 조회
     */
    @GetMapping("/stats")
    @Operation(summary = "금일 대시보드 통계 조회", description = "금일 전체거래, 탐지거래, 탐지율을 조회합니다.")
    public DashboardStatsDto getTodayStats() {
        log.info("GET /api/v1/dashboard/stats - 금일 통계 조회");
        return dashboardService.getTodayStats();
    }

    /**
     * 금일 시간대별 탐지 통계 조회
     */
    @GetMapping("/hourly")
    @Operation(summary = "시간대별 탐지 통계 조회", description = "금일 0-23시 시간대별 거래/탐지 건수를 조회합니다.")
    public List<DashboardStatsDto.HourlyStats> getHourlyStats() {
        log.info("GET /api/v1/dashboard/hourly - 시간대별 통계 조회");
        return dashboardService.getHourlyStats();
    }
}
