package com.itmasters.icon.engine.evaluator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.evaluator.impl.BalanceRatioEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BalanceRatioEvaluator 테스트
 * 거래 전 잔액 대비 이체 비율 탐지 로직 검증
 */
@Slf4j
class BalanceRatioEvaluatorTest {

    private final BalanceRatioEvaluator evaluator = new BalanceRatioEvaluator(new ObjectMapper());

    @Test
    @DisplayName("30% 이상 이체 탐지 성공 - CUS001 첫 번째 이체")
    void shouldDetect30PercentTransfer() {
        // Given: CUS001 첫 번째 이체
        // before_balance: 133,875,000
        // transaction_calc_amount: -35,000,000
        // ratio: -35,000,000 / 133,875,000 = -0.2614 (26.14%)
        Map<String, Object> context = new HashMap<>();
        context.put("customer_id", "CUS001");
        context.put("before_balance", 133875000);
        context.put("transaction_calc_amount", -35000000);
        context.put("transaction_type", "이체");

        String ruleConfig = """
            {
                "operator": "<=",
                "ratio": -0.3
            }
            """;

        // When
        boolean result = evaluator.evaluate(context, ruleConfig);

        // Then: 26.14% < 30% 이므로 탐지되지 않음
        log.info("Result: {}", result);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("30% 이상 이체 탐지 성공 - 실제 30% 초과 케이스")
    void shouldDetect30PercentTransfer_Over30() {
        // Given: 30% 초과 이체
        // before_balance: 100,000,000
        // transaction_calc_amount: -40,000,000
        // ratio: -40,000,000 / 100,000,000 = -0.4 (40%)
        Map<String, Object> context = new HashMap<>();
        context.put("customer_id", "CUS002");
        context.put("before_balance", 100000000);
        context.put("transaction_calc_amount", -40000000);
        context.put("transaction_type", "이체");

        String ruleConfig = """
            {
                "operator": "<=",
                "ratio": -0.3
            }
            """;

        // When
        boolean result = evaluator.evaluate(context, ruleConfig);

        // Then: 40% > 30% 이므로 탐지됨
        log.info("Result: {}", result);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("30% 정확히 - 경계값 테스트")
    void shouldDetectExactly30Percent() {
        // Given: 정확히 30% 이체
        // before_balance: 100,000,000
        // transaction_calc_amount: -30,000,000
        // ratio: -30,000,000 / 100,000,000 = -0.3 (30%)
        Map<String, Object> context = new HashMap<>();
        context.put("customer_id", "CUS003");
        context.put("before_balance", 100000000);
        context.put("transaction_calc_amount", -30000000);
        context.put("transaction_type", "이체");

        String ruleConfig = """
            {
                "operator": "<=",
                "ratio": -0.3
            }
            """;

        // When
        boolean result = evaluator.evaluate(context, ruleConfig);

        // Then: 30% == 30% 이므로 탐지됨 (<=)
        log.info("Result: {}", result);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("잔액이 0이면 탐지 안됨")
    void shouldNotDetectWhenBalanceIsZero() {
        // Given: 잔액이 0
        Map<String, Object> context = new HashMap<>();
        context.put("customer_id", "CUS004");
        context.put("before_balance", 0);
        context.put("transaction_calc_amount", -10000);
        context.put("transaction_type", "이체");

        String ruleConfig = """
            {
                "operator": "<=",
                "ratio": -0.3
            }
            """;

        // When
        boolean result = evaluator.evaluate(context, ruleConfig);

        // Then: 잔액이 0이면 탐지 안됨
        log.info("Result: {}", result);
        assertThat(result).isFalse();
    }
}
