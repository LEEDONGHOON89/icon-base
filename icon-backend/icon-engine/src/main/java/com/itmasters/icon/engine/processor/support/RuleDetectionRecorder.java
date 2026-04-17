package com.itmasters.icon.engine.processor.support;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectRuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.DetectRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleDetectionRecorder {
    private final DetectRuleRepository detectRuleRepository;

    public int save(DetectRuleEntity entity) {
        if (entity == null) {
            return 0;
        }
        detectRuleRepository.save(entity);
        log.info("[RULE-SAVE] DetectRuleEntity saved: ruleId={}, groupKey={}", entity.getRuleId(), entity.getGroupKey());
        return 1;
    }
}
