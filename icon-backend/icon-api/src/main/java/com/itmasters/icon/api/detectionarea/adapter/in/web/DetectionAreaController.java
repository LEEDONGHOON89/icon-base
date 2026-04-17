package com.itmasters.icon.api.detectionarea.adapter.in.web;

import com.itmasters.icon.api.detectionarea.application.service.DetectionAreaService;
import com.itmasters.icon.api.detectionarea.dto.DetectionAreaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 탐지영역 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/detection-areas")
@RequiredArgsConstructor
public class DetectionAreaController {

    private final DetectionAreaService detectionAreaService;

    /**
     * 전체 탐지영역 조회
     *
     * @param activeOnly (Optional) 활성화된 것만 조회
     * @return 탐지영역 목록
     */
    @GetMapping
    public List<DetectionAreaDto.Response> getDetectionAreas(
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly
    ) {
        if (activeOnly) {
            log.debug("활성화된 탐지영역만 조회");
            return detectionAreaService.getActiveDetectionAreas();
        }

        log.debug("전체 탐지영역 조회");
        return detectionAreaService.getAllDetectionAreas();
    }

    /**
     * 단일 탐지영역 조회
     *
     * @param detectionAreaId 탐지영역 ID
     * @return 탐지영역 정보
     */
    @GetMapping("/{detectionAreaId}")
    public DetectionAreaDto.Response getDetectionArea(@PathVariable String detectionAreaId) {
        log.debug("탐지영역 조회 - detectionAreaId: {}", detectionAreaId);
        return detectionAreaService.getDetectionArea(detectionAreaId);
    }

    /**
     * 탐지영역 생성
     *
     * @param request 생성 요청 DTO
     * @return 생성된 탐지영역 정보
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DetectionAreaDto.Response createDetectionArea(
            @RequestBody DetectionAreaDto.CreateRequest request
    ) {
        log.info("탐지영역 생성 요청");
        return detectionAreaService.createDetectionArea(request);
    }

    /**
     * 탐지영역 수정
     *
     * @param detectionAreaId 탐지영역 ID
     * @param request         수정 요청 DTO
     * @return 수정된 탐지영역 정보
     */
    @PutMapping("/{detectionAreaId}")
    public DetectionAreaDto.Response updateDetectionArea(
            @PathVariable String detectionAreaId,
            @RequestBody DetectionAreaDto.UpdateRequest request
    ) {
        log.info("탐지영역 수정 요청 - detectionAreaId: {}", detectionAreaId);
        return detectionAreaService.updateDetectionArea(detectionAreaId, request);
    }

    /**
     * 탐지영역 삭제
     *
     * @param detectionAreaId 탐지영역 ID
     */
    @DeleteMapping("/{detectionAreaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDetectionArea(@PathVariable String detectionAreaId) {
        log.info("탐지영역 삭제 요청 - detectionAreaId: {}", detectionAreaId);
        detectionAreaService.deleteDetectionArea(detectionAreaId);
    }

    /**
     * 탐지영역 활성화
     *
     * @param detectionAreaId 탐지영역 ID
     * @return 수정된 탐지영역 정보
     */
    @PostMapping("/{detectionAreaId}/activate")
    public DetectionAreaDto.Response activateDetectionArea(@PathVariable String detectionAreaId) {
        log.info("탐지영역 활성화 요청 - detectionAreaId: {}", detectionAreaId);
        return detectionAreaService.activateDetectionArea(detectionAreaId);
    }

    /**
     * 탐지영역 비활성화
     *
     * @param detectionAreaId 탐지영역 ID
     * @return 수정된 탐지영역 정보
     */
    @PostMapping("/{detectionAreaId}/deactivate")
    public DetectionAreaDto.Response deactivateDetectionArea(@PathVariable String detectionAreaId) {
        log.info("탐지영역 비활성화 요청 - detectionAreaId: {}", detectionAreaId);
        return detectionAreaService.deactivateDetectionArea(detectionAreaId);
    }
}
