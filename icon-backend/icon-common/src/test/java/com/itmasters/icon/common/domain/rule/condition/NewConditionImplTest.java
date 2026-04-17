package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleConditionFactory;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NewConditionImplTest {

    @Test
    void noActivityWithin_basicEvaluate() {
        var cond = RuleConditionFactory.createCondition("account_id", RuleOperator.NO_ACTIVITY_WITHIN, 180);
        assertTrue(cond.isValid());

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("account_id_no_activity_within_180", true);
        assertTrue(cond.matches(ctx));

        ctx.put("account_id_no_activity_within_180", false);
        assertFalse(cond.matches(ctx));
    }

    @Test
    void sequenceWithin_basicEvaluate() {
        var cond = RuleConditionFactory.createCondition("sequence", RuleOperator.SEQUENCE_WITHIN, "OTP_ISSUE,TRANSFER_OUT,180");
        assertTrue(cond.isValid());

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("sequence_OTP_ISSUE_TRANSFER_OUT_within_180", true);
        assertTrue(cond.matches(ctx));

        ctx.put("sequence_OTP_ISSUE_TRANSFER_OUT_within_180", false);
        assertFalse(cond.matches(ctx));
    }
}

