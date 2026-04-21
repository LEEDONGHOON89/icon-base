package com.itmasters.icon.engine.mapping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.common.constants.EngineConstants;
import com.itmasters.icon.common.domain.type.GroupKeyType;
import com.itmasters.icon.common.domain.type.ParserType;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceParserEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceSchemaEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineParserEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineParserRuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineDataSourceParserJpaRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineDataSourceSchemaRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * DataSourceSchema 기반 필드 매핑 엔진
 * [2026-04-20] 재설계: 파서를 데이터소스 레벨에서 적용
 *   1) 데이터소스에 연결된 활성 파서 목록 조회
 *   2) 각 파서의 source_field 값을 파싱하여 COLUMN1~N 추출
 *   3) 원본 source_field 제거
 *   4) 표준 필드 매핑 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileSchemaMappingEngine implements FieldMappingEngine {

    private final EngineDataSourceSchemaRepository dataSourceSchemaRepository;
    private final EngineProfileRepository profileRepository;
    // [2026-04-20] 데이터소스-파서 연결 레포지토리
    private final EngineDataSourceParserJpaRepository dataSourceParserRepository;

    private final Map<String, GroupKeyInfo> groupKeyCache = new ConcurrentHashMap<>();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public List<Map<String, Object>> mapByDataSource(List<Map<String, Object>> sourceRows, String dataSourceId) {
        log.info("DataSourceSchema 기반 필드 매핑 시작 - dataSourceId: {}, rows: {}",
                dataSourceId, sourceRows.size());

        // ── 1. 데이터소스에 연결된 활성 파서 목록 조회 ────────────────────────
        List<EngineDataSourceParserEntity> linkedParsers =
                dataSourceParserRepository.findByDataSourceIdAndIsActiveTrueOrderByParserOrderAsc(dataSourceId);

        // ── 2. DataSourceSchema 조회 및 매핑 맵 구성 ─────────────────────────
        List<EngineDataSourceSchemaEntity> schemas =
                dataSourceSchemaRepository.findByDataSourceId(dataSourceId);

        if (schemas.isEmpty() && linkedParsers.isEmpty()) {
            log.warn("DataSourceSchema와 파서가 없어 원본 데이터를 그대로 반환합니다 - dataSourceId: {}", dataSourceId);
            return sourceRows;
        }

        Map<String, List<EngineDataSourceSchemaEntity>> schemaMap = new HashMap<>();
        for (EngineDataSourceSchemaEntity schema : schemas) {
            if (Boolean.TRUE.equals(schema.getIsActive())) {
                schemaMap.computeIfAbsent(schema.getFieldName(), k -> new ArrayList<>()).add(schema);
            }
        }

        Set<String> inactiveFieldNames = schemas.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsActive()))
                .map(EngineDataSourceSchemaEntity::getFieldName)
                .collect(Collectors.toSet());

        // ── 3. 각 row 처리 ───────────────────────────────────────────────────
        List<Map<String, Object>> mappedRows = new ArrayList<>();
        for (Map<String, Object> sourceRow : sourceRows) {

            // 3-1. 파서 적용: source_field 파싱 → COLUMN1~N 추출, 원본 제거
            Map<String, Object> parsedRow = applyParsers(sourceRow, linkedParsers);

            // 3-2. 표준 필드 매핑
            Map<String, Object> mappedRow = new HashMap<>();
            for (Map.Entry<String, Object> entry : parsedRow.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();

                if (EngineConstants.Field.isMetadataField(fieldName)) {
                    mappedRow.put(fieldName, value);
                    continue;
                }
                if (inactiveFieldNames.contains(fieldName)) {
                    log.debug("비활성 필드 제외: {}", fieldName);
                    continue;
                }

                List<EngineDataSourceSchemaEntity> mappingList = schemaMap.get(fieldName);
                if (mappingList != null && !mappingList.isEmpty()) {
                    for (EngineDataSourceSchemaEntity mapping : mappingList) {
                        if (!mapping.isValidType(value)) {
                            log.warn("필드 타입 불일치 - field: {}, expected: {}, actual: {}",
                                    fieldName, mapping.getDataType(),
                                    value != null ? value.getClass().getSimpleName() : "null");
                        }
                        mappedRow.put(mapping.getTargetFieldName(), value);
                        log.debug("필드 매핑: {} -> {} = {}", fieldName, mapping.getTargetFieldName(), value);
                    }
                } else {
                    mappedRow.put(fieldName, value);
                    log.trace("스키마 정보 없음, 원본 유지: {}", fieldName);
                }
            }
            mappedRows.add(mappedRow);
        }

        log.info("DataSourceSchema 기반 필드 매핑 완료 - {} rows", mappedRows.size());
        return mappedRows;
    }

    // ─── 파서 적용: 데이터소스에 연결된 파서를 순서대로 실행 ──────────────────────

    /**
     * [2026-04-20] 데이터소스에 연결된 파서를 순서대로 적용
     * - source_field 값을 파싱하여 추출 필드들을 row에 추가
     * - 파싱 완료 후 원본 source_field 제거
     */
    private Map<String, Object> applyParsers(Map<String, Object> row,
                                              List<EngineDataSourceParserEntity> linkedParsers) {
        if (linkedParsers.isEmpty()) return row;

        Map<String, Object> result = new HashMap<>(row);
        for (EngineDataSourceParserEntity link : linkedParsers) {
            EngineParserEntity parser = link.getParser();
            if (parser == null || !parser.isActive()) continue;

            String sourceField = parser.getSourceField();
            if (sourceField == null || sourceField.isBlank()) continue;

            Object rawValue = result.get(sourceField);
            if (rawValue == null) {
                log.debug("파서 대상 필드 없음 - parserId: {}, sourceField: {}", parser.getParserId(), sourceField);
                continue;
            }

            String text = String.valueOf(rawValue);
            try {
                extractFields(parser, text, result);
                // 원본 source_field 제거
                result.remove(sourceField);
                log.debug("파서 적용 완료 - parserId: {}, sourceField: {} 제거", parser.getParserId(), sourceField);
            } catch (Exception e) {
                log.warn("파서 적용 실패 - parserId: {}, sourceField: {}: {}",
                        parser.getParserId(), sourceField, e.getMessage());
            }
        }
        return result;
    }

    /**
     * [2026-04-20] 파서 타입별 추출 실행
     */
    private void extractFields(EngineParserEntity parser, String text, Map<String, Object> row) throws Exception {
        List<EngineParserRuleEntity> rules = parser.getRules();
        if (rules == null || rules.isEmpty()) return;

        switch (parser.getParserType()) {
            case DELIMITER -> extractDelimiter(parser, text, rules, row);
            case FIXED_WIDTH -> extractFixedWidth(text, rules, row);
            case REGEX -> extractRegex(text, rules, row);
        }
    }

    // ─── DELIMITER 파싱 ───────────────────────────────────────────────────────

    /**
     * 구분자 파싱: 파서레벨 config_json에서 delimiter 추출
     * split 후 rule_order 순서대로 출력 필드에 저장
     */
    private void extractDelimiter(EngineParserEntity parser, String text,
                                   List<EngineParserRuleEntity> rules,
                                   Map<String, Object> row) throws Exception {
        String delimiter = "|";  // 기본값
        if (parser.getConfigJson() != null) {
            Map<String, Object> cfg = OBJECT_MAPPER.readValue(
                    parser.getConfigJson(), new TypeReference<>() {});
            if (cfg.containsKey("delimiter")) {
                delimiter = String.valueOf(cfg.get("delimiter"));
            }
        }

        String[] parts = text.split(Pattern.quote(delimiter), -1);
        for (EngineParserRuleEntity rule : rules) {
            int idx = rule.getRuleOrder();
            String value = (idx >= 0 && idx < parts.length) ? parts[idx] : "";
            putExtracted(row, rule, value);
        }
    }

    // ─── FIXED_WIDTH 파싱 ─────────────────────────────────────────────────────

    /**
     * 고정폭 파싱: 규칙 순서대로 byteLength 누적하여 startByte 자동 계산
     * rule.config_json = {"byteLength":4}
     */
    private void extractFixedWidth(String text, List<EngineParserRuleEntity> rules,
                                    Map<String, Object> row) throws Exception {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        int offset = 0;
        for (EngineParserRuleEntity rule : rules) {
            int byteLength = 0;
            if (rule.getConfigJson() != null) {
                Map<String, Object> cfg = OBJECT_MAPPER.readValue(
                        rule.getConfigJson(), new TypeReference<>() {});
                byteLength = cfg.containsKey("byteLength")
                        ? ((Number) cfg.get("byteLength")).intValue() : 0;
            }
            if (byteLength <= 0) {
                offset = bytes.length; // 나머지 전체
                continue;
            }
            int end = Math.min(offset + byteLength, bytes.length);
            String value = offset < bytes.length
                    ? new String(bytes, offset, end - offset, StandardCharsets.UTF_8)
                    : "";
            putExtracted(row, rule, value);
            offset = end;
            if (offset >= bytes.length) break;
        }
    }

    // ─── REGEX 파싱 ───────────────────────────────────────────────────────────

    /**
     * 정규식 파싱: 규칙별 pattern + group
     * rule.config_json = {"pattern":"^(\\w+)","group":1}
     */
    private void extractRegex(String text, List<EngineParserRuleEntity> rules,
                               Map<String, Object> row) throws Exception {
        for (EngineParserRuleEntity rule : rules) {
            if (rule.getConfigJson() == null) continue;
            Map<String, Object> cfg = OBJECT_MAPPER.readValue(
                    rule.getConfigJson(), new TypeReference<>() {});
            String pattern = String.valueOf(cfg.getOrDefault("pattern", ""));
            int group = cfg.containsKey("group") ? ((Number) cfg.get("group")).intValue() : 0;
            Matcher m = Pattern.compile(pattern).matcher(text);
            String value = m.find() && group <= m.groupCount() ? m.group(group) : "";
            putExtracted(row, rule, value);
        }
    }

    // ─── 공통 헬퍼 ───────────────────────────────────────────────────────────

    /** 추출된 값을 타겟 필드명으로 row에 저장 */
    private void putExtracted(Map<String, Object> row, EngineParserRuleEntity rule, String value) {
        String key = resolveTargetKey(rule);
        if (key != null) {
            row.put(key, value);
            log.debug("파서 추출: {} = {}", key, value);
        }
    }

    // [2026-04-21] targetStandardFieldId 제거에 따라 targetFieldName만 사용
    private String resolveTargetKey(EngineParserRuleEntity rule) {
        if (rule.getTargetFieldName() != null && !rule.getTargetFieldName().isBlank())
            return rule.getTargetFieldName();
        return null;
    }

    // ─── transform_rule (기존 유지) ───────────────────────────────────────────

    private Object applyTransformRule(Object value, String transformRule,
                                       Map<String, Object> mappedRow,
                                       Map<String, Object> sourceRow) {
        if (transformRule == null || transformRule.isEmpty()) return value;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = OBJECT_MAPPER.readValue(transformRule, Map.class);
            String op = String.valueOf(rule.getOrDefault("op", "")).toUpperCase();
            boolean ignoreCase = Boolean.parseBoolean(String.valueOf(rule.getOrDefault("ignoreCase", false)));

            Object whenObj = rule.get("when");
            if (whenObj instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked") Map<String, Object> when = (Map<String, Object>) whenObj;
                String condOp = String.valueOf(when.getOrDefault("op", "EQ")).toUpperCase();
                boolean condIgnoreCase = Boolean.parseBoolean(String.valueOf(when.getOrDefault("ignoreCase", ignoreCase)));
                String wf  = asText(when.get("field"));
                String wsf = asText(when.get("sourceField"));
                Object left = null;
                if (wf  != null && mappedRow  != null) left = mappedRow.get(wf);
                if (left == null && wsf != null && sourceRow != null) left = sourceRow.get(wsf);
                Object expected = when.get("value");
                if (expected == null) return null;
                String a = left == null ? null : String.valueOf(left);
                String b = String.valueOf(expected);
                if (a == null) return null;
                if (condIgnoreCase) { a = a.toLowerCase(); b = b.toLowerCase(); }
                boolean ceq  = a.equals(b);
                boolean pass = "EQ".equals(condOp) ? ceq : !ceq;
                if (!pass) return null;
            }

            if ("EQ".equals(op) || "NEQ".equals(op)) {
                Object expected = rule.get("value");
                if (expected == null) return value;
                if (value == null) return null;
                String a = String.valueOf(value);
                String b = String.valueOf(expected);
                if (ignoreCase) { a = a.toLowerCase(); b = b.toLowerCase(); }
                boolean eq = a.equals(b);
                return "EQ".equals(op) ? eq : !eq;
            }
            return value;
        } catch (Exception e) {
            log.warn("transform_rule 적용 실패: {} => {}", transformRule, e.getMessage());
            return value;
        }
    }

    private String asText(Object o) { return o == null ? null : String.valueOf(o); }

    // ─── group_key (기존 유지) ────────────────────────────────────────────────

    public Map<String, List<Map<String, Object>>> groupByGroupKey(
            List<Map<String, Object>> sourceRows, String profileId) {
        GroupKeyInfo groupKeyInfo = loadGroupKeyInfo(profileId);
        if (groupKeyInfo == null || groupKeyInfo.getGroupKey() == null) {
            return Map.of(EngineConstants.GroupKey.DEFAULT_GROUP_NAME, sourceRows);
        }
        Map<String, List<Map<String, Object>>> groupedData = new HashMap<>();
        for (Map<String, Object> row : sourceRows) {
            String keyValue = generateGroupKeyValue(row, groupKeyInfo.getKeyFields(),
                    groupKeyInfo.getGroupKeyType());
            groupedData.computeIfAbsent(keyValue, k -> new ArrayList<>()).add(row);
        }
        return groupedData;
    }

    private String generateGroupKeyValue(Map<String, Object> row,
                                          List<String> keyFields, GroupKeyType keyType) {
        if (keyFields.isEmpty()) return EngineConstants.GroupKey.DEFAULT_GROUP_NAME;
        if (keyType == GroupKeyType.SINGLE) {
            Object value = row.get(keyFields.get(0));
            return value != null ? value.toString() : EngineConstants.GroupKey.NULL_VALUE_PLACEHOLDER;
        } else if (keyType == GroupKeyType.COMPOSITE) {
            return keyFields.stream()
                    .map(f -> { Object v = row.get(f); return v != null ? v.toString()
                            : EngineConstants.GroupKey.NULL_VALUE_PLACEHOLDER; })
                    .collect(Collectors.joining(EngineConstants.GroupKey.COMPOSITE_SEPARATOR));
        } else {
            Object value = row.get(keyFields.get(0));
            return value != null ? value.toString() : EngineConstants.GroupKey.CUSTOM_DEFAULT_VALUE;
        }
    }

    private GroupKeyInfo loadGroupKeyInfo(String profileId) { return null; }

    public void clearCache() { groupKeyCache.clear(); }
    public void clearCache(String profileId) { groupKeyCache.remove(profileId); }

    private static class GroupKeyInfo {
        private final String groupKey;
        private final GroupKeyType groupKeyType;
        private final List<String> keyFields;
        GroupKeyInfo(String groupKey, GroupKeyType groupKeyType, List<String> keyFields) {
            this.groupKey = groupKey; this.groupKeyType = groupKeyType;
            this.keyFields = keyFields != null ? keyFields : Collections.emptyList();
        }
        String getGroupKey() { return groupKey; }
        GroupKeyType getGroupKeyType() { return groupKeyType; }
        List<String> getKeyFields() { return keyFields; }
    }
}
