package com.itmasters.icon.engine.mapping;

import com.itmasters.icon.common.constants.EngineConstants;
import com.itmasters.icon.common.domain.type.GroupKeyType;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineDataSourceSchemaRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineProfileRepository;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceSchemaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * DataSourceSchema 기반 필드 매핑 엔진 구현체
 * 원본 필드명을 표준 필드명으로 변환
 *
 * Note: ProfileSchema는 사용되지 않음 (data_source_schemas만 사용)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileSchemaMappingEngine implements FieldMappingEngine {

    private final EngineDataSourceSchemaRepository dataSourceSchemaRepository;
    private final EngineProfileRepository profileRepository;

    // group_key 정보 캐시
    private final Map<String, GroupKeyInfo> groupKeyCache = new ConcurrentHashMap<>();

    @Override
    public List<Map<String, Object>> mapByDataSource(List<Map<String, Object>> sourceRows, String dataSourceId) {
        log.info("DataSourceSchema 기반 필드 매핑 시작 - dataSourceId: {}, rows: {}",
                dataSourceId, sourceRows.size());

        // DataSourceSchema 조회
        List<EngineDataSourceSchemaEntity> schemas =
            dataSourceSchemaRepository.findByDataSourceId(dataSourceId);

        if (schemas.isEmpty()) {
            log.warn("DataSourceSchema가 없어 원본 데이터를 그대로 반환합니다 - dataSourceId: {}", dataSourceId);
            return sourceRows;
        }

        // 매핑 정보 구성 (동일 원본 필드에 다중 타깃 매핑 허용)
        Map<String, List<EngineDataSourceSchemaEntity>> schemaMap = new HashMap<>();
        for (EngineDataSourceSchemaEntity schema : schemas) {
            if (Boolean.TRUE.equals(schema.getIsActive())) {
                schemaMap.computeIfAbsent(schema.getFieldName(), k -> new ArrayList<>()).add(schema);
            }
        }

        // 각 row에 대해 매핑 수행
        List<Map<String, Object>> mappedRows = new ArrayList<>();
        for (Map<String, Object> sourceRow : sourceRows) {
            Map<String, Object> mappedRow = new HashMap<>();

            for (Map.Entry<String, Object> entry : sourceRow.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();

                // 메타데이터 필드는 그대로 유지
                if (EngineConstants.Field.isMetadataField(fieldName)) {
                    mappedRow.put(fieldName, value);
                    continue;
                }

                // DataSourceSchema 확인 (다중 매핑)
                List<EngineDataSourceSchemaEntity> mappingList = schemaMap.get(fieldName);
                if (mappingList != null && !mappingList.isEmpty()) {
                    for (EngineDataSourceSchemaEntity mapping : mappingList) {
                        // 표준 필드 매핑이 있으면 표준 필드명으로, 없으면 원본 필드명 사용
                        String targetFieldName = mapping.getTargetFieldName();

                        // 타입 검증 (원본 값 기준 간단 검증)
                        if (!mapping.isValidType(value)) {
                            log.warn("필드 타입 불일치 - field: {}, expected: {}, actual: {}",
                                    fieldName, mapping.getDataType(), value != null ? value.getClass().getSimpleName() : "null");
                        }

                        // 변환 규칙 사용 중단: 원값 그대로 사용
                        mappedRow.put(targetFieldName, value);

                        log.debug("필드 매핑 적용: {} -> {} (값: {})",
                                fieldName, targetFieldName, value);
                    }
                } else {
                    // 스키마에 없는 필드는 그대로 유지
                    mappedRow.put(fieldName, value);
                    log.trace("스키마 정보 없음, 원본 유지: {}", fieldName);
                }
            }

            mappedRows.add(mappedRow);
        }

        log.info("DataSourceSchema 기반 필드 매핑 완료 - {} rows", mappedRows.size());
        return mappedRows;
    }

    // Note: any derived value handling should be expressed via data_source_schemas.transform_rule
    // and applied in applyTransformRule(), rather than hardcoding here.

    /**
     * 변환 규칙 적용
     */
    private Object applyTransformRule(Object value, String transformRule, Map<String,Object> mappedRow, Map<String,Object> sourceRow) {
        if (transformRule == null || transformRule.isEmpty()) return value;
        try {
            // 간단한 JSON 규칙 파서 (op 기반)
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = om.readValue(transformRule, Map.class);
            String op = String.valueOf(rule.getOrDefault("op", "")).toUpperCase();
            boolean ignoreCase = Boolean.parseBoolean(String.valueOf(rule.getOrDefault("ignoreCase", false)));

            // Optional conditional guard: when { field | sourceField, op(EQ|NEQ), value, ignoreCase }
            Object whenObj = rule.get("when");
            if (whenObj instanceof Map<?,?>) {
                @SuppressWarnings("unchecked") Map<String,Object> when = (Map<String, Object>) whenObj;
                String condOp = String.valueOf(when.getOrDefault("op", "EQ")).toUpperCase();
                boolean condIgnoreCase = Boolean.parseBoolean(String.valueOf(when.getOrDefault("ignoreCase", ignoreCase)));
                String wf = asText(when.get("field"));
                String wsf = asText(when.get("sourceField"));
                Object left = null;
                if (wf != null && mappedRow != null) left = mappedRow.get(wf);
                if (left == null && wsf != null && sourceRow != null) left = sourceRow.get(wsf);
                Object expected = when.get("value");
                if (expected == null) return null; // invalid condition
                String a = left == null ? null : String.valueOf(left);
                String b = String.valueOf(expected);
                if (a == null) return null; // condition not met (missing field)
                if (condIgnoreCase) { a = a.toLowerCase(); b = b.toLowerCase(); }
                boolean ceq = a.equals(b);
                boolean pass = "EQ".equals(condOp) ? ceq : !ceq;
                if (!pass) return null; // condition failed -> skip transform
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

            if ("NEQ_FIELDS".equals(op) || "EQ_FIELDS".equals(op)) {
                // 양쪽 필드 값을 가져와 비교 (우선 mappedRow의 표준 필드, 없으면 sourceRow의 원본 필드)
                String leftField = asText(rule.get("leftField"));
                String rightField = asText(rule.get("rightField"));
                String leftSourceField = asText(rule.get("leftSourceField"));
                String rightSourceField = asText(rule.get("rightSourceField"));

                Object lv = null;
                if (leftField != null && mappedRow != null) lv = mappedRow.get(leftField);
                if (lv == null && leftSourceField != null && sourceRow != null) lv = sourceRow.get(leftSourceField);
                if (lv == null && leftField != null && sourceRow != null) lv = sourceRow.get(leftField);

                Object rv = null;
                if (rightField != null && mappedRow != null) rv = mappedRow.get(rightField);
                if (rv == null && rightSourceField != null && sourceRow != null) rv = sourceRow.get(rightSourceField);
                if (rv == null && rightField != null && sourceRow != null) rv = sourceRow.get(rightField);

                if (lv == null || rv == null) return null;
                String a = String.valueOf(lv);
                String b = String.valueOf(rv);
                if (ignoreCase) { a = a.toLowerCase(); b = b.toLowerCase(); }
                boolean eq = a.equals(b);
                return "EQ_FIELDS".equals(op) ? eq : !eq;
            }

            // 기본: 변환 불가 → 원값 유지
            return value;
        } catch (Exception e) {
            log.warn("transform_rule 적용 실패: {} => {}", transformRule, e.getMessage());
            return value;
        }
    }

    private String asText(Object o) { return o == null ? null : String.valueOf(o); }

    /**
     * group_key 기반 데이터 그룹화
     * @param sourceRows 원본 데이터
     * @param profileId 프로파일 ID
     * @return group_key 값별로 그룹화된 데이터
     */
    public Map<String, List<Map<String, Object>>> groupByGroupKey(
            List<Map<String, Object>> sourceRows, String profileId) {

        GroupKeyInfo groupKeyInfo = loadGroupKeyInfo(profileId);
        if (groupKeyInfo == null || groupKeyInfo.getGroupKey() == null) {
            log.debug("프로파일 {}에 group_key가 설정되지 않음", profileId);
            return Map.of(EngineConstants.GroupKey.DEFAULT_GROUP_NAME, sourceRows);
        }

        Map<String, List<Map<String, Object>>> groupedData = new HashMap<>();
        List<String> keyFields = groupKeyInfo.getKeyFields();

        for (Map<String, Object> row : sourceRows) {
            // group_key 값 생성
            String keyValue = generateGroupKeyValue(row, keyFields, groupKeyInfo.getGroupKeyType());

            // 그룹에 추가
            groupedData.computeIfAbsent(keyValue, k -> new ArrayList<>()).add(row);
        }

        log.info("프로파일 {} - group_key로 {}개 그룹 생성", profileId, groupedData.size());
        return groupedData;
    }

    /**
     * group_key 값 생성
     */
    private String generateGroupKeyValue(Map<String, Object> row, List<String> keyFields,
                                         GroupKeyType keyType) {
        if (keyFields.isEmpty()) {
            return EngineConstants.GroupKey.DEFAULT_GROUP_NAME;
        }

        if (keyType == GroupKeyType.SINGLE) {
            Object value = row.get(keyFields.get(0));
            return value != null ? value.toString() : EngineConstants.GroupKey.NULL_VALUE_PLACEHOLDER;
        } else if (keyType == GroupKeyType.COMPOSITE) {
            return keyFields.stream()
                    .map(field -> {
                        Object value = row.get(field);
                        return value != null ? value.toString() : EngineConstants.GroupKey.NULL_VALUE_PLACEHOLDER;
                    })
                    .collect(Collectors.joining(EngineConstants.GroupKey.COMPOSITE_SEPARATOR));
        } else { // CUSTOM
            // 커스텀 키는 첫 번째 필드 값을 그대로 사용
            Object value = row.get(keyFields.get(0));
            return value != null ? value.toString() : EngineConstants.GroupKey.CUSTOM_DEFAULT_VALUE;
        }
    }

    /**
     * group_key 정보 로드
     *
     * TODO: groupKey/groupKeyType/groupKeyFields 제거됨
     * aggregate.group_by_fields 사용해야 함 - 현재는 기능 비활성화
     */
    private GroupKeyInfo loadGroupKeyInfo(String profileId) {
        // TODO: aggregate 기반으로 재구현 필요
        // 현재는 null 반환하여 기본 그룹 사용
        return null;
    }

    /**
     * 캐시 초기화 (스키마 변경 시 호출)
     */
    public void clearCache() {
        groupKeyCache.clear();
        log.info("group_key 캐시 초기화 완료");
    }

    /**
     * 특정 프로파일의 캐시 초기화
     */
    public void clearCache(String profileId) {
        groupKeyCache.remove(profileId);
        log.info("프로파일 {} 의 group_key 캐시 초기화", profileId);
    }

    /**
     * group_key 정보 내부 클래스
     */
    private static class GroupKeyInfo {
        private final String groupKey;
        private final GroupKeyType groupKeyType;
        private final List<String> keyFields;

        private GroupKeyInfo(String groupKey, GroupKeyType groupKeyType, List<String> keyFields) {
            this.groupKey = groupKey;
            this.groupKeyType = groupKeyType;
            this.keyFields = keyFields != null ? keyFields : Collections.emptyList();
        }

        public String getGroupKey() {
            return groupKey;
        }

        public GroupKeyType getGroupKeyType() {
            return groupKeyType;
        }

        public List<String> getKeyFields() {
            return keyFields;
        }
    }
}
