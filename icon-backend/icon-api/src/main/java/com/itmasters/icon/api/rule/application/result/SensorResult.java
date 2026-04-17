package com.itmasters.icon.api.rule.application.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.api.rule.adapter.persistence.entity.SensorEntity;
import com.itmasters.icon.common.domain.rule.condition.RuleCondition;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SensorResult {
    private final String sensorId;
    private final String sensorName;
    private final String category;
    private final String categoryLabel;
    private final String fieldName;
    private final String operator;
    private final String operatorLabel;
    private final String operatorSymbol ;
    private final Object value;
    private final String description;
    private final Integer version;
    private final Boolean isActive;

    public static SensorResult from(SensorEntity sensor, ObjectMapper objectMapper) {
        RuleCondition condition = sensor.getConditionAsObject(objectMapper);
        Object conditionValue = extractConditionValue(condition);

        return new SensorResult(
                sensor.getSensorId(),
                sensor.getSensorName(),
                sensor.getCategory().name(),
                sensor.getCategory().getLabel(),
                condition.getFieldName(),
                condition.getOperator().name(), // name() 대신 getCode() 사용
                condition.getOperator().getLabel(),
                condition.getOperator().getSymbol(),
                conditionValue,
                sensor.getDescription(),
                sensor.getVersion(),
                sensor.getIsActive()
        );
    }

    private static Object extractConditionValue(RuleCondition condition) {
        // polymorphism을 활용하여 getConditionValue() 호출
        return condition.getValue();
    }
}