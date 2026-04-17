package com.itmasters.icon.common.domain.rule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 새로 추가/정리된 RuleOperator들의 파라미터 유효성 단위 테스트
 */
public class RuleOperatorNewOperatorsTest {

    @Test
    void testSequenceWithinParameterValidation() {
        assertTrue(RuleOperator.SEQUENCE_WITHIN.validateParameters("OTP_ISSUE,TRANSFER_OUT,180"));
        assertFalse(RuleOperator.SEQUENCE_WITHIN.validateParameters("OTP_ISSUE,TRANSFER_OUT")); // minutes 누락
        assertFalse(RuleOperator.SEQUENCE_WITHIN.validateParameters("OTP_ISSUE,TRANSFER_OUT,abc")); // 숫자 아님
    }

    @Test
    void testNoActivityWithinParameterValidation() {
        assertTrue(RuleOperator.NO_ACTIVITY_WITHIN.validateParameters("180"));
        assertFalse(RuleOperator.NO_ACTIVITY_WITHIN.validateParameters(""));
        assertFalse(RuleOperator.NO_ACTIVITY_WITHIN.validateParameters("abc"));
    }

    @Test
    void testFrequencyWithinTimeParameterValidation() {
        assertTrue(RuleOperator.FREQUENCY_WITHIN_TIME.validateParameters("1,3")); // 1시간 내 3회
        assertFalse(RuleOperator.FREQUENCY_WITHIN_TIME.validateParameters("1")); // 횟수 누락
        assertFalse(RuleOperator.FREQUENCY_WITHIN_TIME.validateParameters("x,3")); // 시간 숫자 아님
        assertFalse(RuleOperator.FREQUENCY_WITHIN_TIME.validateParameters("1,y")); // 횟수 숫자 아님
    }

    @Test
    void testExceedsLimitParameterValidation() {
        assertTrue(RuleOperator.EXCEEDS_LIMIT.validateParameters("DAILY_LIMIT"));
        assertTrue(RuleOperator.EXCEEDS_LIMIT.validateParameters("BALANCE_LIMIT"));
        assertFalse(RuleOperator.EXCEEDS_LIMIT.validateParameters(""));
    }

    @Test
    void testNoHistoryParameterValidation() {
        // 파라미터 없이 사용 가능
        assertTrue(RuleOperator.NO_HISTORY.validateParameters(null));
        assertTrue(RuleOperator.NO_HISTORY.validateParameters(""));
    }
}

