package com.itmasters.icon.api.rule.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.common.domain.rule.RuleConditionFactory;
import com.itmasters.icon.common.domain.rule.condition.RuleCondition;
import com.itmasters.icon.api.rule.dto.SensorDto;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.api.common.exception.NoDataException;
import com.itmasters.icon.api.rule.application.port.out.SensorRepository;
import com.itmasters.icon.api.rule.adapter.persistence.entity.SensorEntity;
import com.itmasters.icon.api.audit.application.service.DetectionConfigAuditService;
import com.itmasters.icon.api.audit.domain.ConfigAuditTargetType;
import com.itmasters.icon.api.audit.util.RequestUtils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorService {

    private final SensorRepository sensorRepository;
    private final ObjectMapper objectMapper;
    private final DetectionConfigAuditService auditService;

    public List<SensorDto.Info> findAll() {
        return sensorRepository.findAll().stream()
                .map(entity -> SensorDto.Info.from(entity, objectMapper))
                .collect(Collectors.toList());
    }

    public com.itmasters.icon.api.common.response.ResponseList<SensorDto.Response> findPaged(String q, Boolean active, String sort, String dir, int page, int size) {
        int p = page < 1 ? 1 : page;
        int s = size < 1 ? 20 : Math.min(size, 1000);
        int offset = (p - 1) * s;
        List<com.itmasters.icon.api.rule.adapter.persistence.entity.SensorEntity> rows =
                ((com.itmasters.icon.api.rule.adapter.persistence.repository.SensorRepositoryImpl) sensorRepository)
                        .findFiltered(q, active, sort, dir, offset, s);
        long total = ((com.itmasters.icon.api.rule.adapter.persistence.repository.SensorRepositoryImpl) sensorRepository)
                .countFiltered(q, active);
        List<SensorDto.Response> data = rows.stream().map(e -> SensorDto.Response.from(SensorDto.Info.from(e, objectMapper))).toList();
        return new com.itmasters.icon.api.common.response.ResponseList<>(total, data);
    }

    public SensorDto.Info findById(String sensorId) {
        SensorEntity sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new NoDataException("Sensor not found with id: " + sensorId));
        return SensorDto.Info.from(sensor, objectMapper);
    }

    // category-based filtering removed in v4

    public List<SensorDto.Info> findActiveSensors() {
        return sensorRepository.findByIsActive(true).stream()
                .map(entity -> SensorDto.Info.from(entity, objectMapper))
                .collect(Collectors.toList());
    }

    @Transactional
    public SensorDto.Info createSensor(SensorDto.CreateCommand command) {
        // 이름에 시퀀스/기간 표현이 포함된 경우 안내 로그 (집계로 모델링 권장)
        if (isSequenceLikeName(command.getSensorName())) {
            log.warn("Sensor name suggests a sequence/window scenario. Consider modeling as rule: name='{}'", command.getSensorName());
        }
        // derive domain when not provided
        com.itmasters.icon.common.domain.RuleDomain domain = command.getDomain();
        if (domain == null) {
            String fieldFromPayload = command.getFieldName();
            if ((fieldFromPayload == null || fieldFromPayload.isBlank()) && command.getWhereJson() != null) {
                fieldFromPayload = extractFirstFieldFromWhereJson(command.getWhereJson());
            }
            domain = deriveDomainFromField(fieldFromPayload);
        }

        // RuleCondition 생성 (builder 모드인 경우에만)
        RuleCondition condition = null;
        if (command.getFieldName() != null && command.getOperator() != null) {
            condition = RuleConditionFactory.createCondition(
                    command.getFieldName(),
                    command.getOperator(),
                    command.getValue()
            );
        }

        // 엔티티 생성 (operator/condition이 없어도 허용)
        SensorEntity sensor = SensorEntity.createWithDomain(
                command.getSensorName(),
                domain,
                null,      // operator는 별도 컬럼 미사용
                condition,
                objectMapper
        );

        // where_json 제공 시 우선 적용
        applyWhereJson(sensor, command.getWhereJson());

        // 설명 추가 (옵션)
        if (command.getDescription() != null) {
            sensor.updateDescription(command.getDescription());
        }

        // 센서 ID 할당 (사용자 입력)
        String sensorId = command.getSensorId();
        if (sensorId == null || sensorId.trim().isEmpty()) {
            throw new IllegalArgumentException("센서 ID는 필수입니다");
        }

        // ID 중복 검증
        if (sensorRepository.existsById(sensorId)) {
            throw new IllegalArgumentException("이미 존재하는 센서 ID입니다: " + sensorId);
        }

        sensor.assignId(sensorId);

        // 저장
        SensorEntity savedSensor = sensorRepository.save(sensor);

        // 감사 로그 기록
        String currentUser = getCurrentUser();
        auditService.logCreate(
                ConfigAuditTargetType.SENSOR,
                savedSensor.getSensorId(),
                savedSensor.getSensorName(),
                SensorDto.Info.from(savedSensor, objectMapper),
                currentUser,
                RequestUtils.getClientIpAddress()
        );

        return SensorDto.Info.from(savedSensor, objectMapper);
    }


    @Transactional
    public SensorDto.Info updateSensor(SensorDto.UpdateCommand command, String userId) {
        if (command.getSensorName() != null && isSequenceLikeName(command.getSensorName())) {
            log.warn("Sensor name suggests a sequence/window scenario. Consider modeling as rule: name='{}'", command.getSensorName());
        }

        SensorEntity sensor = sensorRepository.findById(command.getRuleId())
                .orElseThrow(() -> new NoDataException("Sensor not found with id: " + command.getRuleId()));

        // 감사 로그용 변경 전 상태 저장
        SensorDto.Info beforeSnapshot = SensorDto.Info.from(sensor, objectMapper);
        List<String> changedFields = new java.util.ArrayList<>();

        boolean isChange = false;

        // 조건 업데이트 (builder 모드)
        if (command.getFieldName() != null || command.getOperator() != null || command.getValue() != null) {
            RuleCondition currentCondition = sensor.getConditionAsObject(objectMapper);
            String fieldName = command.getFieldName() != null ? command.getFieldName() : currentCondition.getFieldName();
            RuleOperator operator = command.getOperator() != null ? command.getOperator() : sensor.getOperator();
            Object value = command.getValue() != null ? command.getValue() : currentCondition.getValue();

            RuleCondition newCondition = RuleConditionFactory.createCondition(fieldName, operator, value);

            if (sensor.isChangeCondition(newCondition, objectMapper)) {
                sensor.updateCondition(newCondition, objectMapper);
                changedFields.add("condition");
                isChange = true;
            }
        }

        // 센서명과 카테고리 업데이트
        if (command.getSensorName() != null && sensor.getSensorName().equals(command.getSensorName()) == false) {
            sensor.updateName(command.getSensorName());
            changedFields.add("sensorName");
            isChange = true;
        }

        if (command.getCategory() != null && sensor.getCategory().equals(command.getCategory()) == false) {
            sensor.updateCategory(command.getCategory());
            changedFields.add("category");
            isChange = true;
        }

        // 새로운 domain, operator 필드 업데이트
        if (command.getDomain() != null && (sensor.getDomain() == null || !sensor.getDomain().equals(command.getDomain()))) {
            // transient 업데이트 (응답 표시용)
            sensor.updateDomain(command.getDomain());
            changedFields.add("domain");
        }

        // 설명 업데이트
        if (command.getDescription() != null) {
            sensor.updateDescription(command.getDescription());
            changedFields.add("description");
        }

        // 활성화 상태 업데이트
        if (command.getIsActive() != null) {
            if (command.getIsActive()) {
                sensor.activate();
            } else {
                sensor.deactivate();
            }
            changedFields.add("isActive");
        }

        // Anchor/Where update (optional) — 제공 시 우선 적용
        applyWhereJson(sensor, command.getWhereJson());

        // 도메인 미지정 상태에서 field 입력이 바뀐 경우 도메인 자동 유추
        if (command.getDomain() == null && command.getFieldName() != null) {
            com.itmasters.icon.common.domain.RuleDomain d = deriveDomainFromField(command.getFieldName());
            if (d != null && (sensor.getDomain() == null || !sensor.getDomain().equals(d))) {
                sensor.updateDomain(d);
                isChange = true;
            }
        }

        SensorEntity updatedSensor = sensorRepository.save(sensor);

        // 감사 로그 기록 (변경이 있는 경우에만)
        if (!changedFields.isEmpty()) {
            SensorDto.Info afterSnapshot = SensorDto.Info.from(updatedSensor, objectMapper);
            auditService.logUpdate(
                    ConfigAuditTargetType.SENSOR,
                    updatedSensor.getSensorId(),
                    updatedSensor.getSensorName(),
                    beforeSnapshot,
                    afterSnapshot,
                    changedFields,
                    userId,
                    RequestUtils.getClientIpAddress()
            );
        }

        return SensorDto.Info.from(updatedSensor, objectMapper);
    }

    private boolean isSequenceLikeName(String name) {
        if (name == null) return false;
        String n = name.toLowerCase(Locale.ROOT);
        // 한국어 패턴: "후", "분 내", "이내"  / 영문: "after", "within"
        return (n.contains("후") && (n.contains("분 내") || n.contains("이내")))
                || n.contains("within")
                || n.contains("after");
    }


    @Transactional
    public void activateSensor(String sensorId) {
        SensorEntity sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new NoDataException("Sensor not found with id: " + sensorId));

        SensorDto.Info beforeSnapshot = SensorDto.Info.from(sensor, objectMapper);
        sensor.activate();
        SensorEntity savedSensor = sensorRepository.save(sensor);

        // 감사 로그 기록
        String currentUser = getCurrentUser();
        auditService.logUpdate(
                ConfigAuditTargetType.SENSOR,
                savedSensor.getSensorId(),
                savedSensor.getSensorName(),
                beforeSnapshot,
                SensorDto.Info.from(savedSensor, objectMapper),
                List.of("isActive"),
                currentUser,
                RequestUtils.getClientIpAddress()
        );
    }

    @Transactional
    public void deactivateSensor(String sensorId) {
        SensorEntity sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new NoDataException("Sensor not found with id: " + sensorId));

        SensorDto.Info beforeSnapshot = SensorDto.Info.from(sensor, objectMapper);
        sensor.deactivate();
        SensorEntity savedSensor = sensorRepository.save(sensor);

        // 감사 로그 기록
        String currentUser = getCurrentUser();
        auditService.logUpdate(
                ConfigAuditTargetType.SENSOR,
                savedSensor.getSensorId(),
                savedSensor.getSensorName(),
                beforeSnapshot,
                SensorDto.Info.from(savedSensor, objectMapper),
                List.of("isActive"),
                currentUser,
                RequestUtils.getClientIpAddress()
        );
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // === Anchor/Where linter and setter ===
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.itmasters.icon.api.rule.validation.RuleLinter ruleLinter;

    private void applyWhereJson(SensorEntity sensor, String whereJson) {
        if (whereJson == null || whereJson.isBlank()) return;
        if (ruleLinter != null) {
            ruleLinter.validateWhereJson(whereJson);
        }
        sensor.updateWhereJson(whereJson);
    }
    
    /**
     * 연산자 이름으로 RuleOperator 찾기 (하위 호환성을 위해 유지)
     */
    private RuleOperator findOperatorByName(String name) {
        try {
            return RuleOperator.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown operator: " + name);
        }
    }
    
    // === Helpers ===
    private com.itmasters.icon.common.domain.RuleDomain deriveDomainFromField(String fieldName) {
        if (fieldName == null) return null;
        try {
            com.itmasters.icon.common.domain.rule.RuleField rf = com.itmasters.icon.common.domain.rule.RuleField.valueOf(fieldName.toUpperCase());
            com.itmasters.icon.common.domain.rule.FieldCategory c = rf.getCategory();
            return switch (c) {
                case TRANSACTION, OPEN_BANKING, LOAN, BLACKLIST -> com.itmasters.icon.common.domain.RuleDomain.FINANCIAL_TRANSACTION;
                case ACCESS, AUTH, SECURITY, ACTIVITY -> com.itmasters.icon.common.domain.RuleDomain.LOGIN;
                case DEVICE -> com.itmasters.icon.common.domain.RuleDomain.DEVICE_SECURITY;
                case ACCOUNT -> com.itmasters.icon.common.domain.RuleDomain.ACCOUNT;
                case CUSTOMER -> com.itmasters.icon.common.domain.RuleDomain.CUSTOMER;
                case ATM -> com.itmasters.icon.common.domain.RuleDomain.ATM;
                default -> null;
            };
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String extractFirstFieldFromWhereJson(String whereJson) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(whereJson);
            if (node.isArray() && node.size() > 0) {
                node = node.get(0);
            }
            // fieldName으로 통일 (하위 호환성을 위해 field도 시도)
            if (node.has("fieldName")) {
                return node.get("fieldName").asText();
            } else if (node.has("field")) {
                return node.get("field").asText();
            }
        } catch (Exception ignore) { }
        return null;
    }
}
