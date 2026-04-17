package com.itmasters.icon.api.domaintype.application.service;

import com.itmasters.icon.api.domaintype.dto.DomainTypeDto;
import com.itmasters.icon.engine.adapter.out.persistence.entity.DomainTypeEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.DomainTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 도메인 타입 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DomainTypeService {

    private final DomainTypeRepository domainTypeRepository;

    /**
     * 전체 도메인 타입 조회 (표시 순서로 정렬)
     *
     * @return 모든 도메인 타입 목록
     */
    public List<DomainTypeDto.Response> getAllDomainTypes() {
        log.debug("전체 도메인 타입 조회");

        return domainTypeRepository.findAll()
                .stream()
                .map(DomainTypeDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 활성화된 도메인 타입만 조회
     *
     * @return 활성화된 도메인 타입 목록
     */
    public List<DomainTypeDto.Response> getActiveDomainTypes() {
        log.debug("활성화된 도메인 타입 조회");

        return domainTypeRepository.findByIsActiveTrueOrderByDisplayOrder()
                .stream()
                .map(DomainTypeDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 단일 도메인 타입 조회
     *
     * @param domainTypeId 도메인 타입 ID
     * @return 도메인 타입 정보
     */
    public DomainTypeDto.Response getDomainType(String domainTypeId) {
        log.debug("도메인 타입 조회 - domainTypeId: {}", domainTypeId);

        DomainTypeEntity entity = domainTypeRepository.findById(domainTypeId)
                .orElseThrow(() -> new IllegalArgumentException("도메인 타입을 찾을 수 없습니다. ID: " + domainTypeId));

        return DomainTypeDto.Response.from(entity);
    }

    /**
     * 도메인 타입 생성
     *
     * @param request 생성 요청 DTO
     * @return 생성된 도메인 타입 정보
     */
    @Transactional
    public DomainTypeDto.Response createDomainType(DomainTypeDto.CreateRequest request) {
        log.info("도메인 타입 생성 - domainTypeId: {}, domainName: {}",
                request.getDomainTypeId(), request.getDomainName());

        // 도메인 타입 ID 검증 (영문 대문자, 숫자, 언더스코어만 허용)
        if (!request.getDomainTypeId().matches("^[A-Z0-9_]+$")) {
            throw new IllegalArgumentException(
                    "도메인 타입 ID는 영문 대문자, 숫자, 언더스코어만 사용할 수 있습니다: " + request.getDomainTypeId()
            );
        }

        // 중복 체크
        if (domainTypeRepository.existsByDomainTypeId(request.getDomainTypeId())) {
            throw new IllegalStateException(
                    "이미 존재하는 도메인 타입 ID입니다: " + request.getDomainTypeId()
            );
        }

        // 엔티티 생성
        DomainTypeEntity entity = DomainTypeEntity.of(
                request.getDomainTypeId(),
                request.getDomainName(),
                request.getDescription(),
                request.getIcon(),
                request.getColor(),
                request.getDisplayOrder()
        );

        // 저장
        DomainTypeEntity saved = domainTypeRepository.save(entity);

        log.info("도메인 타입 생성 완료 - domainTypeId: {}", saved.getDomainTypeId());

        return DomainTypeDto.Response.from(saved);
    }

    /**
     * 도메인 타입 수정
     *
     * @param domainTypeId 도메인 타입 ID
     * @param request      수정 요청 DTO
     * @return 수정된 도메인 타입 정보
     */
    @Transactional
    public DomainTypeDto.Response updateDomainType(String domainTypeId, DomainTypeDto.UpdateRequest request) {
        log.info("도메인 타입 수정 - domainTypeId: {}", domainTypeId);

        // 기존 도메인 타입 조회
        DomainTypeEntity entity = domainTypeRepository.findById(domainTypeId)
                .orElseThrow(() -> new IllegalArgumentException("도메인 타입을 찾을 수 없습니다. ID: " + domainTypeId));

        // 정보 수정
        entity.update(
                request.getDomainName(),
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

        log.info("도메인 타입 수정 완료 - domainTypeId: {}", domainTypeId);

        return DomainTypeDto.Response.from(entity);
    }

    /**
     * 도메인 타입 삭제
     *
     * @param domainTypeId 도메인 타입 ID
     */
    @Transactional
    public void deleteDomainType(String domainTypeId) {
        log.info("도메인 타입 삭제 - domainTypeId: {}", domainTypeId);

        DomainTypeEntity entity = domainTypeRepository.findById(domainTypeId)
                .orElseThrow(() -> new IllegalArgumentException("도메인 타입을 찾을 수 없습니다. ID: " + domainTypeId));

        domainTypeRepository.delete(entity);

        log.info("도메인 타입 삭제 완료 - domainTypeId: {}", domainTypeId);
    }

    /**
     * 도메인 타입 활성화
     *
     * @param domainTypeId 도메인 타입 ID
     * @return 수정된 도메인 타입 정보
     */
    @Transactional
    public DomainTypeDto.Response activateDomainType(String domainTypeId) {
        log.info("도메인 타입 활성화 - domainTypeId: {}", domainTypeId);

        DomainTypeEntity entity = domainTypeRepository.findById(domainTypeId)
                .orElseThrow(() -> new IllegalArgumentException("도메인 타입을 찾을 수 없습니다. ID: " + domainTypeId));

        entity.activate();

        log.info("도메인 타입 활성화 완료 - domainTypeId: {}", domainTypeId);

        return DomainTypeDto.Response.from(entity);
    }

    /**
     * 도메인 타입 비활성화
     *
     * @param domainTypeId 도메인 타입 ID
     * @return 수정된 도메인 타입 정보
     */
    @Transactional
    public DomainTypeDto.Response deactivateDomainType(String domainTypeId) {
        log.info("도메인 타입 비활성화 - domainTypeId: {}", domainTypeId);

        DomainTypeEntity entity = domainTypeRepository.findById(domainTypeId)
                .orElseThrow(() -> new IllegalArgumentException("도메인 타입을 찾을 수 없습니다. ID: " + domainTypeId));

        entity.deactivate();

        log.info("도메인 타입 비활성화 완료 - domainTypeId: {}", domainTypeId);

        return DomainTypeDto.Response.from(entity);
    }
}
