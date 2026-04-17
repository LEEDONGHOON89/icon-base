package com.itmasters.icon.engine.processor.prep;

import com.itmasters.icon.engine.dto.MappedDataRow;
import com.itmasters.icon.engine.dto.Step1Result;
import com.itmasters.icon.engine.dto.Step4Result;
import com.itmasters.icon.engine.service.ExecDsMpService;
import com.itmasters.icon.engine.service.RelationExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * PREP-4: Enrich (엔티티 관계 추출) 프로세서
 *
 * 책임:
 * - entity_relation_rules에 정의된 규칙에 따라 관계 추출
 * - 설정 기반으로 동작 (하드코딩 없음)
 * - 완전히 확장 가능 (새 엔티티 타입/관계 타입 추가 시 코드 수정 불필요)
 *
 * 입력: Step1Result (PREP-3에서 파생 필드 계산된 데이터 포함)
 * 출력: Step4Result (관계 추출 통계)
 *
 * 동작 방식:
 * 1. DataSource별 활성 관계 규칙 조회 (entity_relation_rules 테이블)
 * 2. 각 데이터 행에 대해 규칙 적용
 * 3. 조건 만족 시 entity_relations에 관계 생성/업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Prep4EnrichProcessor {

    private final RelationExtractionService relationExtractionService;
    private final ExecDsMpService execDsMpService;

    /**
     * PREP-4 실행: 엔티티 관계 추출
     *
     * @param step1Result Step1 실행 결과 (파생 필드 포함)
     * @return PREP-4 실행 결과
     */
    public Step4Result execute(Step1Result step1Result) {
        List<MappedDataRow> mappedDataRows = execDsMpService.ensureMappedData(step1Result.getExecDsMpId());
        Step1Result enrichedStep1 = step1Result.withMappedData(mappedDataRows);

        log.info("========== PREP-4 시작 (엔티티 관계 추출) ==========");
        log.info("ExecDsMpId: {}, DataSource: {}, 데이터 건수: {}",
                enrichedStep1.getExecDsMpId(), enrichedStep1.getDataSourceId(), enrichedStep1.getTotalRows());

        // 매핑된 데이터가 없으면 빈 결과 반환
        if (!enrichedStep1.hasData()) {
            log.warn("처리할 매핑된 데이터가 없음 - dataSourceId: {}", enrichedStep1.getDataSourceId());
            return Step4Result.empty(enrichedStep1);
        }

        try {
            log.info("관계 추출 시작 - dataSourceId: {}", enrichedStep1.getDataSourceId());

            // 각 데이터 행에서 관계 추출 (RelationExtractionService 사용)
            for (MappedDataRow row : mappedDataRows) {
                try {
                    Map<String, Object> eventData = row.getRawData();

                    // 이벤트 시각 추출 (rawData에서 timestamp 필드 확인, 없으면 현재 시각 사용)
                    LocalDateTime eventTime = extractEventTime(eventData);

                    // RelationExtractionService를 통한 설정 기반 관계 추출
                    relationExtractionService.extractRelationsFromEvent(
                        enrichedStep1.getDataSourceId(),
                        eventData,
                        eventTime
                    );

                } catch (Exception e) {
                    log.warn("개별 행 관계 추출 실패 - mappedStorageId: {}",
                        row.getMappedDataStorageId(), e);
                    // 개별 행 실패해도 계속 진행
                }
            }

            log.info("관계 추출 완료 - dataSourceId: {}", enrichedStep1.getDataSourceId());

        } catch (Exception e) {
            log.error("관계 추출 실패 - dataSourceId: {}", enrichedStep1.getDataSourceId(), e);
        }

        log.info("========== PREP-4 완료 (엔티티 관계 추출) ==========");

        return Step4Result.empty(enrichedStep1);
    }

    /**
     * 이벤트 시각 추출 헬퍼 메서드
     *
     * @param eventData 이벤트 데이터
     * @return 이벤트 시각 (없으면 현재 시각)
     */
    private LocalDateTime extractEventTime(Map<String, Object> eventData) {
        // timestamp, event_time, created_at 등 일반적인 필드명 확인
        for (String fieldName : new String[]{"timestamp", "event_time", "created_at", "eventTime"}) {
            Object value = eventData.get(fieldName);
            if (value != null) {
                try {
                    if (value instanceof LocalDateTime) {
                        return (LocalDateTime) value;
                    } else if (value instanceof String) {
                        return LocalDateTime.parse((String) value);
                    }
                } catch (Exception e) {
                    log.trace("이벤트 시각 파싱 실패 - field: {}, value: {}", fieldName, value);
                }
            }
        }

        // 시각 정보 없으면 현재 시각 사용
        return LocalDateTime.now();
    }
}
