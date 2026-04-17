package com.itmasters.icon.engine.service;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectAggregateEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.DetectAggregateQueryRepository;
import com.itmasters.icon.engine.service.dto.SingleRunRequest;
import com.itmasters.icon.engine.service.dto.SingleRunResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.LocalDateTime.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 다양한 사례 샘플 및 시연
 */
@SpringBootTest
class SingleIngestServiceTest {

    @Autowired
    private SingleIngestService service;

    @Autowired
    private DetectAggregateQueryRepository detectAggQueryRepo;

    // DEV DB 기준: CUS001은 최근 30분 내 이미 ≥100k 이체가 충분히 존재하므로
    // 단 1건만 추가 전송해도 AGG_CUS011 집계가 생성되도록 설정
    @Test
    void ingestSingleEvent_and_run_full_pipeline_returns_ok() {
        LocalDateTime base = now();

        // 1st additional event
        SingleRunRequest req1 = new SingleRunRequest();
        req1.setDataSourceId("DS_API");
        Map<String, Object> row1 = new HashMap<>();
        row1.put("CUS_ID", "CUS001");
        row1.put("TRX_DT", base.plusMinutes(1).toString());
        row1.put("transaction_type", "이체");
        row1.put("transaction_amount", 120000);
        req1.setRow(row1);
        req1.setExecutedBy("test");
        SingleRunResponse r1 = service.ingestAndRun(req1);

        assertThat(r1).isNotNull();
        assertThat(r1.getExecDsMpId()).isNotNull();
        assertThat(r1.getSavedEventStreams()).isGreaterThanOrEqualTo(1);
        // 단건 전송만으로 집계 적재가 1건 이상 발생해야 함 (최근 30분 내 누적 조건 충족)
        assertThat(r1.getSavedAggregates()).isGreaterThanOrEqualTo(1);
    }

    // 집계되지 않아야함
    @Test
    @DisplayName("COUNT_WITHIN(동일 수취계좌 3회) - placeholder/blank 수취계좌는 제외되어야 한다")
    void countWithin_excludes_placeholder_and_blank_receiver_account() {
        LocalDateTime base = now();

        // 그룹키는 새 값으로 (기존 이력 영향 방지)
        String group = "CUS_TEST_PLACE_" + System.nanoTime();

        Long lastExecId = null;
        for (int i = 0; i < 3; i++) {
            SingleRunRequest req = new SingleRunRequest();
            req.setDataSourceId("DS_API");
            Map<String, Object> row = new HashMap<>();
            row.put("CUS_ID", group);
            row.put("TRX_DT", base.plusMinutes(i).toString());
            row.put("transaction_type", "이체");
            row.put("transaction_amount", 1000); // 소액
            // 1,2 번째는 placeholder '내계좌', 3번째는 공백 문자열 (모두 타인 아님)
            if (i < 2) {
                row.put("receiver_account", "내계좌");
            } else {
                row.put("receiver_account", "   ");
            }
            row.put("is_third_party", false);
            req.setRow(row);
            req.setExecutedBy("test");
            SingleRunResponse resp = service.ingestAndRun(req);
            lastExecId = resp.getExecDsMpId();
        }
        // 최종 실행 컨텍스트에서 AGG_CUS013이 기록되지 않았는지 확인
        List<DetectAggregateEntity> rows =
                detectAggQueryRepo.findByExecDsMpId(lastExecId);
        boolean hasCus013 = rows.stream().anyMatch(r -> "AGG_CUS013".equals(r.getAggregateId()));
        org.assertj.core.api.Assertions.assertThat(hasCus013).as("AGG_CUS013 must not be saved for placeholder/blank receiver_account").isFalse();
    }

