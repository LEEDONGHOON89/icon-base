package com.itmasters.icon.api.scenario.application.service;

import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.JpaRuleRepository;
import com.itmasters.icon.api.common.exception.NoDataException;
import com.itmasters.icon.api.scenario.application.port.in.ScenarioService;
import com.itmasters.icon.api.scenario.application.port.out.ScenarioRepository;
import com.itmasters.icon.api.scenario.application.port.out.ScenarioRuleRepository;
import com.itmasters.icon.api.scenario.domain.Scenario;
import com.itmasters.icon.api.scenario.domain.ScenarioRule;
import com.itmasters.icon.api.scenario.dto.ScenarioDto;
import com.itmasters.icon.api.scenario.dto.ScenarioDto.CreateCommand;
import com.itmasters.icon.common.domain.scenario.ScenarioOperator;
import com.itmasters.icon.api.audit.application.service.DetectionConfigAuditService;
import com.itmasters.icon.api.audit.domain.ConfigAuditTargetType;
import com.itmasters.icon.api.audit.util.RequestUtils;
import com.itmasters.icon.api.rule.application.port.out.SensorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.itmasters.icon.api.scenario.dto.ScenarioDto.*;
import static com.itmasters.icon.api.scenario.dto.ScenarioDto.CreateCommand.*;

