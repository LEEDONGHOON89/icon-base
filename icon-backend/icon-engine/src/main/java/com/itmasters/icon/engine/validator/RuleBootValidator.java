package com.itmasters.icon.engine.validator;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineRuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineRuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleBootValidator {

    private final EngineRuleRepository engineRuleRepository;

    @Value("${engine.rules.validateOnStartup:true}")
    private boolean validateOnStartup;

    @PostConstruct
    public void validate() {
        if (!validateOnStartup) return;
        List<EngineRuleEntity> rules = engineRuleRepository.findAllActive();
        int missingFilter = 0;
        for (EngineRuleEntity r : rules) {
            // 1. where_json 체크
            boolean hasWhereJson = r.getWhereJson() != null && !r.getWhereJson().isBlank();
            // 2. predicate_sensor_id 또는 anchor_sensor_id (일반 집계 룰)
            boolean hasSensorFilter = isNotBlank(r.getPredicateSensorId()) || isNotBlank(r.getAnchorSensorId());
            // 3. prev_sensor_id + next_sensor_id (SEQUENCE 룰)
            boolean hasSequenceSensors = isNotBlank(r.getPrevSensorId()) && isNotBlank(r.getNextSensorId());

            // 위 조건 중 하나라도 만족하면 유효한 룰
            if (!hasWhereJson && !hasSensorFilter && !hasSequenceSensors) {
                missingFilter++;
                log.error("[RULE] No filter defined - ruleId: {}, name: {}, operator: {}",
                        r.getRuleId(), r.getRuleName(), r.getOperatorName());
            }
        }
        if (missingFilter == 0) {
            log.info("[RULE] Startup validation passed - total: {}", rules.size());
        } else {
            log.warn("[RULE] Startup validation issues - total: {}, missing_filter: {}",
                    rules.size(), missingFilter);
        }
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
