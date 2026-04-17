package com.itmasters.icon.api.audit.adapter.in.web;

import com.itmasters.icon.api.audit.application.service.DetectionConfigAuditService;
import com.itmasters.icon.api.audit.domain.ConfigAuditTargetType;
import com.itmasters.icon.api.audit.dto.AuditSearchRequest;
import com.itmasters.icon.api.audit.dto.DetectionConfigAuditDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Detection Config Audit", description = "탐지 설정 변경 이력 API")
@RestController
@RequestMapping("/api/v1/audit/detection-config")
@RequiredArgsConstructor
public class DetectionConfigAuditController {

    private final DetectionConfigAuditService auditService;

    @Operation(summary = "최근 변경 이력 조회", description = "최근 50건의 변경 이력을 조회합니다")
    @GetMapping("/recent")
    public List<DetectionConfigAuditDto.Summary> getRecentAuditHistory() {
        return auditService.getRecentAuditHistory();
    }

    @Operation(summary = "특정 대상 변경 이력 조회", description = "특정 센서/룰/시나리오의 변경 이력을 조회합니다")
    @GetMapping("/{targetType}/{targetId}")
    public List<DetectionConfigAuditDto> getAuditHistory(
            @PathVariable ConfigAuditTargetType targetType,
            @PathVariable String targetId) {
        return auditService.getAuditHistory(targetType, targetId);
    }

    @Operation(summary = "특정 대상 변경 이력 조회 (페이징)", description = "특정 센서/룰/시나리오의 변경 이력을 페이징하여 조회합니다")
    @GetMapping("/{targetType}/{targetId}/page")
    public Page<DetectionConfigAuditDto.Summary> getAuditHistoryPaged(
            @PathVariable ConfigAuditTargetType targetType,
            @PathVariable String targetId,
            @PageableDefault(size = 20) Pageable pageable) {
        return auditService.getAuditHistory(targetType, targetId, pageable);
    }

    @Operation(summary = "사용자별 변경 이력 조회", description = "특정 사용자가 변경한 이력을 조회합니다")
    @GetMapping("/user/{userId}")
    public Page<DetectionConfigAuditDto.Summary> getAuditHistoryByUser(
            @PathVariable String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return auditService.getAuditHistoryByUser(userId, pageable);
    }

    @Operation(summary = "기간별 변경 이력 조회", description = "특정 기간의 변경 이력을 조회합니다")
    @GetMapping("/date-range")
    public Page<DetectionConfigAuditDto.Summary> getAuditHistoryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        return auditService.getAuditHistoryByDateRange(startDate, endDate, pageable);
    }

    @Operation(summary = "타입별 + 기간별 변경 이력 조회", description = "특정 타입의 특정 기간 변경 이력을 조회합니다")
    @GetMapping("/type/{targetType}/date-range")
    public Page<DetectionConfigAuditDto.Summary> getAuditHistoryByTypeAndDateRange(
            @PathVariable ConfigAuditTargetType targetType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        return auditService.getAuditHistoryByTypeAndDateRange(targetType, startDate, endDate, pageable);
    }

    @Operation(summary = "통합 검색", description = "모든 필터를 선택적으로 적용하여 변경 이력을 검색합니다")
    @GetMapping("/search")
    public Page<DetectionConfigAuditDto.Summary> searchAuditHistory(
            AuditSearchRequest request,
            @PageableDefault(size = 20) Pageable pageable) {
        return auditService.searchAuditHistory(request, pageable);
    }
}
