package com.itmasters.icon.engine.processor.prep;

import com.itmasters.icon.engine.adapter.out.persistence.entity.PatternRelationEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityRelationFieldEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.PatternRelationRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityRelationFieldRepository;
import com.itmasters.icon.engine.dto.MappedDataRow;
import com.itmasters.icon.engine.dto.Step1Result;
import com.itmasters.icon.engine.dto.Step4Result;
import com.itmasters.icon.engine.service.ExecDsMpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PREP-5: Pattern Discovery (패턴 자동 발견) 프로세서
 *
 * 책임:
 * - 데이터 분석을 통한 엔티티 간 관계 패턴 자동 발견
 * - 빈도 기반 패턴 추출
 * - pattern_relations 테이블에 저장 (검증 대기)
 *
 * 입력: Step1Result (PREP-4까지 완료된 데이터)
 * 출력: Step4Result (발견된 패턴 통계)
 *
 * 발견 방법:
 * 1. 빈도 분석: 두 필드가 함께 나타나는 빈도 측정
 * 2. 신뢰도 계산: 빈도 / 전체 데이터 비율
 * 3. 임계값 이상만 패턴으로 등록
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Prep5PatternDiscoveryProcessor {

    private final PatternRelationRepository patternRelationRepository;
    private final EntityRelationFieldRepository entityRelationFieldRepository;
    private final ExecDsMpService execDsMpService;

    // 설정 가능한 임계값 (테스트용으로 낮춤)
    private static final int MIN_OCCURRENCE = 2;        // 최소 발견 빈도 (테스트: 2회)
    private static final BigDecimal MIN_CONFIDENCE = new BigDecimal("0.50");  // 최소 신뢰도 (테스트: 50%)

    /**
     * PREP-5 실행: 패턴 자동 발견
     *
     * @param step1Result Step1 실행 결과
     * @return PREP-5 실행 결과
     */
    public Step4Result execute(Step1Result step1Result) {
        List<MappedDataRow> mappedDataRows = execDsMpService.ensureMappedData(step1Result.getExecDsMpId());
        Step1Result enrichedStep1 = step1Result.withMappedData(mappedDataRows);

        log.info("========== PREP-5 시작 (패턴 자동 발견) ==========");
        log.info("ExecDsMpId: {}, DataSource: {}, 데이터 건수: {}",
                enrichedStep1.getExecDsMpId(), enrichedStep1.getDataSourceId(), enrichedStep1.getTotalRows());

        if (!enrichedStep1.hasData()) {
            log.warn("처리할 매핑된 데이터가 없음 - dataSourceId: {}", enrichedStep1.getDataSourceId());
            return Step4Result.empty(enrichedStep1);
        }

        try {
            // 1. 관계 후보 필드 조회 (entity_relation_fields 테이블)
            List<EntityRelationFieldEntity> relationFields =
                entityRelationFieldRepository.findByDataSourceIdAndIsEnabledOrderByPriorityDesc(
                    enrichedStep1.getDataSourceId(),
                    true
                );

            if (relationFields.isEmpty()) {
                log.warn("활성화된 관계 후보 필드가 없음 - dataSourceId: {}", enrichedStep1.getDataSourceId());
                return Step4Result.empty(enrichedStep1);
            }

            log.info("관계 후보 필드: {} 개", relationFields.size());
            relationFields.forEach(field ->
                log.debug("  - {} (역할: {}, 우선순위: {})",
                    field.getFieldName(), field.getFieldRole(), field.getPriority())
            );

            // 2. 필드 조합 빈도 분석
            Map<FieldPair, Integer> fieldPairFrequency = analyzeFieldPairFrequency(
                mappedDataRows,
                relationFields
            );

            // 3. 패턴 추출 및 저장
            int discoveredPatterns = extractAndSavePatterns(
                enrichedStep1.getDataSourceId(),
                fieldPairFrequency,
                mappedDataRows.size()
            );

            log.info("발견된 패턴: {} 개", discoveredPatterns);

        } catch (Exception e) {
            log.error("패턴 발견 실패 - dataSourceId: {}", enrichedStep1.getDataSourceId(), e);
        }

        log.info("========== PREP-5 완료 (패턴 자동 발견) ==========");
        return Step4Result.empty(enrichedStep1);
    }

    /**
     * 필드 조합 빈도 분석 (entity_relation_fields 기반)
     */
    private Map<FieldPair, Integer> analyzeFieldPairFrequency(
            List<MappedDataRow> mappedDataRows,
            List<EntityRelationFieldEntity> relationFields) {

        Map<FieldPair, Integer> frequency = new HashMap<>();

        // 관계 후보 필드명 Set으로 변환 (빠른 검색)
        Set<String> candidateFields = relationFields.stream()
            .map(EntityRelationFieldEntity::getFieldName)
            .collect(Collectors.toSet());

        for (MappedDataRow row : mappedDataRows) {
            Map<String, Object> data = row.getRawData();

            // 데이터에 존재하는 관계 후보 필드만 추출
            List<String> availableFields = data.keySet().stream()
                .filter(candidateFields::contains)
                .toList();

            // 필드 쌍 생성 (순서 있음: from → to)
            for (int i = 0; i < availableFields.size(); i++) {
                for (int j = 0; j < availableFields.size(); j++) {
                    if (i != j) {
                        String fromField = availableFields.get(i);
                        String toField = availableFields.get(j);

                        Object fromValue = data.get(fromField);
                        Object toValue = data.get(toField);

                        if (fromValue != null && toValue != null) {
                            FieldPair pair = new FieldPair(fromField, toField);
                            frequency.merge(pair, 1, Integer::sum);
                        }
                    }
                }
            }
        }

        return frequency;
    }

    /**
     * 패턴 추출 및 저장
     */
    @Transactional
    protected int extractAndSavePatterns(
            String dataSourceId,
            Map<FieldPair, Integer> fieldPairFrequency,
            int totalRows) {

        int discoveredCount = 0;

        for (Map.Entry<FieldPair, Integer> entry : fieldPairFrequency.entrySet()) {
            FieldPair pair = entry.getKey();
            int occurrences = entry.getValue();

            // 임계값 검사
            if (occurrences < MIN_OCCURRENCE) {
                continue;
            }

            // 신뢰도 계산 (빈도 / 전체 데이터)
            BigDecimal confidence = BigDecimal.valueOf(occurrences)
                .divide(BigDecimal.valueOf(totalRows), 4, RoundingMode.HALF_UP);

            if (confidence.compareTo(MIN_CONFIDENCE) < 0) {
                continue;
            }

            // 엔티티 타입 추론 (필드명에서)
            String fromEntityType = inferEntityType(pair.fromField);
            String toEntityType = inferEntityType(pair.toField);

            // 관계 타입 추론
            String relationType = inferRelationType(fromEntityType, toEntityType);

            try {
                // 기존 패턴 확인
                Optional<PatternRelationEntity> existing = patternRelationRepository
                    .findByDataSourceIdAndFromEntityTypeAndFromIdFieldAndRelationTypeAndToEntityTypeAndToIdField(
                        dataSourceId, fromEntityType, pair.fromField, relationType, toEntityType, pair.toField
                    );

                if (existing.isPresent()) {
                    // 기존 패턴 업데이트
                    PatternRelationEntity pattern = existing.get();
                    pattern.incrementOccurrence();
                    pattern.updateConfidence(confidence);
                    patternRelationRepository.save(pattern);
                    log.debug("패턴 업데이트: {} --{}--> {}", fromEntityType, relationType, toEntityType);

                } else {
                    // 새 패턴 생성
                    PatternRelationEntity newPattern = PatternRelationEntity.of(
                        dataSourceId,
                        fromEntityType,
                        pair.fromField,
                        relationType,
                        toEntityType,
                        pair.toField,
                        confidence,
                        occurrences,
                        "FREQUENCY"
                    );
                    patternRelationRepository.save(newPattern);
                    discoveredCount++;

                    log.info("신규 패턴 발견: {} ({}) --{}--> {} ({}) - 신뢰도: {}%, 빈도: {}",
                        fromEntityType, pair.fromField, relationType, toEntityType, pair.toField,
                        confidence.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP),
                        occurrences);
                }

            } catch (Exception e) {
                log.warn("패턴 저장 실패 - from: {}, to: {}", pair.fromField, pair.toField, e);
            }
        }

        return discoveredCount;
    }

    /**
     * 필드명에서 엔티티 타입 추론
     * 예: "customer_id" → "CUSTOMER"
     */
    private String inferEntityType(String fieldName) {
        if (fieldName.endsWith("_id")) {
            String entityName = fieldName.substring(0, fieldName.length() - 3);  // "_id" 제거
            return entityName.toUpperCase();
        }
        return fieldName.toUpperCase();
    }

    /**
     * 관계 타입 추론 (간단한 규칙 기반)
     */
    private String inferRelationType(String fromType, String toType) {
        // 기본 규칙
        if (fromType.equals("CUSTOMER") && toType.equals("ACCOUNT")) {
            return "OWNS";
        }
        if (fromType.equals("DEVICE") && toType.equals("ACCOUNT")) {
            return "USES";
        }
        if (fromType.equals("ACCOUNT") && toType.equals("ACCOUNT")) {
            return "TRANSFERS_TO";
        }
        if (fromType.equals("CUSTOMER") && toType.equals("MERCHANT")) {
            return "BUYS_FROM";
        }

        // 기본값
        return "ASSOCIATED_WITH";
    }

    /**
     * 필드 쌍 (From/To)
     */
    private record FieldPair(String fromField, String toField) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FieldPair)) return false;
            FieldPair that = (FieldPair) o;
            return Objects.equals(fromField, that.fromField) &&
                   Objects.equals(toField, that.toField);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fromField, toField);
        }
    }
}
