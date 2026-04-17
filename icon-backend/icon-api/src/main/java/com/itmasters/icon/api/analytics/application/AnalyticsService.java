package com.itmasters.icon.api.analytics.application;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectRuleEntity;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectScenarioEntity;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiEventStreamEntity;
import com.itmasters.icon.api.analytics.adapter.out.persistence.repository.DetectRuleRepository;
import com.itmasters.icon.api.analytics.adapter.out.persistence.repository.DetectScenarioRepository;
import com.itmasters.icon.api.analytics.adapter.out.persistence.repository.EventStreamRepository;
import com.itmasters.icon.api.analytics.adapter.out.persistence.repository.DetectRuleReadRepository;
import com.itmasters.icon.api.analytics.adapter.out.persistence.repository.EntityHistoryRepository;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectRuleEntity;
import com.itmasters.icon.api.analytics.dto.DetectRuleDto;
import com.itmasters.icon.api.analytics.dto.DetectScenarioDto;
import com.itmasters.icon.api.analytics.dto.DetectedRuleDto;
import com.itmasters.icon.api.analytics.dto.EntityHistoryDto;
import com.itmasters.icon.api.analytics.dto.EntityProfileDto;
import com.itmasters.icon.api.analytics.dto.EventStreamDto;
import com.itmasters.icon.engine.adapter.out.persistence.repository.JpaRuleRepository;
import com.itmasters.icon.api.entityattribute.adapter.out.persistence.repository.EntityAttributeJpaRepository;
import com.itmasters.icon.api.entityattribute.adapter.out.persistence.entity.ApiEntityAttributeEntity;
import com.itmasters.icon.api.rule.adapter.persistence.repository.SensorRepositoryImpl;
import com.itmasters.icon.api.rule.adapter.persistence.entity.SensorEntity;
import com.itmasters.icon.api.analytics.dto.EntityGraphDto;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityRelationRepository;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityRelationEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.DetectionAreaRepository;
import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectionAreaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EventStreamRepository eventStreamRepository;
    private final DetectRuleRepository detectRuleRepository;
    private final DetectScenarioRepository detectScenarioRepository;
    private final DetectRuleReadRepository detectRuleReadRepository;
    private final EntityHistoryRepository entityHistoryRepository;
    @Autowired
    private com.itmasters.icon.api.scenario.adapter.persistence.repository.JpaScenarioRepository jpaScenarioRepository;
    @Autowired
    private JpaRuleRepository ruleRepository;
    @Autowired
    private SensorRepositoryImpl sensorRepository;
    @Autowired
    private com.itmasters.icon.api.scenario.adapter.persistence.repository.JpaScenarioAggregateRepository jpaScenarioAggregateRepository;
    @Autowired
    private EntityAttributeJpaRepository entityAttributeJpaRepository;
    @Autowired
    private EntityRelationRepository entityRelationRepository;
    @Autowired
    private DetectionAreaRepository detectionAreaRepository;

    public List<EventStreamDto> getEventStream(String groupKey, LocalDateTime start, LocalDateTime end, int limit) {
        List<ApiEventStreamEntity> rows = eventStreamRepository.findByGroupKeyBetween(groupKey, start, end, limit);
        return rows.stream()
                .map(e -> EventStreamDto.builder()
                        .groupKey(groupKey)  // 파라미터로 받은 groupKey 사용 (event_stream_groups를 통해 필터링됨)
                        .eventDt(e.getEventDt())
                        .eventData(e.getEventData())
                        .build())
                .collect(Collectors.toList());
    }

    public List<DetectRuleDto> getAggregates(String groupKey, LocalDateTime start, LocalDateTime end, Integer limit) {
        List<ApiDetectRuleEntity> rows = (groupKey != null && !groupKey.isBlank())
                ? detectRuleRepository.findByGroupKeyBetween(groupKey, start, end)
                : detectRuleRepository.findBetween(start, end, limit != null ? limit : 200);
        // 집계 이름 매핑
        var ids = rows.stream().map(ApiDetectRuleEntity::getRuleId).distinct().toList();
        var nameMap = ruleRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity::getRuleId,
                        com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity::getName
                ));

        return rows.stream()
                .map(a -> DetectRuleDto.builder()
                        .groupKey(a.getGroupKey())
                        .ruleId(a.getRuleId())
                        .ruleName(nameMap.get(a.getRuleId()))
                        .operator(a.getOperator())
                        .windowMinutes(a.getWindowMinutes())
                        .matchedCount(a.getMatchedCount())
                        .thresholdCount(a.getThresholdCount())
                        .detectedAt(a.getDetectedDt())
                        .mappedStorageId(a.getMappedStorageId())
                        .build())
                .collect(Collectors.toList());
    }

    public List<DetectScenarioDto> getScenarios(String groupKey, LocalDateTime start, LocalDateTime end, Integer limit) {
        List<ApiDetectScenarioEntity> rows;
        if (groupKey != null && !groupKey.isBlank()) {
            rows = (start != null && end != null)
                    ? detectScenarioRepository.findByGroupKeyBetween(groupKey, start, end)
                    : detectScenarioRepository.findRecentByGroupKey(groupKey, limit != null ? limit : 50);
        } else {
            rows = (start != null && end != null)
                    ? detectScenarioRepository.findBetween(start, end, limit != null ? limit : 200)
                    : detectScenarioRepository.findRecent(limit != null ? limit : 50);
        }
        // 시나리오 정보 조회 (이름 + 탐지영역)
        var scenarioIds = rows.stream().map(ApiDetectScenarioEntity::getScenarioId).distinct().toList();
        var scenarioEntities = jpaScenarioRepository.findAllById(scenarioIds);
        var scnNameMap = scenarioEntities.stream()
                .collect(Collectors.toMap(
                        com.itmasters.icon.api.scenario.adapter.persistence.entity.ScenarioEntity::getScenarioId,
                        com.itmasters.icon.api.scenario.adapter.persistence.entity.ScenarioEntity::getScenarioName
                ));
        var scnDetectionAreaMap = scenarioEntities.stream()
                .collect(Collectors.toMap(
                        com.itmasters.icon.api.scenario.adapter.persistence.entity.ScenarioEntity::getScenarioId,
                        s -> s.getDetectionAreaId() != null ? s.getDetectionAreaId() : "",
                        (a, b) -> a
                ));

        // 탐지영역명 매핑
        var detectionAreaIds = scnDetectionAreaMap.values().stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        var detectionAreaNameMap = detectionAreaRepository.findAllById(detectionAreaIds).stream()
                .collect(Collectors.toMap(
                        DetectionAreaEntity::getDetectionAreaId,
                        DetectionAreaEntity::getAreaName
                ));

        return rows.stream()
                .map(s -> {
                    // scenario_aggregates 테이블에서 집계 목록 조회
                    var scenarioAggregates = jpaScenarioAggregateRepository.findByScenarioId(s.getScenarioId());
                    int total = scenarioAggregates != null ? scenarioAggregates.size() : 0;
                    int passed = 0;
                    if (total > 0) {
                        var aggIds = scenarioAggregates.stream()
                                .map(com.itmasters.icon.entity.ScenarioAggregateEntity::getAggregateId)
                                .distinct()
                                .toList();
                        // FIX: Find aggregates by group_key regardless of exec_ds_mp_id
                        var detectedAggRows = detectRuleRepository.findByGroupKeyAndRuleIds(s.getGroupKey(), aggIds);
                        java.util.Set<String> passedIds = detectedAggRows.stream()
                                .filter(r -> {
                                    // windowStart가 null이면 시간 필터 생략 (엔진에서 이미 평가됨)
                                    if (s.getWindowStart() == null) {
                                        return true;
                                    }
                                    // windowStart가 있으면 시간 필터 적용
                                    return r.getDetectedDt() != null
                                        && (r.getDetectedDt().isEqual(s.getDetectedDt())
                                            || (r.getDetectedDt().isAfter(s.getWindowStart()) && r.getDetectedDt().isBefore(s.getDetectedDt())));
                                })
                                .filter(r -> Boolean.TRUE.equals(r.getPass()))
                                .map(com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectRuleEntity::getRuleId)
                                .collect(java.util.stream.Collectors.toSet());
                        passed = passedIds.size();
                    }
                    boolean all = total > 0 && passed == total;
                    String detectionAreaId = scnDetectionAreaMap.get(s.getScenarioId());
                    String detectionAreaName = detectionAreaId != null && !detectionAreaId.isBlank()
                            ? detectionAreaNameMap.get(detectionAreaId) : null;
                    return DetectScenarioDto.builder()
                            .scenarioId(s.getScenarioId())
                            .scenarioName(scnNameMap.get(s.getScenarioId()))
                            .groupKey(s.getGroupKey())
                            .detectedAt(s.getDetectedDt())
                            .windowStart(s.getWindowStart())
                            .windowEnd(s.getWindowEnd())
                            .aggregateCount(total)
                            .passedCount(passed)
                            .allPassed(all)
                            .detectionAreaId(detectionAreaId)
                            .detectionAreaName(detectionAreaName)
                            .transactionId(s.getTransactionId())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public ScenarioDetailDto getScenarioDetail(String groupKey, String scenarioId, LocalDateTime detectedAt) {
        ApiDetectScenarioEntity scenarioEntity = detectScenarioRepository.findOne(groupKey, scenarioId, detectedAt);
        if (scenarioEntity == null) throw new IllegalArgumentException("Scenario detection not found");

        var scenarioOpt = jpaScenarioRepository.findById(scenarioId);
        String scenarioName = scenarioOpt.map(com.itmasters.icon.api.scenario.adapter.persistence.entity.ScenarioEntity::getScenarioName).orElse(null);

        // scenario_aggregates 테이블에서 집계 목록 조회
        var scenarioAggregates = jpaScenarioAggregateRepository.findByScenarioId(scenarioId);
        var aggIds = scenarioAggregates.stream()
                .map(com.itmasters.icon.entity.ScenarioAggregateEntity::getAggregateId)
                .distinct()
                .toList();

        // 이름 매핑을 위해 집계 엔티티 조회
        var nameMap = ruleRepository.findAllById(aggIds).stream()
                .collect(Collectors.toMap(
                        com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity::getRuleId,
                        com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity::getName
                ));

        // 실행 컨텍스트 기준으로 해당 집계들의 탐지 결과 조회
        List<ApiDetectRuleEntity> detectedAggRows = detectRuleRepository.findByExecIdAndRuleIds(scenarioEntity.getMappedStorageId(), aggIds);
        Map<String, ApiDetectRuleEntity> aggRowMap = detectedAggRows.stream()
                .collect(Collectors.toMap(ApiDetectRuleEntity::getRuleId, r -> r, (a, b) -> a));

        List<ScenarioAggregateHit> aggregates = new ArrayList<>();
        // 준비: 집계 정의 맵 (predicate_rule_id 조회용) + 룰명 맵
        Map<String, com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity> aggDefMap =
                ruleRepository.findAllById(aggIds).stream().collect(Collectors.toMap(
                        com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity::getRuleId,
                        a -> a
                ));
        Map<String, String> ruleNameMap = new HashMap<>();
        for (var def : aggDefMap.values()) {
            String rid = def.getPredicateSensorId();
            if (rid != null && !rid.isBlank() && !ruleNameMap.containsKey(rid)) {
                var re = sensorRepository.findById(rid).orElse(null);
                if (re != null) ruleNameMap.put(rid, re.getSensorName());
            }
        }

        for (var sa : scenarioAggregates) {
            String aid = sa.getAggregateId();
            ApiDetectRuleEntity ar = aggRowMap.get(aid);
            var def = aggDefMap.get(aid);
            String predicateRuleId = def != null ? def.getPredicateSensorId() : null;
            String predicateRuleName = predicateRuleId != null ? ruleNameMap.get(predicateRuleId) : null;
            aggregates.add(ScenarioAggregateHit.builder()
                    .ruleId(aid)
                    .aggregateName(nameMap.get(aid))
                    .operator(ar != null ? ar.getOperator() : null)
                    .matchedCount(ar != null ? ar.getMatchedCount() : null)
                    .thresholdCount(ar != null ? ar.getThresholdCount() : null)
                    .windowMinutes(ar != null ? ar.getWindowMinutes() : null)
                    .detectedAt(ar != null ? ar.getDetectedDt() : null)
                    .predicateRuleId(predicateRuleId)
                    .predicateRuleName(predicateRuleName)
                    .pass(ar != null && Boolean.TRUE.equals(ar.getPass()))
                    .build());
        }

        return ScenarioDetailDto.builder()
                .scenarioId(scenarioId)
                .scenarioName(scenarioName)
                .groupKey(groupKey)
                .detectedAt(detectedAt)
                .windowStart(scenarioEntity.getWindowStart())
                .windowEnd(scenarioEntity.getWindowEnd())
                .aggregates(aggregates)
                .build();
    }

    // 상세: 집계 결과 → 창 내 매칭 이벤트 반환 (간이 where_json 평가)
    public AggregateDetailDto getAggregateDetail(String groupKey, String ruleId, LocalDateTime anchor) {
        var row = detectRuleRepository.findOne(groupKey, ruleId, anchor);
        if (row == null) return null;

        var aggOpt = ruleRepository.findById(ruleId);
        String predicateRuleId = aggOpt.map(a -> a.getPredicateSensorId()).orElse(null);
        LocalDateTime start = row.getStartDt();
        LocalDateTime end = row.getEndDt();

        // originalGroupKey가 있으면 그것을 사용하여 event_stream 조회
        // (group_by_fields를 사용한 경우 groupKey와 event_stream.group_key가 다름)
        String eventGroupKey = row.getOriginalGroupKey() != null ? row.getOriginalGroupKey() : groupKey;
        var events = eventStreamRepository.findByGroupKeyBetween(eventGroupKey, start, end, 1000);
        List<EventStreamDto> matched;
        String predicateRuleName = null;
        if (predicateRuleId != null) {
            SensorEntity rule = sensorRepository.findById(predicateRuleId).orElse(null);
            if (rule != null) predicateRuleName = rule.getSensorName();
            matched = events.stream()
                    .filter(e -> rule == null || matchWhere(rule.getWhereJson(), e.getEventData()))
                    .map(e -> EventStreamDto.builder()
                            .groupKey(eventGroupKey)  // eventGroupKey 사용 (event_stream_groups를 통해 필터링됨)
                            .eventDt(e.getEventDt())
                            .eventData(e.getEventData())
                            .build())
                    .collect(Collectors.toList());
        } else {
            matched = events.stream().map(e -> EventStreamDto.builder()
                    .groupKey(eventGroupKey)  // eventGroupKey 사용 (event_stream_groups를 통해 필터링됨)
                    .eventDt(e.getEventDt())
                    .eventData(e.getEventData())
                    .build()).collect(Collectors.toList());
        }

        return AggregateDetailDto.builder()
                .groupKey(groupKey)
                .ruleId(ruleId)
                .detectedAt(anchor)
                .windowStart(start)
                .windowEnd(end)
                .predicateRuleId(predicateRuleId)
                .predicateRuleName(predicateRuleName)
                .matchedEvents(matched)
                .matchedCount(matched.size())
                .build();
    }

    // 매우 간단한 where_json 평가 (EQUALS, GREATER_THAN_OR_EQUALS, LESS_THAN_OR_EQUALS)
    private boolean matchWhere(String whereJson, Map<String,Object> event) {
        if (whereJson == null || whereJson.isBlank() || event == null) return true;
        try {
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            if (whereJson.trim().startsWith("[")) {
                java.util.List<java.util.Map<String,Object>> conds = om.readValue(whereJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String,Object>>>(){});
                for (var c : conds) if (!evalCond(c, event)) return false;
                return true;
            } else {
                java.util.Map<String,Object> cond = om.readValue(whereJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>(){});
                return evalCond(cond, event);
            }
        } catch (Exception e) {
            return true; // 파싱 실패 시 필터 미적용 (보수적)
        }
    }

    private boolean evalCond(java.util.Map<String,Object> c, java.util.Map<String,Object> event) {
        Object fn = c.get("fieldName"); Object op = c.get("operator"); Object val = c.get("value");
        if (fn == null || op == null) return true;
        String field = fn.toString(); String operator = op.toString(); Object ev = findCaseInsensitive(event, field);
        if ("EQUALS".equalsIgnoreCase(operator)) {
            return ev != null && ev.toString().equals(val != null ? val.toString() : null);
        }
        if (ev == null) return false;
        try {
            java.math.BigDecimal evNum = new java.math.BigDecimal(ev.toString());
            java.math.BigDecimal vNum = new java.math.BigDecimal(val.toString());
            int cmp = evNum.compareTo(vNum);
            if ("GREATER_THAN_OR_EQUALS".equalsIgnoreCase(operator)) return cmp >= 0;
            if ("LESS_THAN_OR_EQUALS".equalsIgnoreCase(operator)) return cmp <= 0;
        } catch (Exception ignore) {}
        return true;
    }

    private Object findCaseInsensitive(java.util.Map<String,Object> m, String key) {
        if (m.containsKey(key)) return m.get(key);
        for (var e : m.entrySet()) if (e.getKey().equalsIgnoreCase(key)) return e.getValue();
        return null;
    }

    @lombok.Builder
    @lombok.Getter
    public static class AggregateDetailDto {
        private String groupKey;
        private String ruleId;
        private LocalDateTime detectedAt;
        private LocalDateTime windowStart;
        private LocalDateTime windowEnd;
        private String predicateRuleId;
        private String predicateRuleName;
        private java.util.List<EventStreamDto> matchedEvents;
        private int matchedCount;
    }

    @lombok.Builder
    @lombok.Getter
    public static class ScenarioDetailDto {
        private String scenarioId;
        private String scenarioName;
        private String groupKey;
        private LocalDateTime detectedAt;
        private LocalDateTime windowStart;
        private LocalDateTime windowEnd;
        private java.util.List<ScenarioAggregateHit> aggregates;
    }

    @lombok.Builder
    @lombok.Getter
    public static class ScenarioAggregateHit {
        private String ruleId;
        private String aggregateName;
        private String operator;
        private java.math.BigDecimal matchedCount;
        private java.math.BigDecimal thresholdCount;
        private Integer windowMinutes;
        private LocalDateTime detectedAt;
        private String predicateRuleId;
        private String predicateRuleName;
        private boolean pass;
    }

    /**
     * 엔티티 프로필 조회 (entity_attributes + 탐지 이력)
     *
     * @param groupKey 조회할 엔티티 ID (group_key)
     * @param limit    탐지 이력 조회 제한 (기본 30건)
     * @return 엔티티 프로필 정보
     */
    public EntityProfileDto getEntityProfile(String groupKey, Integer limit) {
        int detectionLimit = limit != null ? limit : 30;
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMonths(3); // 최근 3개월

        // 1. entity_attributes 조회
        ApiEntityAttributeEntity entityAttr = entityAttributeJpaRepository
                .findFirstByEntityId(groupKey)
                .orElse(null);

        // 2. 탐지 이력 조회
        // 2-1. 룰 탐지 이력
        List<ApiDetectRuleEntity> ruleRows = detectRuleReadRepository
                .findByGroupKeyBetween(groupKey, startTime, endTime);

        // 룰 이름 매핑
        var ruleIds = ruleRows.stream().map(ApiDetectRuleEntity::getRuleId).distinct().collect(Collectors.toList());
        var ruleNameMap = sensorRepository.findByIds(ruleIds).stream()
                .collect(Collectors.toMap(
                        SensorEntity::getSensorId,
                        r -> r.getSensorName() != null ? r.getSensorName() : r.getSensorId(),
                        (a, b) -> a
                ));

        List<DetectedRuleDto> detectedRules = ruleRows.stream()
                .limit(detectionLimit)
                .map(row -> DetectedRuleDto.builder()
                        .detectRuleId(row.getDetectRuleId())
                        .ruleId(row.getRuleId())
                        .ruleName(ruleNameMap.get(row.getRuleId()))
                        .groupKey(row.getGroupKey())
                        .mappedStorageId(row.getMappedStorageId())
                        .detectedAt(row.getDetectedDt())
                        .build())
                .collect(Collectors.toList());

        // 2-2. 집계 탐지 이력
        List<DetectRuleDto> detectedAggregates = getAggregates(groupKey, startTime, endTime, detectionLimit);

        // 2-3. 시나리오 탐지 이력
        List<DetectScenarioDto> detectedScenarios = getScenarios(groupKey, startTime, endTime, detectionLimit);

        // 3. 탐지 통계
        EntityProfileDto.DetectionStats stats = EntityProfileDto.DetectionStats.builder()
                .totalRules((long) detectedRules.size())
                .totalAggregates((long) detectedAggregates.size())
                .totalScenarios((long) detectedScenarios.size())
                .build();

        return EntityProfileDto.builder()
                .entityId(groupKey)
                .entityType(entityAttr != null ? entityAttr.getEntityType() : null)
                .attributes(entityAttr != null ? entityAttr.getAttributes() : null)
                .detectedRules(detectedRules)
                .detectedAggregates(detectedAggregates)
                .detectedScenarios(detectedScenarios)
                .stats(stats)
                .build();
    }

    /**
     * 엔티티 관계 그래프 조회 (React 그래프 라이브러리용)
     *
     * @param entityType 조회할 엔티티 타입 (예: "ACCOUNT", "CUSTOMER")
     * @param entityId   조회할 엔티티 ID
     * @param depth      탐색 깊이 (1: 직접 연결, 2: 2단계 연결, null: 직접 연결만)
     * @param limit      각 노드별 최대 연결 개수 (null: 제한 없음)
     * @return 노드와 엣지로 구성된 그래프 데이터
     */
    public EntityGraphDto getEntityGraph(String entityType, String entityId, Integer depth, Integer limit) {
        int searchDepth = depth != null ? Math.min(depth, 3) : 1; // 최대 3단계까지만
        int connectionLimit = limit != null ? limit : Integer.MAX_VALUE;

        java.util.Set<String> visitedNodes = new java.util.HashSet<>();
        java.util.List<EntityGraphDto.GraphNode> nodes = new ArrayList<>();
        java.util.List<EntityGraphDto.GraphEdge> edges = new ArrayList<>();
        java.util.Map<String, Integer> totalConnectionsMap = new java.util.HashMap<>();

        // 시작 노드 추가
        String startNodeId = entityType + ":" + entityId;
        visitedNodes.add(startNodeId);

        // 시작 노드의 attributes 조회
        ApiEntityAttributeEntity startEntity = entityAttributeJpaRepository
                .findByEntityTypeAndEntityId(entityType, entityId)
                .orElse(null);

        // 시작 노드의 전체 연결 개수 계산
        int totalConnections = entityRelationRepository.findByFromEntityTypeAndFromEntityId(entityType, entityId).size()
                + entityRelationRepository.findByToEntityTypeAndToEntityId(entityType, entityId).size();

        totalConnectionsMap.put(startNodeId, totalConnections);

        nodes.add(new EntityGraphDto.GraphNode(
                startNodeId,
                entityId,
                entityType,
                startEntity != null ? startEntity.getAttributes() : null,
                null, // totalConnections는 나중에 설정
                null  // displayedConnections는 나중에 설정
        ));

        // 깊이 우선 탐색
        exploreRelations(entityType, entityId, searchDepth, 1, connectionLimit, visitedNodes, nodes, edges, totalConnectionsMap);

        // 각 노드의 totalConnections와 displayedConnections 계산
        for (EntityGraphDto.GraphNode node : nodes) {
            int total = totalConnectionsMap.getOrDefault(node.getId(), 0);
            int displayed = (int) edges.stream()
                    .filter(e -> e.getSource().equals(node.getId()) || e.getTarget().equals(node.getId()))
                    .count();

            // 노드 재생성 (totalConnections, displayedConnections 포함)
            int index = nodes.indexOf(node);
            nodes.set(index, new EntityGraphDto.GraphNode(
                    node.getId(),
                    node.getLabel(),
                    node.getType(),
                    node.getData(),
                    total,
                    displayed
            ));
        }

        return new EntityGraphDto(nodes, edges);
    }

    /**
     * 관계 탐색 (재귀적)
     */
    private void exploreRelations(
            String entityType,
            String entityId,
            int maxDepth,
            int currentDepth,
            int connectionLimit,
            java.util.Set<String> visitedNodes,
            java.util.List<EntityGraphDto.GraphNode> nodes,
            java.util.List<EntityGraphDto.GraphEdge> edges,
            java.util.Map<String, Integer> totalConnectionsMap
    ) {
        if (currentDepth > maxDepth) {
            return;
        }

        // 1. 이 엔티티에서 나가는 관계 조회
        List<EntityRelationEntity> outgoingRelations =
                entityRelationRepository.findByFromEntityTypeAndFromEntityId(entityType, entityId);

        // limit 적용 (depth 1일 때만 제한)
        int processedCount = 0;
        for (EntityRelationEntity relation : outgoingRelations) {
            if (currentDepth == 1 && processedCount >= connectionLimit) {
                break; // limit 도달 시 중단
            }
            String toNodeId = relation.getToEntityType() + ":" + relation.getToEntityId();

            // 엣지 추가
            String edgeId = String.format("%s:%s->%s->%s:%s",
                    relation.getFromEntityType(), relation.getFromEntityId(),
                    relation.getRelationType(),
                    relation.getToEntityType(), relation.getToEntityId());

            edges.add(new EntityGraphDto.GraphEdge(
                    edgeId,
                    entityType + ":" + entityId,
                    toNodeId,
                    relation.getRelationType(),
                    relation.getRelationType(),
                    relation.getProperties()
            ));

            // 새 노드면 추가하고 재귀 탐색
            if (!visitedNodes.contains(toNodeId)) {
                visitedNodes.add(toNodeId);

                // 노드 attributes 조회
                ApiEntityAttributeEntity targetEntity = entityAttributeJpaRepository
                        .findByEntityTypeAndEntityId(relation.getToEntityType(), relation.getToEntityId())
                        .orElse(null);

                // 대상 노드의 전체 연결 개수 계산
                int targetTotalConnections = entityRelationRepository
                        .findByFromEntityTypeAndFromEntityId(relation.getToEntityType(), relation.getToEntityId()).size()
                        + entityRelationRepository
                        .findByToEntityTypeAndToEntityId(relation.getToEntityType(), relation.getToEntityId()).size();
                totalConnectionsMap.put(toNodeId, targetTotalConnections);

                nodes.add(new EntityGraphDto.GraphNode(
                        toNodeId,
                        relation.getToEntityId(),
                        relation.getToEntityType(),
                        targetEntity != null ? targetEntity.getAttributes() : null,
                        null, // totalConnections는 나중에 설정
                        null  // displayedConnections는 나중에 설정
                ));

                // 재귀 탐색
                exploreRelations(
                        relation.getToEntityType(),
                        relation.getToEntityId(),
                        maxDepth,
                        currentDepth + 1,
                        connectionLimit,
                        visitedNodes,
                        nodes,
                        edges,
                        totalConnectionsMap
                );
            }

            processedCount++; // limit 카운트 증가
        }

        // 2. 이 엔티티로 들어오는 관계 조회 (역방향)
        List<EntityRelationEntity> incomingRelations =
                entityRelationRepository.findByToEntityTypeAndToEntityId(entityType, entityId);

        // limit 적용 (depth 1일 때만 제한, 이미 처리한 개수를 고려)
        for (EntityRelationEntity relation : incomingRelations) {
            if (currentDepth == 1 && processedCount >= connectionLimit) {
                break; // limit 도달 시 중단
            }

            String fromNodeId = relation.getFromEntityType() + ":" + relation.getFromEntityId();

            // 엣지 추가
            String edgeId = String.format("%s:%s->%s->%s:%s",
                    relation.getFromEntityType(), relation.getFromEntityId(),
                    relation.getRelationType(),
                    relation.getToEntityType(), relation.getToEntityId());

            // 중복 방지 (이미 추가된 엣지는 스킵)
            boolean alreadyAdded = edges.stream()
                    .anyMatch(e -> e.getId().equals(edgeId));

            if (!alreadyAdded) {
                edges.add(new EntityGraphDto.GraphEdge(
                        edgeId,
                        fromNodeId,
                        entityType + ":" + entityId,
                        relation.getRelationType(),
                        relation.getRelationType(),
                        relation.getProperties()
                ));
            }

            // 새 노드면 추가하고 재귀 탐색
            if (!visitedNodes.contains(fromNodeId)) {
                visitedNodes.add(fromNodeId);

                // 노드 attributes 조회
                ApiEntityAttributeEntity sourceEntity = entityAttributeJpaRepository
                        .findByEntityTypeAndEntityId(relation.getFromEntityType(), relation.getFromEntityId())
                        .orElse(null);

                // 소스 노드의 전체 연결 개수 계산
                int sourceTotalConnections = entityRelationRepository
                        .findByFromEntityTypeAndFromEntityId(relation.getFromEntityType(), relation.getFromEntityId()).size()
                        + entityRelationRepository
                        .findByToEntityTypeAndToEntityId(relation.getFromEntityType(), relation.getFromEntityId()).size();
                totalConnectionsMap.put(fromNodeId, sourceTotalConnections);

                nodes.add(new EntityGraphDto.GraphNode(
                        fromNodeId,
                        relation.getFromEntityId(),
                        relation.getFromEntityType(),
                        sourceEntity != null ? sourceEntity.getAttributes() : null,
                        null, // totalConnections는 나중에 설정
                        null  // displayedConnections는 나중에 설정
                ));

                // 재귀 탐색
                exploreRelations(
                        relation.getFromEntityType(),
                        relation.getFromEntityId(),
                        maxDepth,
                        currentDepth + 1,
                        connectionLimit,
                        visitedNodes,
                        nodes,
                        edges,
                        totalConnectionsMap
                );
            }

            processedCount++; // limit 카운트 증가
        }
    }

    /**
     * 엔티티 행적 조회 (entity_id가 group_key에 포함된 모든 탐지 이력 및 활동 로그)
     *
     * @param entityId 조회할 엔티티 ID (예: EMP004)
     * @param detectionLimit 탐지 이력 조회 제한 (기본 50건)
     * @param activityLimit 활동 로그 조회 제한 (기본 100건)
     * @return 엔티티 행적 정보
     */
    public EntityHistoryDto getEntityHistory(String entityId, Integer detectionLimit, Integer activityLimit) {
        int detLimit = detectionLimit != null ? detectionLimit : 50;
        int actLimit = activityLimit != null ? activityLimit : 100;

        // 1. 통계 정보 조회 (count)
        long scenarioCount = entityHistoryRepository.countScenariosByEntityId(entityId);
        long ruleCount = entityHistoryRepository.countRulesByEntityId(entityId);
        long activityCount = entityHistoryRepository.countEventsByEntityId(entityId);

        // 2. 탐지 이력 조회 (시나리오 + 룰)
        List<EntityHistoryDto.DetectionRecord> detections = new ArrayList<>();

        // 2-1. 시나리오 탐지 조회
        var scenarios = entityHistoryRepository.findScenariosByEntityId(entityId, detLimit);
        var scenarioIds = scenarios.stream().map(ApiDetectScenarioEntity::getScenarioId).distinct().toList();
        var scenarioNameMap = jpaScenarioRepository.findAllById(scenarioIds).stream()
                .collect(Collectors.toMap(
                        s -> s.getScenarioId(),
                        s -> s.getScenarioName(),
                        (a, b) -> a
                ));

        for (var s : scenarios) {
            detections.add(EntityHistoryDto.DetectionRecord.builder()
                    .type("SCENARIO")
                    .detectionId(s.getScenarioId())
                    .detectionName(scenarioNameMap.get(s.getScenarioId()))
                    .groupKey(s.getGroupKey())
                    .riskLevel(null) // 시나리오는 별도 위험도 없음
                    .detectedAt(s.getDetectedDt())
                    .details(Map.of(
                            "windowStart", s.getWindowStart() != null ? s.getWindowStart().toString() : "",
                            "windowEnd", s.getWindowEnd() != null ? s.getWindowEnd().toString() : "",
                            "transactionId", s.getTransactionId() != null ? s.getTransactionId() : ""
                    ))
                    .build());
        }

        // 2-2. 룰 탐지 조회
        var rules = entityHistoryRepository.findRulesByEntityId(entityId, detLimit);
        var ruleIds = rules.stream().map(ApiDetectRuleEntity::getRuleId).distinct().toList();
        var ruleEntities = ruleRepository.findAllById(ruleIds);
        var ruleNameMap = ruleEntities.stream()
                .collect(Collectors.toMap(
                        com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity::getRuleId,
                        com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity::getName,
                        (a, b) -> a
                ));
        var ruleEvaluationModeMap = ruleEntities.stream()
                .collect(Collectors.toMap(
                        com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity::getRuleId,
                        rule -> rule.getEvaluationMode() != null ? rule.getEvaluationMode() : "",
                        (a, b) -> a
                ));

        for (var r : rules) {
            detections.add(EntityHistoryDto.DetectionRecord.builder()
                    .type("RULE")
                    .detectionId(r.getRuleId())
                    .detectionName(ruleNameMap.get(r.getRuleId()))
                    .groupKey(r.getGroupKey())
                    .riskLevel(null)
                    .detectedAt(r.getDetectedDt())
                    .details(Map.of(
                            "evaluationMode", ruleEvaluationModeMap.get(r.getRuleId()),
                            "eventDt", r.getEventDt() != null ? r.getEventDt().toString() : "",
                            "matchedCount", r.getMatchedCount() != null ? r.getMatchedCount().toString() : "",
                            "thresholdCount", r.getThresholdCount() != null ? r.getThresholdCount().toString() : "",
                            "transactionId", r.getTransactionId() != null ? r.getTransactionId() : ""
                    ))
                    .build());
        }

        // 시간순 정렬 (최신순)
        detections.sort((a, b) -> b.getDetectedAt().compareTo(a.getDetectedAt()));

        // 3. 활동 로그 조회 (이벤트 스트림)
        var events = entityHistoryRepository.findEventsByEntityId(entityId, actLimit);
        List<EntityHistoryDto.ActivityLog> activities = new ArrayList<>();
        for (var e : events) {
            activities.add(EntityHistoryDto.ActivityLog.builder()
                    .eventStreamId(e.getEventStreamId())
                    .groupKey(null) // event_streams에는 group_key가 없음
                    .dataSourceId(null) // event_streams에는 data_source_id가 없음
                    .transactionId(e.getTransactionId())
                    .eventDt(e.getEventDt())
                    .eventData(e.getEventData())
                    .build());
        }

        // 4. 첫/마지막 활동 시간 계산
        LocalDateTime firstActivity = null;
        LocalDateTime lastActivity = null;
        if (!activities.isEmpty()) {
            firstActivity = activities.stream()
                    .map(EntityHistoryDto.ActivityLog::getEventDt)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
            lastActivity = activities.stream()
                    .map(EntityHistoryDto.ActivityLog::getEventDt)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
        }

        // 5. entityType 조회 (entity_attributes 테이블에서)
        String entityType = entityAttributeJpaRepository.findFirstByEntityId(entityId)
                .map(ApiEntityAttributeEntity::getEntityType)
                .orElse(null);

        // 6. 결과 조합
        EntityHistoryDto.EntityHistoryStats stats = EntityHistoryDto.EntityHistoryStats.builder()
                .totalDetections((int) (scenarioCount + ruleCount))
                .scenarioCount((int) scenarioCount)
                .aggregateCount(0)
                .ruleCount((int) ruleCount)
                .totalActivities((int) activityCount)
                .firstActivityAt(firstActivity)
                .lastActivityAt(lastActivity)
                .build();

        return EntityHistoryDto.builder()
                .entityId(entityId)
                .entityType(entityType)
                .stats(stats)
                .detections(detections)
                .activities(activities)
                .build();
    }
}
