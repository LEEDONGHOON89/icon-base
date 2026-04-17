package com.itmasters.icon.engine.processor;

import com.itmasters.icon.engine.dto.MappedDataRow;
import com.itmasters.icon.engine.dto.Step1Result;
import com.itmasters.icon.engine.dto.Step3Result;
import com.itmasters.icon.engine.service.DerivedFieldService;
import com.itmasters.icon.engine.service.ExecDsMpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Step3: 파생 필드 계산 프로세서
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
public class Step3_DerivedFieldProcessor {

    private final DerivedFieldService derivedFieldService;
    private final ExecDsMpService execDsMpService;

    /**
     * Step3 실행: 파생 필드 계산
     *
     * @param step1Result Step1 실행 결과
     * @return Step3 실행 결과
     */
    public Step3Result execute(Step1Result step1Result) {
        List<MappedDataRow> mappedDataRows = execDsMpService.ensureMappedData(step1Result.getExecDsMpId());
        Step1Result enrichedStep1 = step1Result.withMappedData(mappedDataRows);

        log.info("========== Step3 시작 (파생 필드 계산) ==========");
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

        log.info("========== Step3 완료 (파생 필드 계산) ==========");
        log.info("처리된 행 수: {} 건", calculatedFieldsCount);

        // 임시로 빈 Step3Result 반환 (나중에 수정)
        return Step3Result.empty(enrichedStep1);
    }
}
