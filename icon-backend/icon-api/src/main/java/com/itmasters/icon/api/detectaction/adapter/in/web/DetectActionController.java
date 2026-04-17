package com.itmasters.icon.api.detectaction.adapter.in.web;

import com.itmasters.icon.api.detectaction.application.DetectActionService;
import com.itmasters.icon.api.detectaction.dto.DetectActionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;

/**
 * 탐지 조치 관리 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/detect-actions")
@RequiredArgsConstructor
@Tag(name = "Detect Action", description = "탐지 조치 관리 API")
public class DetectActionController {

    private final DetectActionService detectActionService;

    /**
     * 탐지 조치 목록 조회
     */
    @GetMapping
    @Operation(summary = "탐지 조치 목록 조회", description = "날짜 범위, 위험수준, 조치 상태별로 조치 목록 조회")
    public Page<DetectActionDto> getDetectActions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String actionStatus,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        log.info("Fetching detect actions - startDate: {}, endDate: {}, riskLevel: {}, actionStatus: {}",
                startDate, endDate, riskLevel, actionStatus);

        DetectActionDto.SearchRequest request = DetectActionDto.SearchRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .riskLevel(riskLevel)
                .actionStatus(actionStatus)
                .page(page)
                .size(size)
                .build();

        return detectActionService.getDetectActions(request);
    }

    /**
     * 탐지 조치 업데이트
     */
    @PutMapping("/{id}")
    @Operation(summary = "탐지 조치 업데이트", description = "조치 메모, 사유, 상태를 업데이트합니다")
    public DetectActionDto updateDetectAction(
            @PathVariable Long id,
            @RequestBody DetectActionDto.UpdateRequest request,
            Principal principal
    ) {
        String currentUserId = principal.getName();
        log.info("Updating detect action {} by user {}", id, currentUserId);

        return detectActionService.updateDetectAction(id, request, currentUserId);
    }
}
