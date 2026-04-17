package com.itmasters.icon.api.scenario.application.port.in;

import com.itmasters.icon.api.scenario.dto.ScenarioDto;
import com.itmasters.icon.common.domain.scenario.ScenarioOperator;

import java.util.List;

/**
 * 시나리오 서비스 인터페이스
 */
public interface ScenarioService {

    /**
     * 시나리오 생성
     */
    ScenarioDto.Response createScenario(ScenarioDto.CreateCommand command);

    /**
     * 시나리오 조회
     */
    ScenarioDto.Response getScenario(String scenarioId);


    /**
     * 시나리오 목록 조회
     */
    List<ScenarioDto.Response> getAllScenarios();

    /**
     * 활성화된 시나리오 목록 조회
     */
    List<ScenarioDto.Response> getActiveScenarios();

    /**
     * 시나리오 검색
     */
    List<ScenarioDto.Response> searchScenarios(String scenarioName);

    /**
     * 시나리오 수정
     */
    ScenarioDto.Response updateScenario(ScenarioDto.UpdateCommand command);

    /**
     * 시나리오 활성화/비활성화
     */
    ScenarioDto.Response activateScenario(String scenarioId);

    ScenarioDto.Response deactivateScenario(String scenarioId);

    /**
     * 시나리오에 규칙 추가
     */
    ScenarioDto.Response addRuleToScenario(String scenarioId, String ruleId, Integer orderNo, ScenarioOperator operator);

    /**
     * 시나리오에서 규칙 제거
     */
    ScenarioDto.Response removeRuleFromScenario(String scenarioId, String ruleId);
    
    /**
     * 시나리오 삭제
     */
    void deleteScenario(String scenarioId);

    /**
     * 시나리오 목록 페이징/정렬/검색 조회
     */
    com.itmasters.icon.api.common.response.ResponseList<ScenarioDto.Response> getScenariosPaged(
            String search,
            Boolean activeOnly,
            int page,
            int size,
            String sort,
            String dir
    );

    /**
     * 시나리오 시각화 데이터 조회 (Rule + Sensor 정보 포함)
     */
    ScenarioDto.VisualizationResponse getScenarioVisualization(String scenarioId);

}
