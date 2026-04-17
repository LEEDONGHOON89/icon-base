package com.itmasters.icon.api.domaintype.adapter.in.web;

import com.itmasters.icon.api.domaintype.application.service.DomainTypeService;
import com.itmasters.icon.api.domaintype.dto.DomainTypeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 도메인 타입 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/domain-types")
@RequiredArgsConstructor
public class DomainTypeController {

    private final DomainTypeService domainTypeService;

    /**
     * 전체 도메인 타입 조회
     *
     * @param activeOnly (Optional) 활성화된 것만 조회
     * @return 도메인 타입 목록
     */
    @GetMapping
    public List<DomainTypeDto.Response> getDomainTypes(
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly
    ) {
        if (activeOnly) {
            log.debug("활성화된 도메인 타입만 조회");
            return domainTypeService.getActiveDomainTypes();
        }

        log.debug("전체 도메인 타입 조회");
        return domainTypeService.getAllDomainTypes();
    }

    /**
     * 단일 도메인 타입 조회
     *
     * @param domainTypeId 도메인 타입 ID
     * @return 도메인 타입 정보
     */
    @GetMapping("/{domainTypeId}")
    public DomainTypeDto.Response getDomainType(@PathVariable String domainTypeId) {
        log.debug("도메인 타입 조회 - domainTypeId: {}", domainTypeId);
        return domainTypeService.getDomainType(domainTypeId);
    }

    /**
     * 도메인 타입 생성
     *
     * @param request 생성 요청 DTO
     * @return 생성된 도메인 타입 정보
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DomainTypeDto.Response createDomainType(
            @RequestBody DomainTypeDto.CreateRequest request
    ) {
        log.info("도메인 타입 생성 요청");
        return domainTypeService.createDomainType(request);
    }

    /**
     * 도메인 타입 수정
     *
     * @param domainTypeId 도메인 타입 ID
     * @param request      수정 요청 DTO
     * @return 수정된 도메인 타입 정보
     */
    @PutMapping("/{domainTypeId}")
    public DomainTypeDto.Response updateDomainType(
            @PathVariable String domainTypeId,
            @RequestBody DomainTypeDto.UpdateRequest request
    ) {
        log.info("도메인 타입 수정 요청 - domainTypeId: {}", domainTypeId);
        return domainTypeService.updateDomainType(domainTypeId, request);
    }

    /**
     * 도메인 타입 삭제
     *
     * @param domainTypeId 도메인 타입 ID
     */
    @DeleteMapping("/{domainTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDomainType(@PathVariable String domainTypeId) {
        log.info("도메인 타입 삭제 요청 - domainTypeId: {}", domainTypeId);
        domainTypeService.deleteDomainType(domainTypeId);
    }

    /**
     * 도메인 타입 활성화
     *
     * @param domainTypeId 도메인 타입 ID
     * @return 수정된 도메인 타입 정보
     */
    @PostMapping("/{domainTypeId}/activate")
    public DomainTypeDto.Response activateDomainType(@PathVariable String domainTypeId) {
        log.info("도메인 타입 활성화 요청 - domainTypeId: {}", domainTypeId);
        return domainTypeService.activateDomainType(domainTypeId);
    }

    /**
     * 도메인 타입 비활성화
     *
     * @param domainTypeId 도메인 타입 ID
     * @return 수정된 도메인 타입 정보
     */
    @PostMapping("/{domainTypeId}/deactivate")
    public DomainTypeDto.Response deactivateDomainType(@PathVariable String domainTypeId) {
        log.info("도메인 타입 비활성화 요청 - domainTypeId: {}", domainTypeId);
        return domainTypeService.deactivateDomainType(domainTypeId);
    }
}
