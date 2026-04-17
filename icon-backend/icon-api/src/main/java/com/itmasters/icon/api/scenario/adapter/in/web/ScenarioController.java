package com.itmasters.icon.api.scenario.adapter.in.web;

import com.itmasters.icon.api.common.response.ResponseList;
import com.itmasters.icon.api.common.response.ResponseOk;
import com.itmasters.icon.api.scenario.application.port.in.ScenarioService;
import com.itmasters.icon.api.scenario.dto.ScenarioDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 시나리오 관리 Controller
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioService scenarioService;

    /**
     * 시나리오 생성
     */
    @PostMapping("/api/v1/scenarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseOk<ScenarioDto.Response> createScenario(@Valid @RequestBody ScenarioDto.CreateRequest request) {
        log.info("Creating scenario: {}", request.getScenarioName());

        ScenarioDto.CreateCommand command = request.toCommand();

        ScenarioDto.Response result = scenarioService.createScenario(command);

        return ResponseOk.of("시나리오가 생성되었습니다.", result);
    }

    /**
     * 시나리오 조회
     */
    @GetMapping("/api/v1/scenarios/{scenarioId}")
    public ResponseOk<ScenarioDto.Response> getScenario(@PathVariable String scenarioId) {
        log.info("Getting scenario: {}", scenarioId);

        ScenarioDto.Response result = scenarioService.getScenario(scenarioId);

        return ResponseOk.of("시나리오 조회 성공", result);
    }

    /**
     * 시나리오 목록 조회
     */
    @GetMapping("/api/v1/scenarios")
    public ResponseList<ScenarioDto.Response> getScenarios(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "name") String sort,
            @RequestParam(required = false, defaultValue = "asc") String dir) {

        log.info("Getting scenarios, activeOnly: {}, search: {}, page: {} size: {} sort: {} dir: {}", activeOnly, search, page, size, sort, dir);

        if (page != null || size != null) {
            return scenarioService.getScenariosPaged(search, activeOnly, page == null ? 1 : page, size == null ? 20 : size, sort, dir);
        }

        List<ScenarioDto.Response> results;
        if (search != null && !search.trim().isEmpty()) {
            results = scenarioService.searchScenarios(search.trim());
        } else if (Boolean.TRUE.equals(activeOnly)) {
            results = scenarioService.getActiveScenarios();
        } else {
            results = scenarioService.getAllScenarios();
        }

        return new ResponseList<>(results); // ResponseList.
    }

    /**
     * 시나리오 수정
     */
    @PutMapping("/api/v1/scenarios/{scenarioId}")
    public ResponseOk<ScenarioDto.Response> updateScenario(
            @PathVariable String scenarioId,
            @Valid @RequestBody ScenarioDto.UpdateRequest request) {

        log.info("Updating scenario: {}", scenarioId);

        ScenarioDto.UpdateCommand command = request.toCommand(scenarioId);

        ScenarioDto.Response result = scenarioService.updateScenario(command);

        return ResponseOk.of("시나리오가 수정되었습니다.", result);
    }

    /**
     * 시나리오 활성화
     */
    @PatchMapping("/api/v1/scenarios/{scenarioId}/activate")
    public ResponseOk<ScenarioDto.Response> activateScenario(
            @PathVariable String scenarioId) {

        log.info("Activating scenario: {}", scenarioId);

        ScenarioDto.Response response = scenarioService.activateScenario(scenarioId);

        return ResponseOk.of("시나리오가 활성화되었습니다.", response);
    }

    /**
     * 시나리오 비활성화
     */
    @PatchMapping("/api/v1/scenarios/{scenarioId}/deactivate")
    public ResponseOk<ScenarioDto.Response> deactivateScenario(
            @PathVariable String scenarioId) {

        log.info("Deactivating scenario: {}", scenarioId);

        ScenarioDto.Response response = scenarioService.deactivateScenario(scenarioId);

        return ResponseOk.of("시나리오가 비활성화되었습니다.", response);
    }

    /**
     * 시나리오 삭제
     */
    @DeleteMapping("/api/v1/scenarios/{scenarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseOk<Void> deleteScenario(
            @PathVariable String scenarioId) {

        log.info("Deleting scenario: {}", scenarioId);

        scenarioService.deleteScenario(scenarioId);

        return ResponseOk.of("시나리오가 삭제되었습니다.", null);
    }

    // ========== 세분화된 수정 API ==========

    /**
     * 시나리오 기본정보 수정
     */
    @PatchMapping("/api/v1/scenarios/{scenarioId}/basic-info")
    public ResponseOk<ScenarioDto.Response> updateBasicInfo(
            @PathVariable String scenarioId,
            @Valid @RequestBody ScenarioDto.UpdateBasicInfoRequest request) {

        log.info("Updating scenario basic info: {}", scenarioId);

        ScenarioDto.UpdateCommand command = request.toCommand(scenarioId);
        ScenarioDto.Response result = scenarioService.updateScenario(command);

        return ResponseOk.of("시나리오 기본정보가 수정되었습니다.", result);
    }

    /**
     * 시나리오 엔티티 필터 수정
     */
    @PatchMapping("/api/v1/scenarios/{scenarioId}/entity-filter")
    public ResponseOk<ScenarioDto.Response> updateEntityFilter(
            @PathVariable String scenarioId,
            @Valid @RequestBody ScenarioDto.UpdateEntityFilterRequest request) {

        log.info("Updating scenario entity filter: {}", scenarioId);

        ScenarioDto.UpdateCommand command = request.toCommand(scenarioId);
        ScenarioDto.Response result = scenarioService.updateScenario(command);

        return ResponseOk.of("시나리오 엔티티 필터가 수정되었습니다.", result);
    }

    /**
     * 시나리오 집계구성 수정
     */
    @PatchMapping("/api/v1/scenarios/{scenarioId}/rules")
    public ResponseOk<ScenarioDto.Response> updateRules(
            @PathVariable String scenarioId,
            @Valid @RequestBody ScenarioDto.UpdateRulesRequest request) {

        log.info("Updating scenario rules: {}", scenarioId);

        ScenarioDto.UpdateCommand command = request.toCommand(scenarioId);
        ScenarioDto.Response result = scenarioService.updateScenario(command);

        return ResponseOk.of("시나리오 집계구성이 수정되었습니다.", result);
    }

    /**
     * 시나리오에 규칙 추가
     */
    @PostMapping("/api/v1/scenarios/{scenarioId}/rules")
    public ResponseOk<ScenarioDto.Response> addRuleToScenario(
            @PathVariable String scenarioId,
            @Valid @RequestBody ScenarioDto.AddRuleRequest request) {

        log.info("Adding rule to scenario: {} - {}", scenarioId, request.getRuleId());

        ScenarioDto.Response result = scenarioService.addRuleToScenario(
                scenarioId,
                request.getRuleId(),
                request.getOrderNo(),
                request.getOperator()
        );


        return ResponseOk.of("시나리오에 규칙이 추가되었습니다.", result);
    }

    /**
     * 시나리오에서 규칙 제거
     */
    @DeleteMapping("/api/v1/scenarios/{scenarioId}/rules/{ruleId}")
    public ResponseOk<ScenarioDto.Response> removeRuleFromScenario(
            @PathVariable String scenarioId,
            @PathVariable String ruleId) {

        log.info("Removing rule from scenario: {} - {}", scenarioId, ruleId);

        ScenarioDto.Response response = scenarioService.removeRuleFromScenario(scenarioId, ruleId);

        return ResponseOk.of("시나리오에서 규칙이 제거되었습니다.", response);
    }

    /**
     * 시나리오 시각화 데이터 조회
     */
    @GetMapping("/api/v1/scenarios/{scenarioId}/visualization")
    public ScenarioDto.VisualizationResponse getScenarioVisualization(
            @PathVariable String scenarioId) {

        log.info("Getting visualization for scenario: {}", scenarioId);

        return scenarioService.getScenarioVisualization(scenarioId);
    }
}