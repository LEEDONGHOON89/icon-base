package com.itmasters.icon.common.domain.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itmasters.icon.common.domain.rule.condition.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * RuleCondition을 JSON으로 직렬화하기 위한 Visitor
 * 각 조건 타입별로 적절한 JSON 구조로 변환
 */
@Component
@RequiredArgsConstructor
public class ConditionSerializationVisitor implements RuleConditionVisitor<ObjectNode> {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public ObjectNode visit(ComparisonCondition condition) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("fieldName", condition.getFieldName());
        node.put("operator", condition.getOperator().name());
        node.put("value", condition.getCompareValue());
        return node;
    }
    
    @Override
    public ObjectNode visit(AmountCondition condition) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("fieldName", condition.getFieldName());
        node.put("operator", condition.getOperator().name());
        node.put("value", condition.getThreshold().toString());
        return node;
    }
    
    @Override
    public ObjectNode visit(DurationCondition condition) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("fieldName", condition.getFieldName());
        node.put("operator", condition.getOperator().name());
        node.put("value", condition.getDurationExpression());
        return node;
    }
    
    @Override
    public ObjectNode visit(TimeRangeCondition condition) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("fieldName", condition.getFieldName());
        node.put("operator", condition.getOperator().name());
        node.put("value", condition.getStartHour() + "," + condition.getEndHour());
        return node;
    }
    
    @Override
    public ObjectNode visit(TimeDifferenceCondition condition) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("fieldName", condition.getFieldName() + "," + condition.getCompareFieldName());
        node.put("operator", condition.getOperator().name());
        node.put("value", condition.getThreshold().toMinutes() + "m");
        return node;
    }
    
    @Override
    public ObjectNode visit(TimeRangeWithMinutesCondition condition) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("fieldName", condition.getFieldName());
        node.put("operator", condition.getOperator().name());
        node.put("value", condition.getStartTime() + "," + condition.getEndTime());
        return node;
    }
    
    @Override
    public ObjectNode visit(CountWithinCondition condition) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("fieldName", condition.getFieldName());
        node.put("operator", condition.getOperator().name());
        node.put("value", condition.getTimeWindowMinutes() + "," + condition.getCount());
        return node;
    }
    
    @Override
    public ObjectNode visit(SumWithinCondition condition) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("fieldName", condition.getFieldName());
        node.put("operator", condition.getOperator().name());
        node.put("value", condition.getTimeWindowMinutes() + "," + condition.getAmount());
        return node;
    }
    
    @Override
    public ObjectNode visit(DistinctCountCondition condition) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("fieldName", condition.getFieldName());
        node.put("operator", condition.getOperator().name());
        node.put("value", condition.getTimeWindowMinutes() + "," + condition.getCount());
        return node;
    }

    @Override
    public ObjectNode visitBooleanCondition(BooleanCondition condition) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("fieldName", condition.getFieldName());
        node.put("operator", condition.getOperator().name());
        // Boolean 조건은 값이 없음
        node.putNull("value");
        return node;
    }
}