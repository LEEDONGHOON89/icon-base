package com.itmasters.icon.engine.processor.support;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;
import com.itmasters.icon.engine.dto.AggregationResult;
import com.itmasters.icon.common.domain.aggregate.AggregateOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 집계 연산을 수행하는 책임을 RuleProcessor에서 분리한 클래스.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationEvaluator {

    private final SensorPredicateFilter sensorPredicateFilter;

    public AggregationResult evaluate(RuleEntity definition,
                                      List<EngineEventStreamEntity> events,
                                      LocalDateTime endTime) {
        BigDecimal matchedValue = BigDecimal.ZERO;
        boolean pass = false;
        LocalDateTime detectedAt = null;
        Long anchorMappedStorageId = null;
        String transactionId = null;
        String actualGroupKey = null;

        String aggregationField = null;
        String[] groupByFields = definition.getGroupByFields();
        AggregateOperator operator = definition.getOperator();
        if (operator == AggregateOperator.SUM_WITHIN
                || operator == AggregateOperator.AVG_WITHIN
                || operator == AggregateOperator.MAX_WITHIN
                || operator == AggregateOperator.MIN_WITHIN) {
            aggregationField = definition.getAggregationField();
        }

        boolean hasGrouping = groupByFields != null && groupByFields.length > 0;

        switch (operator) {
            case COUNT_WITHIN -> {
                List<EngineEventStreamEntity> filtered = sensorPredicateFilter.filter(
                        definition.getPredicateSensorId(), events, definition);
                if (!hasGrouping) {
                    matchedValue = BigDecimal.valueOf(filtered.size());
                    if (definition.getThresholdCount() != null) {
                        pass = matchedValue.compareTo(definition.getThresholdCount()) >= 0;
                    }
                    if (!filtered.isEmpty()) {
                        EngineEventStreamEntity last = filtered.get(filtered.size() - 1);
                        detectedAt = last.getEventDt();
                        anchorMappedStorageId = last.getMappedDataStorageId();
                        transactionId = last.getTransactionId();
                    }
                } else {
                    CountAggregationResult result = evaluateCountWithGrouping(
                            filtered, groupByFields, definition.getThresholdCount());
                    pass = result.pass();
                    matchedValue = result.matchedValue();
                    detectedAt = result.detectedAt();
                    anchorMappedStorageId = result.anchorMappedStorageId();
                    transactionId = result.transactionId();
                    actualGroupKey = result.actualGroupKey();
                }
            }
            case ABSENCE_WITHIN -> {
                List<EngineEventStreamEntity> filtered = sensorPredicateFilter.filter(
                        definition.getPredicateSensorId(), events, definition);
                pass = filtered == null || filtered.isEmpty();
                matchedValue = BigDecimal.valueOf(filtered == null ? 0 : filtered.size());
                detectedAt = endTime;
            }
            case SUM_WITHIN -> {
                if (aggregationField == null) {
                    log.error("aggregationField is not defined for SUM_WITHIN in RuleEntity: {}",
                            definition.getRuleId());
                    break;
                }
                SumAggregationResult result = evaluateSum(definition, events, aggregationField, hasGrouping);
                pass = result.pass();
                matchedValue = result.matchedValue();
                detectedAt = result.detectedAt();
                anchorMappedStorageId = result.anchorMappedStorageId();
                transactionId = result.transactionId();
                actualGroupKey = result.actualGroupKey();
            }
            case AVG_WITHIN -> {
                if (aggregationField == null) {
                    log.error("aggregationField is not defined for AVG_WITHIN in RuleEntity: {}",
                            definition.getRuleId());
                    break;
                }
                List<EngineEventStreamEntity> filtered = sensorPredicateFilter.filter(
                        definition.getPredicateSensorId(), events, definition);
                final String aggFieldForAvg = aggregationField;
                List<BigDecimal> values = filtered.stream()
                        .map(event -> getNumericFieldValue(event.getEventData(), aggFieldForAvg))
                        .filter(val -> val.compareTo(BigDecimal.ZERO) != 0)
                        .collect(Collectors.toList());
                if (!values.isEmpty()) {
                    BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                    matchedValue = sum.divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL128);
                }
                if (definition.getThresholdAmount() != null) {
                    pass = matchedValue.compareTo(definition.getThresholdAmount()) >= 0;
                }
                if (!filtered.isEmpty()) {
                    EngineEventStreamEntity anchorEvt = filtered.get(filtered.size() - 1);
                    detectedAt = anchorEvt.getEventDt();
                    anchorMappedStorageId = anchorEvt.getMappedDataStorageId();
                    transactionId = anchorEvt.getTransactionId();
                }
            }
            case MAX_WITHIN -> {
                if (aggregationField == null) {
                    log.error("aggregationField is not defined for MAX_WITHIN in RuleEntity: {}",
                            definition.getRuleId());
                    break;
                }
                List<EngineEventStreamEntity> filtered = sensorPredicateFilter.filter(
                        definition.getPredicateSensorId(), events, definition);
                final String aggFieldForMax = aggregationField;
                matchedValue = filtered.stream()
                        .map(event -> getNumericFieldValue(event.getEventData(), aggFieldForMax))
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                if (definition.getThresholdAmount() != null) {
                    pass = matchedValue.compareTo(definition.getThresholdAmount()) >= 0;
                }
                if (!filtered.isEmpty()) {
                    EngineEventStreamEntity anchorEvt = filtered.get(filtered.size() - 1);
                    detectedAt = anchorEvt.getEventDt();
                    anchorMappedStorageId = anchorEvt.getMappedDataStorageId();
                    transactionId = anchorEvt.getTransactionId();
                }
            }
            case MIN_WITHIN -> {
                if (aggregationField == null) {
                    log.error("aggregationField is not defined for MIN_WITHIN in RuleEntity: {}",
                            definition.getRuleId());
                    break;
                }
                List<EngineEventStreamEntity> filtered = sensorPredicateFilter.filter(
                        definition.getPredicateSensorId(), events, definition);
                final String aggFieldForMax = aggregationField;
                matchedValue = filtered.stream()
                        .map(event -> getNumericFieldValue(event.getEventData(), aggFieldForMax))
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                if (definition.getThresholdAmount() != null) {
                    pass = matchedValue.compareTo(definition.getThresholdAmount()) <= 0;
                }
                if (!filtered.isEmpty()) {
                    EngineEventStreamEntity anchorEvt = filtered.get(filtered.size() - 1);
                    detectedAt = anchorEvt.getEventDt();
                    anchorMappedStorageId = anchorEvt.getMappedDataStorageId();
                    transactionId = anchorEvt.getTransactionId();
                }
            }
            case DISTINCT_COUNT_WITHIN -> {
                if (aggregationField == null) {
                    log.error("aggregationField is not defined for DISTINCT_COUNT_WITHIN in RuleEntity: {}",
                            definition.getRuleId());
                    break;
                }
                List<EngineEventStreamEntity> filtered = sensorPredicateFilter.filter(
                        definition.getPredicateSensorId(), events, definition);
                final String aggFieldForDistinct = aggregationField;
                matchedValue = filtered.stream()
                        .map(event -> getStringFieldValue(event.getEventData(), aggFieldForDistinct))
                        .filter(Objects::nonNull)
                        .distinct()
                        .count() > 0 ? BigDecimal.valueOf(
                        filtered.stream()
                                .map(event -> getStringFieldValue(event.getEventData(), aggFieldForDistinct))
                                .filter(Objects::nonNull)
                                .distinct()
                                .count()) : BigDecimal.ZERO;
                if (definition.getThresholdCount() != null) {
                    pass = matchedValue.compareTo(definition.getThresholdCount()) >= 0;
                }
                if (!filtered.isEmpty()) {
                    EngineEventStreamEntity anchorEvt = filtered.get(filtered.size() - 1);
                    detectedAt = anchorEvt.getEventDt();
                    anchorMappedStorageId = anchorEvt.getMappedDataStorageId();
                    transactionId = anchorEvt.getTransactionId();
                }
            }
            case SEQUENCE_WITHIN -> {
                // SEQUENCE 처리 로직 복사 필요 시 여기에 이동
                log.warn("SEQUENCE_WITHIN 연산은 아직 AggregationEvaluator로 이동되지 않았습니다.");
            }
            default -> log.warn("Unsupported operator: {}", operator);
        }

        return AggregationResult.builder()
                .pass(pass)
                .matchedValue(matchedValue)
                .detectedAt(detectedAt)
                .actualGroupKey(actualGroupKey)
                .anchorMappedStorageId(anchorMappedStorageId)
                .transactionId(transactionId)
                .build();
    }

    private CountAggregationResult evaluateCountWithGrouping(List<EngineEventStreamEntity> events,
                                                             String[] groupByFields,
                                                             BigDecimal thresholdCount) {
        Map<String, List<EngineEventStreamEntity>> grouped = events.stream()
                .collect(Collectors.groupingBy(e -> buildGroupKey(e, groupByFields)));

        int maxCount = 0;
        LocalDateTime candidateAnchor = null;
        String candidateGroupKey = null;
        EngineEventStreamEntity candidateEvent = null;
        boolean pass = false;

        for (Map.Entry<String, List<EngineEventStreamEntity>> entry : grouped.entrySet()) {
            List<EngineEventStreamEntity> list = entry.getValue();
            int count = list.size();
            if (thresholdCount != null && count >= thresholdCount.intValue()) {
                EngineEventStreamEntity lastEvent = list.stream()
                        .max(Comparator.comparing(EngineEventStreamEntity::getEventDt))
                        .orElse(null);
                if (lastEvent != null &&
                        (candidateAnchor == null || lastEvent.getEventDt().isAfter(candidateAnchor))) {
                    candidateAnchor = lastEvent.getEventDt();
                    candidateGroupKey = entry.getKey();
                    candidateEvent = lastEvent;
                }
                pass = true;
            }
            if (count > maxCount) maxCount = count;
        }

        Long anchorMappedStorageId = candidateEvent != null ? candidateEvent.getMappedDataStorageId() : null;
        String transactionId = candidateEvent != null ? candidateEvent.getTransactionId() : null;

        return new CountAggregationResult(
                pass,
                BigDecimal.valueOf(maxCount),
                candidateAnchor,
                anchorMappedStorageId,
                transactionId,
                candidateGroupKey
        );
    }

    private SumAggregationResult evaluateSum(RuleEntity definition,
                                             List<EngineEventStreamEntity> events,
                                             String aggregationField,
                                             boolean hasGrouping) {
        List<EngineEventStreamEntity> filtered = sensorPredicateFilter.filter(
                definition.getPredicateSensorId(), events, definition);
        final String aggField = aggregationField;

        if (!hasGrouping) {
            BigDecimal sum = filtered.stream()
                    .map(event -> getNumericFieldValue(event.getEventData(), aggField))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean pass = definition.getThresholdAmount() == null
                    || sum.compareTo(definition.getThresholdAmount()) >= 0;
            EngineEventStreamEntity anchorEvt = filtered.isEmpty() ? null : filtered.get(filtered.size() - 1);
            return new SumAggregationResult(pass, sum,
                    anchorEvt != null ? anchorEvt.getEventDt() : null,
                    anchorEvt != null ? anchorEvt.getMappedDataStorageId() : null,
                    anchorEvt != null ? anchorEvt.getTransactionId() : null,
                    null);
        }

        final String[] gbFields = definition.getGroupByFields();
        Map<String, List<EngineEventStreamEntity>> grouped = filtered.stream()
                .collect(Collectors.groupingBy(e -> buildGroupKey(e, gbFields)));

        BigDecimal maxSum = BigDecimal.ZERO;
        LocalDateTime candidateAnchor = null;
        String candidateGroupKey = null;
        EngineEventStreamEntity candidateEvent = null;
        boolean pass = false;

        for (Map.Entry<String, List<EngineEventStreamEntity>> entry : grouped.entrySet()) {
            List<EngineEventStreamEntity> list = entry.getValue();
            BigDecimal groupSum = list.stream()
                    .map(event -> getNumericFieldValue(event.getEventData(), aggField))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            boolean countOk = definition.getThresholdCount() == null
                    || list.size() >= definition.getThresholdCount().intValue();
            boolean amountOk = definition.getThresholdAmount() == null
                    || groupSum.compareTo(definition.getThresholdAmount()) >= 0;

            if (countOk && amountOk) {
                EngineEventStreamEntity lastEvent = list.stream()
                        .max(Comparator.comparing(EngineEventStreamEntity::getEventDt))
                        .orElse(null);
                if (lastEvent != null &&
                        (candidateAnchor == null || lastEvent.getEventDt().isAfter(candidateAnchor))) {
                    candidateAnchor = lastEvent.getEventDt();
                    candidateGroupKey = entry.getKey();
                    candidateEvent = lastEvent;
                    maxSum = groupSum;
                }
                pass = true;
            }
            if (groupSum.compareTo(maxSum) > 0 && !pass) {
                maxSum = groupSum;
            }
        }

        return new SumAggregationResult(pass, maxSum, candidateAnchor,
                candidateEvent != null ? candidateEvent.getMappedDataStorageId() : null,
                candidateEvent != null ? candidateEvent.getTransactionId() : null,
                candidateGroupKey);
    }

    private String buildGroupKey(EngineEventStreamEntity event, String[] groupByFields) {
        List<String> keyParts = new ArrayList<>();
        for (String field : groupByFields) {
            Object value = event.getEventData().get(field);
            keyParts.add(value == null ? "" : value.toString().trim());
        }
        return String.join("|", keyParts);
    }

    private BigDecimal getNumericFieldValue(Map<String, Object> eventData, String fieldName) {
        Object value = eventData.get(fieldName);
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        if (value instanceof String str) {
            try {
                return new BigDecimal(str);
            } catch (NumberFormatException e) {
                log.warn("Cannot parse '{}' as number for field '{}'", value, fieldName);
            }
        }
        return BigDecimal.ZERO;
    }

    private String getStringFieldValue(Map<String, Object> eventData, String fieldName) {
        Object value = eventData.get(fieldName);
        return value != null ? value.toString() : null;
    }

    private record CountAggregationResult(boolean pass,
                                          BigDecimal matchedValue,
                                          LocalDateTime detectedAt,
                                          Long anchorMappedStorageId,
                                          String transactionId,
                                          String actualGroupKey) {
    }

    private record SumAggregationResult(boolean pass,
                                        BigDecimal matchedValue,
                                        LocalDateTime detectedAt,
                                        Long anchorMappedStorageId,
                                        String transactionId,
                                        String actualGroupKey) {
    }
}
