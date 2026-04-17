package com.itmasters.icon.engine.processor.detect;

import com.itmasters.icon.engine.dto.MappedDataRow;
import com.itmasters.icon.engine.dto.Step1Result;
import com.itmasters.icon.engine.dto.Step4Result;
import com.itmasters.icon.engine.dto.StreamKey;
import com.itmasters.icon.engine.dto.EventStreamResult;
import com.itmasters.icon.engine.service.EventStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.itmasters.icon.engine.processor.Step2Processor;

import java.util.List;
import java.util.Set;

/**
 * DET-1: Stream (Event Stream 저장) 프로세서
 *
 * 책임:
 * - event_stream 테이블에 이벤트 저장
 * - StreamKey 수집 및 중복 제거
 * - 필드 존재 여부 사전 점검
 *
 * 입력: Step1Result (파생 필드가 추가된 mappedDataRows 포함)
 * 출력: Step4Result (streamResult 포함)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Det1StreamProcessor {

    private final EventStreamService eventStreamService;
    private final Step2Processor step2Processor; // collectActiveStreamKeys 재사용

    /**
     * DET-1 실행: Event Stream 저장 (단일 Row 컨텍스트)
     */
    public Step4Result processRow(Step1Result rowContext, String executedBy) {
        if (rowContext == null || !rowContext.hasData()) {
            return Step4Result.empty(rowContext);
        }

        return executeRowContext(rowContext, executedBy);
    }


    private Step4Result executeRowContext(Step1Result rowContext, String executedBy) {
        log.info("========== DET-1 시작 (Event Stream 저장) ==========");
        log.info("ExecDsMpId: {}, DataSource: {}, 데이터 건수: {}",
                rowContext.getExecDsMpId(), rowContext.getDataSourceId(), rowContext.getTotalRows());

        // 매핑된 데이터가 없으면 빈 결과 반환
        if (!rowContext.hasData()) {
            log.warn("처리할 매핑된 데이터가 없음 - dataSourceId: {}", rowContext.getDataSourceId());
            return Step4Result.empty(rowContext);
        }

        // 1. 활성 프로파일들의 StreamKey 수집 및 중복 제거
        Set<StreamKey> activeStreamKeys = step2Processor.collectActiveStreamKeys(rowContext.getDataSourceId());

        if (activeStreamKeys.isEmpty()) {
            log.warn("활성 프로파일이 없음 - dataSourceId: {}, 기본 StreamKey 사용",
                    rowContext.getDataSourceId());
            // Event Stream 설계원칙에 따른 기본값 처리
            activeStreamKeys.add(StreamKey.defaultKey());
            log.info("기본 StreamKey 적용: {}", StreamKey.defaultKey().toDisplayString());
        }

        log.info("활성 StreamKey {} 개 수집됨: {}", activeStreamKeys.size(),
                activeStreamKeys.stream().map(StreamKey::toDisplayString).toList());

        // 2. 사전 점검: StreamKey에 필요한 필드 존재 여부 로그
        preflightCheck(rowContext, activeStreamKeys);

        // 3. Event Stream 저장 (중복 제거 적용)
        EventStreamResult streamResult = eventStreamService.saveEventStream(
                rowContext, activeStreamKeys, executedBy);

        // 결과 요약 출력
        printSummary(streamResult);

        log.info("========== DET-1 완료 (Event Stream 저장) ==========");
        log.info("저장된 이벤트: {} 건", streamResult.getTotalEvents());

        // 실제 streamResult 반환
        return Step4Result.builder()
                .step1Result(rowContext)
                .streamResult(streamResult)
                .activeStreamKeys(activeStreamKeys)
                .executionTime(System.currentTimeMillis())
                .build();
    }

    /**
     * StreamKey별로 group_key 필드와 timestamp 필드 존재 여부를 간단 점검 (로그 전용)
     */
    protected void preflightCheck(Step1Result step1Result, Set<StreamKey> activeStreamKeys) {
        if (activeStreamKeys == null || activeStreamKeys.isEmpty()) return;
        if (step1Result.getMappedDataRows() == null || step1Result.getMappedDataRows().isEmpty()) return;

        for (StreamKey key : activeStreamKeys) {
            String gk = key.getGroupKey();
            String ts = key.getTimestampKey();
            int n = step1Result.getMappedDataRows().size();
            int hasGk = 0, hasTs = 0;
            for (var mdr : step1Result.getMappedDataRows()) {
                var row = mdr.getRawData();
                if (row.containsKey(gk)) hasGk++;
                if (row.containsKey(ts)) hasTs++;
            }
            if (hasGk < n) {
                log.warn("[DET-1 Preflight] group_key 필드 누락: field='{}' {}/{} (profile timestampKey='{}')",
                        gk, (n - hasGk), n, ts);
            }
            if (hasTs < n) {
                log.warn("[DET-1 Preflight] timestamp 필드 누락: field='{}' {}/{} (profile groupKey='{}')",
                        ts, (n - hasTs), n, gk);
            }
            if (hasGk == n && hasTs == n) {
                log.debug("[DET-1 Preflight] OK - group_key='{}', timestamp='{}' 모두 존재", gk, ts);
            }
        }
    }

    /**
     * 결과 요약 출력
     */
    private void printSummary(EventStreamResult streamResult) {
        log.info("===== DET-1 처리 요약 =====");

        if (streamResult.isSuccess()) {
            log.info("상태: 성공");
            log.info("저장된 이벤트: {} 건", streamResult.getTotalEvents());

            if (streamResult.getTotalEvents() > 0) {
                log.info("첫 번째 이벤트 ID: {}", streamResult.getFirstEventStreamId());
                log.info("마지막 이벤트 ID: {}", streamResult.getLastEventStreamId());
            }
        } else {
            log.error("상태: 실패");
            log.error("오류 메시지: {}", streamResult.getErrorMessage());
        }
    }
}
