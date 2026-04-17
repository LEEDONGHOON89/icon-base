package com.itmasters.icon.engine.validator;

import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineDataSourceSchemaRepository;
import com.itmasters.icon.engine.config.Const;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceSchemaEntity;
import com.itmasters.icon.engine.dto.SchemaValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.itmasters.icon.engine.config.Const.METADATA_LINE_NUMBER;

/**
 * 데이터소스 스키마 검증기
 * DataSource에서 읽은 데이터가 DataSourceSchema에 정의된 스키마와 일치하는지 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchemaValidator {
    
    private final EngineDataSourceSchemaRepository schemaRepository;
    
    /**
     * 데이터 스키마 검증
     * 
     * @param dataSourceId 데이터소스 ID
     * @param data 검증할 데이터 목록
     * @return 검증 결과
     */
    public SchemaValidationResult validate(String dataSourceId, List<Map<String, Object>> data) {
        log.info("스키마 검증 시작 - DataSourceId: {}, 데이터 건수: {}", dataSourceId, data.size());
        
        if (data.isEmpty()) {
            return SchemaValidationResult.success("데이터가 없습니다.");
        }
        
        // 데이터소스 스키마 조회
        List<EngineDataSourceSchemaEntity> schemas = schemaRepository.findByDataSourceId(dataSourceId);
        if (schemas.isEmpty()) {
            log.error("스키마 정의가 없습니다 - DataSourceId: {}", dataSourceId);
            return SchemaValidationResult.failure("스키마 정의가 없습니다. 데이터 검증을 수행할 수 없습니다.", 
                    List.of("DataSourceId: " + dataSourceId + "에 대한 스키마가 정의되지 않았습니다."), 
                    List.of());
        }
        
        // 스키마를 Map으로 변환 (field_name과 standard_field_id 모두 허용)
        Map<String, EngineDataSourceSchemaEntity> schemaMap = new HashMap<>();
        for (EngineDataSourceSchemaEntity schema : schemas) {
            // field_name으로 매핑
            schemaMap.put(schema.getFieldName(), schema);
            // standard_field_id가 있으면 추가 매핑
            if (schema.getStandardFieldId() != null) {
                schemaMap.put(schema.getStandardFieldId(), schema);
            }
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 첫 번째 레코드로 기본 검증
        Map<String, Object> firstRecord = data.get(0);
        Set<String> actualFields = firstRecord.keySet();
        Set<String> schemaFields = schemaMap.keySet();

        // 1. 스키마에 정의되지 않은 필드 체크
        Set<String> extraFields = new HashSet<>(actualFields);
        extraFields.removeAll(schemaFields);

        // 메타데이터 제거
        extraFields.remove(METADATA_LINE_NUMBER);
        if (!extraFields.isEmpty()) {
            warnings.add("스키마에 정의되지 않은 필드: " + extraFields);
            log.warn("입력 필드: {}", actualFields);
            log.warn("스키마 허용 필드: {}", schemaFields);
        }

        // 2. 필수 필드 누락 체크
        // 각 필수 스키마에 대해 field_name 또는 standard_field_id 중 하나라도 있으면 OK
        Set<String> missingFields = schemas.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsRequired()))
                .filter(s -> {
                    // field_name과 standard_field_id 둘 다 확인
                    boolean hasFieldName = actualFields.contains(s.getFieldName());
                    boolean hasStandardField = s.getStandardFieldId() != null &&
                                                actualFields.contains(s.getStandardFieldId());
                    return !hasFieldName && !hasStandardField;
                })
                .map(s -> s.getFieldName() + " (또는 " + s.getStandardFieldId() + ")")
                .collect(Collectors.toSet());
        
        if (!missingFields.isEmpty()) {
            errors.add("필수 필드 누락: " + missingFields);
        }
        
        // 3. 샘플 데이터로 타입 검증 (처음 10개 레코드만)
        int sampleSize = Math.min(10, data.size());
        for (int i = 0; i < sampleSize; i++) {
            Map<String, Object> record = data.get(i);
            
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                
                EngineDataSourceSchemaEntity schema = schemaMap.get(fieldName);
                if (schema != null && !schema.isValidType(value)) {
                    String error = String.format("레코드 %d: 필드 '%s'의 타입 불일치 (예상: %s, 실제: %s)", 
                            i + 1, fieldName, schema.getDataType(),
                            value != null ? value.getClass().getSimpleName() : "null");
                    errors.add(error);
                }
            }
        }
        
        // 결과 반환
        if (!errors.isEmpty()) {
            log.error("스키마 검증 실패 - errors: {}, warnings: {}", errors, warnings);
            return SchemaValidationResult.failure("스키마 검증 실패", errors, warnings);
        } else if (!warnings.isEmpty()) {
            log.warn("스키마 검증 경고 - warnings: {}", warnings);
            return SchemaValidationResult.successWithWarnings("스키마 검증 성공 (경고 있음)", warnings);
        } else {
            return SchemaValidationResult.success("스키마 검증 성공");
        }
    }
}