    @Test
    @DisplayName("COUNT_WITHIN(동일 수취계좌 3회) - 정상 수취계좌로 3회면 집계되어야 한다")
    void countWithin_counts_when_valid_receiver_account() {
        LocalDateTime base = now();
        String group = "CUS_TEST_VALID";

        for (int i = 0; i < 3; i++) {
            SingleRunRequest req = new SingleRunRequest();
            req.setDataSourceId("DS_API");
            Map<String, Object> row = new HashMap<>();
            row.put("CUS_ID", group);
            row.put("TRX_DT", base.plusMinutes(i).toString());
            row.put("transaction_type", "이체");
            row.put("transaction_amount", 1000);
            row.put("receiver_account", "BENEF_A");
            row.put("is_third_party", true);
            req.setRow(row);
            req.setExecutedBy("test");
            SingleRunResponse resp = service.ingestAndRun(req);
            if (i == 2) {
                // 3번째에서 집계 1건 이상 저장 기대
                org.assertj.core.api.Assertions.assertThat(resp.getSavedAggregates()).as("should save aggregates for valid group").isGreaterThanOrEqualTo(1);
            }
        }
    }

    @Test
    @DisplayName("COUNT_WITHIN(동일 수취계좌 3회) - 공급사가 is_third_party=true를 제공하면 placeholder라도 탐지된다")
    void countWithin_counts_when_external_is_third_party_flag_provided() {
        LocalDateTime base = now();
        String group = "CUS_TEST_SUPPLIED_FLAG" + System.nanoTime();

        SingleRunResponse lastResponse = null;
        Long lastExecId = null;
        for (int i = 0; i < 3; i++) {
            SingleRunRequest req = new SingleRunRequest();
            req.setDataSourceId("DS_API");
            Map<String, Object> row = new HashMap<>();
            row.put("CUS_ID", group);
            row.put("TRX_DT", base.plusMinutes(i).toString());
            row.put("transaction_type", "이체");
            row.put("transaction_amount", 1000);
            row.put("receiver_account", "내계좌"); // placeholder 계좌
            row.put("is_third_party", true);       // 공급사에서 명시적으로 전달
            req.setRow(row);
            req.setExecutedBy("test");
            lastResponse = service.ingestAndRun(req);
            lastExecId = lastResponse.getExecDsMpId();
        }

        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getSavedAggregates())
                .as("외부 제공 플래그 덕분에 집계가 발생해야 한다")
                .isGreaterThanOrEqualTo(1);

        List<DetectAggregateEntity> aggregates = detectAggQueryRepo.findByExecDsMpId(lastExecId);
        boolean hasCus013 = aggregates.stream().anyMatch(r -> "AGG_CUS013".equals(r.getAggregateId()));
        assertThat(hasCus013)
                .as("AGG_CUS013 집계가 적재되어야 한다")
                .isTrue();
    }

    @Test
    @DisplayName("SEQUENCE_WITHIN(RU 로그인 후 10분 내 이체) - 10분 내 만족 시 집계되어야 한다")
    void sequenceWithin_ru_login_then_transfer_within_10min() {
        LocalDateTime base = now();
        String group = "CUS_TEST_RU_SEQ";

        // 1) RU 로그인 이벤트
        {
            SingleRunRequest req = new SingleRunRequest();
            req.setDataSourceId("DS_API");
            Map<String, Object> row = new HashMap<>();
            row.put("CUS_ID", group);
            row.put("TRX_DT", base.toString());
            row.put("event_type", "LOGIN");
            row.put("access_country", "RU");
            req.setRow(row);
            req.setExecutedBy("test");
            service.ingestAndRun(req);
        }

        // 2) 5분 후 이체 이벤트 (RULE_FIN_TXN_TRANSFER는 'TRANSFER' 코드 사용)
        {
            SingleRunRequest req = new SingleRunRequest();
            req.setDataSourceId("DS_API");
            Map<String, Object> row = new HashMap<>();
            row.put("CUS_ID", group);
            row.put("TRX_DT", base.plusMinutes(5).toString());
            row.put("transaction_type", "TRANSFER");
            row.put("transaction_amount", 1000);
            req.setRow(row);
            req.setExecutedBy("test");
            SingleRunResponse resp = service.ingestAndRun(req);
            Assertions.assertThat(resp.getSavedAggregates()).as("sequence should be detected within 10min").isGreaterThanOrEqualTo(1);
        }
    }
}
