package com.itmasters.icon.api.detectionarea.application.service;

import com.itmasters.icon.api.detectionarea.dto.DetectionAreaDto;
import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectionAreaEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.DetectionAreaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 탐지영역 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DetectionAreaService {

    private final DetectionAreaRepository detectionAreaRepository;

    /**
     * 전체 탐지영역 조회 (표시 순서로 정렬)
     *
     * @return 모든 탐지영역 목록
     */
    public List<DetectionAreaDto.Response> getAllDetectionAreas() {
        log.debug("전체 탐지영역 조회");

        return detectionAreaRepository.findAll()
                .stream()
                .map(DetectionAreaDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 활성화된 탐지영역만 조회
     *
     * @return 활성화된 탐지영역 목록
     */
    public List<DetectionAreaDto.Response> getActiveDetectionAreas() {
        log.debug("활성화된 탐지영역 조회");

        return detectionAreaRepository.findByIsActiveTrueOrderByDisplayOrder()
                .stream()
                .map(DetectionAreaDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 단일 탐지영역 조회
     *
     * @param detectionAreaId 탐지영역 ID
     * @return 탐지영역 정보
     */
    public DetectionAreaDto.Response getDetectionArea(String detectionAreaId) {
        log.debug("탐지영역 조회 - detectionAreaId: {}", detectionAreaId);

        DetectionAreaEntity entity = detectionAreaRepository.findById(detectionAreaId)
                .orElseThrow(() -> new IllegalArgumentException("탐지영역을 찾을 수 없습니다. ID: " + detectionAreaId));

        return DetectionAreaDto.Response.from(entity);
    }

    /**
     * 탐지영역 생성
     *
     * @param request 생성 요청 DTO
     * @return 생성된 탐지영역 정보
     */
    @Transactional
    public DetectionAreaDto.Response createDetectionArea(DetectionAreaDto.CreateRequest request) {
        log.info("탐지영역 생성 - detectionAreaId: {}, areaName: {}",
                request.getDetectionAreaId(), request.getAreaName());

        // 탐지영역 ID 검증 (영문 대문자, 숫자, 언더스코어만 허용)
        if (!request.getDetectionAreaId().matches("^[A-Z0-9_]+$")) {
            throw new IllegalArgumentException(
                    "탐지영역 ID는 영문 대문자, 숫자, 언더스코어만 사용할 수 있습니다: " + request.getDetectionAreaId()
            );
        }

        // 중복 체크
        if (detectionAreaRepository.existsByDetectionAreaId(request.getDetectionAreaId())) {
            throw new IllegalStateException(
                    "이미 존재하는 탐지영역 ID입니다: " + request.getDetectionAreaId()
            );
        }

        // 엔티티 생성
        DetectionAreaEntity entity = DetectionAreaEntity.of(
                request.getDetectionAreaId(),
                request.getAreaName(),
                request.getDescription(),
                request.getIcon(),
                request.getColor(),
                request.getDisplayOrder()
        );

        // 저장
        DetectionAreaEntity saved = detectionAreaRepository.save(entity);

        log.info("탐지영역 생성 완료 - detectionAreaId: {}", saved.getDetectionAreaId());

        return DetectionAreaDto.Response.from(saved);
    }

    /**
     * 탐지영역 수정
     *
     * @param detectionAreaId 탐지영역 ID
     * @param request         수정 요청 DTO
     * @return 수정된 탐지영역 정보
     */
    @Transactional
    public DetectionAreaDto.Response updateDetectionArea(String detectionAreaId, DetectionAreaDto.UpdateRequest request) {
        log.info("탐지영역 수정 - detectionAreaId: {}", detectionAreaId);

        // 기존 탐지영역 조회
        DetectionAreaEntity entity = detectionAreaRepository.findById(detectionAreaId)
                .orElseThrow(() -> new IllegalArgumentException("탐지영역을 찾을 수 없습니다. ID: " + detectionAreaId));

        // 정보 수정
        entity.update(
                request.getAreaName(),
                request.getDescription(),
                request.getIcon(),
                request.getColor(),
                request.getDisplayOrder()
        );

        // 활성화 상태 변경
        if (request.getIsActive() != null) {
            if (request.getIsActive()) {
                entity.activate();
            } else {
                entity.deactivate();
            }
        }

        log.info("탐지영역 수정 완료 - detectionAreaId: {}", detectionAreaId);

        return DetectionAreaDto.Response.from(entity);
    }

    /**
     * 탐지영역 삭제
     *
     * @param detectionAreaId 탐지영역 ID
     */
    @Transactional
    public void deleteDetectionArea(String detectionAreaId) {
        log.info("탐지영역 삭제 - detectionAreaId: {}", detectionAreaId);

        DetectionAreaEntity entity = detectionAreaRepository.findById(detectionAreaId)
                .orElseThrow(() -> new IllegalArgumentException("탐지영역을 찾을 수 없습니다. ID: " + detectionAreaId));

        detectionAreaRepository.delete(entity);

        log.info("탐지영역 삭제 완료 - detectionAreaId: {}", detectionAreaId);
    }

    /**
     * 탐지영역 활성화
     *
     * @param detectionAreaId 탐지영역 ID
     * @return 수정된 탐지영역 정보
     */
    @Transactional
    public DetectionAreaDto.Response activateDetectionArea(String detectionAreaId) {
        log.info("탐지영역 활성화 - detectionAreaId: {}", detectionAreaId);

        DetectionAreaEntity entity = detectionAreaRepository.findById(detectionAreaId)
                .orElseThrow(() -> new IllegalArgumentException("탐지영역을 찾을 수 없습니다. ID: " + detectionAreaId));

        entity.activate();

        log.info("탐지영역 활성화 완료 - detectionAreaId: {}", detectionAreaId);

        return DetectionAreaDto.Response.from(entity);
    }

    /**
     * 탐지영역 비활성화
     *
     * @param detectionAreaId 탐지영역 ID
     * @return 수정된 탐지영역 정보
     */
    @Transactional
    public DetectionAreaDto.Response deactivateDetectionArea(String detectionAreaId) {
        log.info("탐지영역 비활성화 - detectionAreaId: {}", detectionAreaId);

        DetectionAreaEntity entity = detectionAreaRepository.findById(detectionAreaId)
                .orElseThrow(() -> new IllegalArgumentException("탐지영역을 찾을 수 없습니다. ID: " + detectionAreaId));

        entity.deactivate();

        log.info("탐지영역 비활성화 완료 - detectionAreaId: {}", detectionAreaId);

        return DetectionAreaDto.Response.from(entity);
    }
}
