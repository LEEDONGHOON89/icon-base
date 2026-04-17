package com.itmasters.icon.api.relationrule.adapter.in.web;

import com.itmasters.icon.api.relationrule.application.service.RelationRuleService;
import com.itmasters.icon.api.relationrule.dto.RelationRuleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 도메인 관계 규칙 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/relation-rules")
@RequiredArgsConstructor
public class RelationRuleController {

    private final RelationRuleService relationRuleService;

    /**
     * 전체 도메인 관계 규칙 조회
     *
     * @param dataSourceId (Optional) 데이터소스 ID로 필터링
     * @return 규칙 목록
     */
    @GetMapping
    public List<RelationRuleDto.Response> getRelationRules(
            @RequestParam(required = false) String dataSourceId
    ) {
        if (dataSourceId != null && !dataSourceId.isEmpty()) {
            log.debug("DataSource별 도메인 관계 규칙 조회 - dataSourceId: {}", dataSourceId);
            return relationRuleService.getRelationRulesByDataSource(dataSourceId);
        }

        log.debug("전체 도메인 관계 규칙 조회");
        return relationRuleService.getAllRelationRules();
    }

    /**
     * 단일 도메인 관계 규칙 조회
     *
     * @param ruleId 규칙 ID
     * @return 규칙 정보
     */
    @GetMapping("/{ruleId}")
    public RelationRuleDto.Response getRelationRule(@PathVariable Long ruleId) {
        log.debug("도메인 관계 규칙 조회 - ruleId: {}", ruleId);
        return relationRuleService.getRelationRule(ruleId);
    }

    /**
     * 도메인 관계 규칙 생성
     *
     * @param request 생성 요청 DTO
     * @return 생성된 규칙 정보
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RelationRuleDto.Response createRelationRule(
            @RequestBody RelationRuleDto.CreateRequest request
    ) {
        log.info("도메인 관계 규칙 생성 요청");
        return relationRuleService.createRelationRule(request);
    }

    /**
     * 도메인 관계 규칙 수정
     *
     * @param ruleId 규칙 ID
     * @param request 수정 요청 DTO
     * @return 수정된 규칙 정보
     */
    @PutMapping("/{ruleId}")
    public RelationRuleDto.Response updateRelationRule(
            @PathVariable Long ruleId,
            @RequestBody RelationRuleDto.UpdateRequest request
    ) {
        log.info("도메인 관계 규칙 수정 요청 - ruleId: {}", ruleId);
        return relationRuleService.updateRelationRule(ruleId, request);
    }

    /**
     * 도메인 관계 규칙 삭제
     *
     * @param ruleId 규칙 ID
     */
    @DeleteMapping("/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRelationRule(@PathVariable Long ruleId) {
        log.info("도메인 관계 규칙 삭제 요청 - ruleId: {}", ruleId);
        relationRuleService.deleteRelationRule(ruleId);
    }

    /**
     * 도메인 관계 규칙 활성화
     *
     * @param ruleId 규칙 ID
     * @return 수정된 규칙 정보
     */
    @PostMapping("/{ruleId}/activate")
    public RelationRuleDto.Response activateRelationRule(@PathVariable Long ruleId) {
        log.info("도메인 관계 규칙 활성화 요청 - ruleId: {}", ruleId);
        return relationRuleService.activateRelationRule(ruleId);
    }

    /**
     * 도메인 관계 규칙 비활성화
     *
     * @param ruleId 규칙 ID
     * @return 수정된 규칙 정보
     */
    @PostMapping("/{ruleId}/deactivate")
    public RelationRuleDto.Response deactivateRelationRule(@PathVariable Long ruleId) {
        log.info("도메인 관계 규칙 비활성화 요청 - ruleId: {}", ruleId);
        return relationRuleService.deactivateRelationRule(ruleId);
    }
}