/**
 * 시나리오 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ScenarioServiceImpl implements ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioRuleRepository scenarioRuleRepository;
    private final JpaRuleRepository ruleRepository;
    private final SensorRepository sensorRepository;
    private final DetectionConfigAuditService auditService;


    @Override
    @Transactional
    public Response createScenario(CreateCommand command) {
        log.info("Creating scenario: {}", command.getScenarioName());

        // 기본 검증
        validateScenarioCommand(command);

        // 시나리오 생성 (엔진 설정 필드 포함)
        Scenario scenario = Scenario.of(
                command.getScenarioName(),
                command.getDescription(),
                command.getEntityFilterJson(),
                command.getRiskLevelId(),
                command.getDetectionAreaId(),
                command.getPrimaryEntityType(),
                command.getDedupMinutes()
        );
        scenario.validate();

        Scenario savedScenario = scenarioRepository.save(scenario);

        // 규칙 매핑 생성
        if (command.getRules() != null && !command.getRules().isEmpty()) {
            List<ScenarioRule> scenarioRules = command.getRules().stream()
                    .map(ruleCommand -> {
                        // 집계 존재 확인 (ruleId 필드에 aggregateId를 전달)
                        if (!ruleRepository.existsById(ruleCommand.getRuleId())) {
                            throw new NoDataException("존재하지 않는 집계입니다: " + ruleCommand.getRuleId());
                        }

                        return ScenarioRule.of(
                                savedScenario.getScenarioId(),
                                ruleCommand.getRuleId(),
                                ruleCommand.getOrderNo(),
                                ruleCommand.getOperator()
                        );
                    })
                    .toList();

            scenarioRuleRepository.saveAll(scenarioRules);
        }

        // 감사 로그 기록
        Response response = getScenarioWithRules(savedScenario);
        String currentUser = getCurrentUser();
        auditService.logCreate(
                ConfigAuditTargetType.SCENARIO,
                savedScenario.getScenarioId(),
                savedScenario.getScenarioName(),
                response,
                currentUser,
                RequestUtils.getClientIpAddress()
        );

        // 규칙 정보 포함하여 반환
        return response;
    }

    @Override
    public Response getScenario(String scenarioId) {
        log.info("Getting scenario: {}", scenarioId);

        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NoDataException("시나리오를 찾을 수 없습니다: " + scenarioId));

        return getScenarioWithRules(scenario);
    }

    @Override
    public List<Response> getAllScenarios() {
        log.info("Getting all scenarios");

        return scenarioRepository.findAll()
                .stream()
                .map(this::getScenarioWithRules)
                .toList();
    }

    public com.itmasters.icon.api.common.response.ResponseList<Response> getScenariosPaged(String search, Boolean activeOnly, int page, int size, String sort, String dir) {
        int p = page < 1 ? 1 : page;
        int s = size < 1 ? 20 : Math.min(size, 1000);
        org.springframework.data.domain.Sort.Direction d = (dir == null || dir.equalsIgnoreCase("asc")) ? org.springframework.data.domain.Sort.Direction.ASC : org.springframework.data.domain.Sort.Direction.DESC;
        String sortKey = (sort == null || sort.isBlank()) ? "scenarioName" : ("name".equalsIgnoreCase(sort) ? "scenarioName" : sort);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(p - 1, s, org.springframework.data.domain.Sort.by(d, sortKey));
        var pageResult = scenarioRepository.findPagedFiltered(search, activeOnly, pageable);
        var data = pageResult.getContent().stream().map(this::getScenarioWithRules).toList();
        return new com.itmasters.icon.api.common.response.ResponseList<>(pageResult.getTotalElements(), data);
    }

    @Override
    public List<Response> getActiveScenarios() {
        return scenarioRepository.findByIsActiveTrue()
                .stream()
                .map(this::getScenarioWithRules)
                .toList();
    }

    @Override
    public List<Response> searchScenarios(String scenarioName) {
        log.info("Searching scenarios: {}", scenarioName);

        return scenarioRepository.findByScenarioNameContaining(scenarioName)
                .stream()
                .map(this::getScenarioWithRules)
                .toList();
    }

    @Override
    @Transactional
    public Response updateScenario(UpdateCommand command) {
        log.info("Updating scenario: {}", command.getScenarioId());

        // 기본 검증
        validateScenarioUpdateCommand(command);

        Scenario scenario = scenarioRepository.findById(command.getScenarioId())
                .orElseThrow(() -> new NoDataException("시나리오를 찾을 수 없습니다: " + command.getScenarioId()));

        // 감사 로그용 변경 전 상태 저장
        Response beforeSnapshot = getScenarioWithRules(scenario);

        // 시나리오명 중복 확인 (자기 자신 제외) - scenarioName이 전달된 경우에만
        if (command.getScenarioName() != null &&
                !scenario.getScenarioName().equals(command.getScenarioName()) &&
                scenarioRepository.existsByScenarioName(command.getScenarioName())) {
            throw new IllegalArgumentException("이미 존재하는 시나리오명입니다: " + command.getScenarioName());
        }

        // 시나리오 수정 (null인 필드는 기존 값 유지, 엔진 설정 필드 포함)
        log.info("🔍 Before update - entityFilterJson: {}", scenario.getEntityFilterJson());
        log.info("🔍 Command entityFilterJson: {}", command.getEntityFilterJson());
        scenario.update(
                command.getScenarioName(),
                command.getDescription(),
                command.getEntityFilterJson(),
                command.getIsActive(),
                command.getRiskLevelId(),
                command.getDetectionAreaId(),
                command.getPrimaryEntityType(),
                command.getDedupMinutes()
        );
        log.info("🔍 After update - entityFilterJson: {}", scenario.getEntityFilterJson());
        Scenario updatedScenario = scenarioRepository.save(scenario);
        log.info("🔍 After save - entityFilterJson: {}", updatedScenario.getEntityFilterJson());

        // 규칙 업데이트 (command.getRules()가 null이 아닌 경우에만)
        if (command.getRules() != null) {
            // 기존 규칙 매핑 조회
            List<ScenarioRule> existingRules = scenarioRuleRepository.findByScenarioIdOrderByOrderNo(command.getScenarioId());

            if (!command.getRules().isEmpty()) {
                // 새 규칙의 ruleId 목록
                Set<String> newRuleIds = command.getRules().stream()
                        .map(UpdateCommand.ScenarioRuleCommand::getRuleId)
                        .collect(Collectors.toSet());

                // 삭제할 규칙 (기존에 있었는데 새 목록에 없는 것)
                existingRules.stream()
                        .filter(rule -> !newRuleIds.contains(rule.getRuleId()))
                        .forEach(rule -> scenarioRuleRepository.deleteByScenarioIdAndRuleId(
                                command.getScenarioId(), rule.getRuleId()));

                // 추가/업데이트할 규칙
                List<ScenarioRule> rulesToSave = command.getRules().stream()
                        .map(ruleCommand -> {
                            // 집계 존재 확인
                            if (!ruleRepository.existsById(ruleCommand.getRuleId())) {
                                throw new NoDataException("존재하지 않는 집계입니다: " + ruleCommand.getRuleId());
                            }

                            // 기존 규칙이 있으면 업데이트, 없으면 새로 생성
                            ScenarioRule existingRule = existingRules.stream()
                                    .filter(r -> r.getRuleId().equals(ruleCommand.getRuleId()))
                                    .findFirst()
                                    .orElse(null);

                            if (existingRule != null) {
                                // 기존 규칙 업데이트
                                existingRule.setOrder(ruleCommand.getOrderNo());
                                existingRule.setOperator(ruleCommand.getOperator());
                                return existingRule;
                            } else {
                                // 새 규칙 생성
                                return ScenarioRule.of(
                                        updatedScenario.getScenarioId(),
                                        ruleCommand.getRuleId(),
                                        ruleCommand.getOrderNo(),
                                        ruleCommand.getOperator()
                                );
                            }
                        })
                        .toList();

                scenarioRuleRepository.saveAll(rulesToSave);
            } else {
                // 빈 목록이 명시적으로 전달된 경우에만 모두 삭제
                scenarioRuleRepository.deleteByScenarioId(command.getScenarioId());
            }
        }
        // command.getRules()가 null이면 규칙은 건드리지 않음

        // 감사 로그 기록
        Response afterSnapshot = getScenarioWithRules(updatedScenario);
        List<String> changedFields = detectChangedFields(beforeSnapshot, afterSnapshot);
        if (!changedFields.isEmpty()) {
            String currentUser = getCurrentUser();
            auditService.logUpdate(
                    ConfigAuditTargetType.SCENARIO,
                    updatedScenario.getScenarioId(),
                    updatedScenario.getScenarioName(),
                    beforeSnapshot,
                    afterSnapshot,
                    changedFields,
                    currentUser,
                    RequestUtils.getClientIpAddress()
            );
        }

        return afterSnapshot;
    }

    @Override
    @Transactional
    public Response activateScenario(String scenarioId) {
        log.info("Activating scenario: {}", scenarioId);

        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NoDataException("시나리오를 찾을 수 없습니다: " + scenarioId));

        Response beforeSnapshot = getScenarioWithRules(scenario);
        scenario.activate();
        Scenario updatedScenario = scenarioRepository.save(scenario);
        Response afterSnapshot = getScenarioWithRules(updatedScenario);

        // 감사 로그 기록
        String currentUser = getCurrentUser();
        auditService.logUpdate(
                ConfigAuditTargetType.SCENARIO,
                updatedScenario.getScenarioId(),
                updatedScenario.getScenarioName(),
                beforeSnapshot,
                afterSnapshot,
                List.of("isActive"),
                currentUser,
                RequestUtils.getClientIpAddress()
        );

        return afterSnapshot;
    }

    @Override
    @Transactional
    public Response deactivateScenario(String scenarioId) {
        log.info("Deactivating scenario: {}", scenarioId);

        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NoDataException("시나리오를 찾을 수 없습니다: " + scenarioId));

        Response beforeSnapshot = getScenarioWithRules(scenario);
        scenario.deactivate();
        Scenario updatedScenario = scenarioRepository.save(scenario);
        Response afterSnapshot = getScenarioWithRules(updatedScenario);

        // 감사 로그 기록
        String currentUser = getCurrentUser();
        auditService.logUpdate(
                ConfigAuditTargetType.SCENARIO,
                updatedScenario.getScenarioId(),
                updatedScenario.getScenarioName(),
                beforeSnapshot,
                afterSnapshot,
                List.of("isActive"),
                currentUser,
                RequestUtils.getClientIpAddress()
        );

        return afterSnapshot;
    }


    @Override
    @Transactional
    public Response addRuleToScenario(String scenarioId, String ruleId, Integer orderNo, ScenarioOperator operator) {
        log.info("Adding rule to scenario: {} - {} with operator: {}", scenarioId, ruleId, operator);

        // 시나리오 존재 확인
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NoDataException("시나리오를 찾을 수 없습니다: " + scenarioId));

        // 집계 존재 확인
        if (!ruleRepository.existsById(ruleId)) {
            throw new NoDataException("존재하지 않는 집계입니다: " + ruleId);
        }

        // 중복 확인
        if (scenarioRuleRepository.existsByScenarioIdAndRuleId(scenarioId, ruleId)) {
            throw new IllegalArgumentException("이미 시나리오에 추가된 집계입니다: " + ruleId);
        }

        // 규칙 매핑 생성 (operator 포함)
        ScenarioRule scenarioRule = ScenarioRule.of(scenarioId, ruleId, orderNo, operator);
        scenarioRuleRepository.save(scenarioRule);

        return getScenarioWithRules(scenario);
    }

    @Override
    @Transactional
    public Response removeRuleFromScenario(String scenarioId, String ruleId) {
        log.info("Removing rule from scenario: {} - {}", scenarioId, ruleId);

        // 시나리오 존재 확인
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NoDataException("시나리오를 찾을 수 없습니다: " + scenarioId));

        // 규칙 매핑 삭제
        scenarioRuleRepository.deleteByScenarioIdAndRuleId(scenarioId, ruleId);

        return getScenarioWithRules(scenario);
    }

    @Override
    @Transactional
    public void deleteScenario(String scenarioId) {
        log.info("Deleting scenario: {}", scenarioId);

        // 시나리오 조회 (삭제 전 상태 저장)
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NoDataException("시나리오를 찾을 수 없습니다: " + scenarioId));
        Response beforeSnapshot = getScenarioWithRules(scenario);
        String scenarioName = scenario.getScenarioName();

        // 시나리오-규칙 매핑 먼저 삭제
        scenarioRuleRepository.deleteByScenarioId(scenarioId);

        // 시나리오 삭제
        scenarioRepository.deleteById(scenarioId);

        // 감사 로그 기록
        String currentUser = getCurrentUser();
        auditService.logDelete(
                ConfigAuditTargetType.SCENARIO,
                scenarioId,
                scenarioName,
                beforeSnapshot,
                currentUser,
                RequestUtils.getClientIpAddress()
        );
    }

    /**
     * 시나리오에 규칙 정보를 포함한 결과 반환
     */
    private Response getScenarioWithRules(Scenario scenario) {
        List<ScenarioRule> scenarioRules = scenarioRuleRepository.findByScenarioIdOrderByOrderNo(scenario.getScenarioId());

        List<Response.ScenarioRuleResponse> ruleInfos = scenarioRules.stream()
                .map(scenarioRule -> {
                    RuleEntity agg = ruleRepository.findById(scenarioRule.getRuleId()).orElse(null);
                    return Response.ScenarioRuleResponse.builder()
                            .ruleId(scenarioRule.getRuleId())
                            .ruleName(agg != null ? agg.getName() : null)
                            .orderNo(scenarioRule.getOrderNo())
                            .operator(scenarioRule.getOperator())

                            .thresholdCount(agg != null ? agg.getThresholdCount() : null)
                            .thresholdAmount(agg != null ? agg.getThresholdAmount() : null)
                            .aggregateOperator(agg != null && agg.getOperator() != null ? agg.getOperator().name() : null)
                            .build();
                })
                .toList();

        return Response.builder()
                .scenarioId(scenario.getScenarioId())
                .scenarioName(scenario.getScenarioName())
                .description(scenario.getDescription())
                .entityFilterJson(scenario.getEntityFilterJson())
                .riskLevelId(scenario.getRiskLevelId())
                .detectionAreaId(scenario.getDetectionAreaId())
                .primaryEntityType(scenario.getPrimaryEntityType())
                .isActive(scenario.isActive())
                // 엔진 설정 필드
                .dedupMinutes(scenario.getDedupMinutes())
                .rules(ruleInfos)
                .regDt(scenario.getRegDt())
                .build();
    }

    /**
     * 시나리오 생성 명령 검증
     */
    private void validateScenarioCommand(CreateCommand command) {
        // 기본 필드 검증
        if (command.getScenarioName() == null || command.getScenarioName().isBlank()) {
            throw new IllegalArgumentException("시나리오 이름은 필수입니다.");
        }

        // 시나리오명 중복 확인
        if (scenarioRepository.existsByScenarioName(command.getScenarioName())) {
            throw new IllegalArgumentException("이미 존재하는 시나리오명입니다: " + command.getScenarioName());
        }

        // 집계 목록 검증
        if (command.getRules() == null || command.getRules().isEmpty()) {
            throw new IllegalArgumentException("최소 1개 이상의 집계를 추가해야 합니다.");
        }

        // 각 집계 규칙 검증
        validateScenarioRules(command.getRules());
    }

    /**
     * 시나리오 수정 명령 검증 (null 필드는 기존 값 유지)
     */
    private void validateScenarioUpdateCommand(UpdateCommand command) {
        // scenarioName이 전달된 경우에만 검증
        if (command.getScenarioName() != null && command.getScenarioName().isBlank()) {
            throw new IllegalArgumentException("시나리오 이름은 빈 값일 수 없습니다.");
        }

        // 집계 목록 검증 - 전달된 경우에만
        if (command.getRules() != null) {
            // 빈 목록이 아닌 경우에만 각 규칙 검증
            if (!command.getRules().isEmpty()) {
                validateScenarioRules(command.getRules());
            }
        }
    }

    /**
     * 시나리오 집계 규칙 목록 검증
     */
    private void validateScenarioRules(List<?> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
        }

        // orderNo 중복 검증
        java.util.Set<Integer> orderNumbers = new java.util.HashSet<>();

        for (int i = 0; i < rules.size(); i++) {
            Object ruleObj = rules.get(i);

            // ScenarioRuleCommand 타입으로 변환 (CreateCommand와 UpdateCommand 모두 동일한 ScenarioRuleCommand 사용)
            if (ruleObj instanceof ScenarioRuleCommand) {
                ScenarioRuleCommand rule = (ScenarioRuleCommand) ruleObj;
                validateSingleRule(rule.getRuleId(), rule.getOrderNo(), rule.getOperator(), i, orderNumbers);
            } else if (ruleObj instanceof UpdateCommand.ScenarioRuleCommand) {
                UpdateCommand.ScenarioRuleCommand rule = (UpdateCommand.ScenarioRuleCommand) ruleObj;
                validateSingleRule(rule.getRuleId(), rule.getOrderNo(), rule.getOperator(), i, orderNumbers);
            }
        }
    }

    /**
     * 개별 시나리오 규칙 검증
     */
    private void validateSingleRule(String ruleId, Integer orderNo, ScenarioOperator operator,
                                     int index, java.util.Set<Integer> orderNumbers) {
        // ruleId 필수 확인
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("집계 ID는 필수입니다. (위치: " + (index + 1) + "번째 집계)");
        }

        // orderNo 필수 확인
        if (orderNo == null) {
            throw new IllegalArgumentException("순서 번호는 필수입니다. (집계: " + ruleId + ")");
        }

        // orderNo 양수 확인
        if (orderNo <= 0) {
            throw new IllegalArgumentException("순서 번호는 1 이상이어야 합니다. (집계: " + ruleId + ", 순서: " + orderNo + ")");
        }

        // orderNo 중복 확인
        if (orderNumbers.contains(orderNo)) {
            throw new IllegalArgumentException("중복된 순서 번호입니다: " + orderNo + " (집계: " + ruleId + ")");
        }
        orderNumbers.add(orderNo);

        // operator 검증
        // orderNo가 1이면 첫 번째 집계이므로 operator 무시 (있어도 에러 발생 안함)
        // orderNo가 2 이상이면 operator 필수
        if (orderNo > 1) {
            if (operator == null) {
                throw new IllegalArgumentException("두 번째 이후 집계는 연산자(AND/OR)가 필수입니다. (집계: " + ruleId + ", orderNo: " + orderNo + ")");
            }
        }
    }

    /**
     * 현재 사용자 ID 조회
     */
    private String getCurrentUser() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    /**
     * 변경된 필드 목록 탐지
     */
    private List<String> detectChangedFields(Response before, Response after) {
        List<String> changedFields = new java.util.ArrayList<>();

        if (!java.util.Objects.equals(before.getScenarioName(), after.getScenarioName())) {
            changedFields.add("scenarioName");
        }
        if (!java.util.Objects.equals(before.getDescription(), after.getDescription())) {
            changedFields.add("description");
        }
        if (!java.util.Objects.equals(before.getEntityFilterJson(), after.getEntityFilterJson())) {
            changedFields.add("entityFilterJson");
        }
        if (!java.util.Objects.equals(before.getIsActive(), after.getIsActive())) {
            changedFields.add("isActive");
        }
        if (!java.util.Objects.equals(before.getRiskLevelId(), after.getRiskLevelId())) {
            changedFields.add("riskLevelId");
        }
        if (!java.util.Objects.equals(before.getDetectionAreaId(), after.getDetectionAreaId())) {
            changedFields.add("detectionAreaId");
        }
        if (!java.util.Objects.equals(before.getPrimaryEntityType(), after.getPrimaryEntityType())) {
            changedFields.add("primaryEntityType");
        }
        if (!java.util.Objects.equals(before.getDedupMinutes(), after.getDedupMinutes())) {
            changedFields.add("dedupMinutes");
        }
        // 규칙 변경 여부 단순 비교 (목록 크기나 내용 변경)
        if (before.getRules() != null && after.getRules() != null) {
            if (before.getRules().size() != after.getRules().size()) {
                changedFields.add("rules");
            }
        } else if (before.getRules() != after.getRules()) {
            changedFields.add("rules");
        }

        return changedFields;
    }

    @Override
    public ScenarioDto.VisualizationResponse getScenarioVisualization(String scenarioId) {
        log.info("Getting visualization for scenario: {}", scenarioId);

        // 1. 시나리오 조회
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NoDataException("시나리오를 찾을 수 없습니다: " + scenarioId));

        // 2. 시나리오 룰 조회 (정렬된 순서대로)
        List<ScenarioRule> scenarioRules = scenarioRuleRepository.findByScenarioIdOrderByOrderNo(scenarioId);

        // 3. 각 룰의 상세 정보 + 센서 정보 조회
        List<ScenarioDto.VisualizationResponse.RuleWithSensor> rulesWithSensors = scenarioRules.stream()
                .map(scenarioRule -> {
                    // Rule 정보 조회
                    RuleEntity rule = ruleRepository.findById(scenarioRule.getRuleId())
                            .orElse(null);

                    if (rule == null) {
                        log.warn("Rule not found: {}", scenarioRule.getRuleId());
                        return null;
                    }

                    // Sensor 정보 조회 (predicate_sensor_id)
                    String sensorId = rule.getPredicateSensorId();
                    String sensorName = null;

                    if (sensorId != null) {
                        // Sensor 조회 (sensors 테이블에서 조회)
                        sensorName = sensorRepository.findById(sensorId)
                                .map(sensor -> sensor.getSensorName())
                                .orElse(sensorId);
                    }

                    return ScenarioDto.VisualizationResponse.RuleWithSensor.builder()
                            .ruleId(rule.getRuleId())
                            .ruleName(rule.getName())
                            .operator(rule.getOperator() != null ? rule.getOperator().name() : null)
                            .windowMinutes(rule.getWindowMinutes())
                            .orderNo(scenarioRule.getOrderNo())
                            .scenarioOperator(scenarioRule.getOperator())
                            .predicateSensorId(sensorId)
                            .predicateSensorName(sensorName)
                            .build();
                })
                .filter(rws -> rws != null)
                .collect(Collectors.toList());

        return ScenarioDto.VisualizationResponse.builder()
                .scenarioId(scenario.getScenarioId())
                .scenarioName(scenario.getScenarioName())
                .rules(rulesWithSensors)
                .build();
    }
}
