package com.itmasters.icon.engine.processor.prep;

import com.itmasters.icon.engine.dto.MappedDataRow;
import com.itmasters.icon.engine.dto.Step1Result;
import com.itmasters.icon.engine.dto.Step3Result;
import com.itmasters.icon.engine.service.DerivedFieldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * PREP-3: Transform (파생 필드 계산) 프로세서
 *
 * 책임:
 * - mapped_data에 파생 필드 추가 (IS_DORMANT_ACCOUNT, is_third_party 등)
 * - derived_rules 테이블 기반으로 동적 계산
 *
 * 입력: Step1Result (mappedDataRows 포함)
 * 출력: Step3Result (파생 필드 계산 통계)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Prep3TransformProcessor {

    private final DerivedFieldService derivedFieldService;

    /**
     * PREP-3 실행: 파생 필드 계산
     *
     * @param step1Result Step1 실행 결과
     * @return PREP-3 실행 결과
     */
    public Step3Result execute(Step1Result step1Result) {
        Step1Result enrichedStep1 = step1Result;
        List<MappedDataRow> mappedDataRows = enrichedStep1.getMappedDataRows();
        if (mappedDataRows == null) {
            mappedDataRows = List.of();
        }

        log.info("========== PREP-3 시작 (파생 필드 계산) ==========");
        log.info("ExecDsMpId: {}, DataSource: {}, 데이터 건수: {}",
                enrichedStep1.getExecDsMpId(), enrichedStep1.getDataSourceId(), enrichedStep1.getTotalRows());

        // 매핑된 데이터가 없으면 빈 결과 반환
        if (!enrichedStep1.hasData()) {
            log.warn("처리할 매핑된 데이터가 없음 - dataSourceId: {}", enrichedStep1.getDataSourceId());
            return Step3Result.empty(enrichedStep1);
        }

        int calculatedFieldsCount = 0;

        try {
            log.info("파생 필드 계산 시작 - dataSourceId: {}", enrichedStep1.getDataSourceId());

            // MappedDataRow → Map<String, Object> 변환
            List<Map<String, Object>> rawDataList = mappedDataRows.stream()
                .map(MappedDataRow::getRawData)
                .toList();

            // 파생 필드 계산 및 추가
            derivedFieldService.computeFields(enrichedStep1.getDataSourceId(), rawDataList);

            calculatedFieldsCount = rawDataList.size();
            log.info("파생 필드 계산 완료 - {} rows processed", calculatedFieldsCount);

            // DEBUG: 첫 번째 행 확인
            if (!rawDataList.isEmpty()) {
                Map<String, Object> firstRow = rawDataList.get(0);
                log.debug("🔍 파생 필드 계산 후 첫 번째 행 샘플: {}", firstRow);

                // 파생 필드 예시 로깅
                if (firstRow.containsKey("is_third_party")) {
                    log.info("✅ is_third_party 필드 생성됨: {}", firstRow.get("is_third_party"));
                }
                if (firstRow.containsKey("IS_DORMANT_ACCOUNT")) {
                    log.info("✅ IS_DORMANT_ACCOUNT 필드 생성됨: {}", firstRow.get("IS_DORMANT_ACCOUNT"));
                }
            }

        } catch (Exception e) {
            log.error("파생 필드 계산 실패 - dataSourceId: {}", enrichedStep1.getDataSourceId(), e);
            // 파생 필드 계산 실패해도 다음 단계는 계속 진행
        }

        log.info("========== PREP-3 완료 (파생 필드 계산) ==========");
        log.info("처리된 행 수: {} 건", calculatedFieldsCount);

        return Step3Result.empty(enrichedStep1);
    }

    /**
     * Row 파이프라인 전용 실행
     */
    @Transactional
    public Step3Result processRow(Step1Result context, MappedDataRow mappedDataRow) {
        if (mappedDataRow == null) {
            return Step3Result.empty(context);
        }

        try {
            derivedFieldService.computeFields(
                    context.getDataSourceId(),
                    List.of(mappedDataRow.getRawData())
            );
            return Step3Result.builder()
                    .step1Result(context)
                    .calculatedFieldsCount(1)
                    .executionTime(System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            log.error("파생 필드 계산 실패 - mappedStorageId: {}",
                    mappedDataRow.getMappedDataStorageId(), e);
            return Step3Result.empty(context);
        }
    }
}
