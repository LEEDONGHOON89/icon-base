package com.itmasters.icon.api.rule.adapter.persistence.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import com.itmasters.icon.common.domain.rule.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleConditionJacksonConfig {

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
    )
    @JsonSubTypes({
        @JsonSubTypes.Type(value = ComparisonCondition.class, name = "COMPARISON"),
        @JsonSubTypes.Type(value = AmountCondition.class, name = "AMOUNT"),
        @JsonSubTypes.Type(value = DurationCondition.class, name = "DURATION"),
        @JsonSubTypes.Type(value = TimeDifferenceCondition.class, name = "TIME_DIFFERENCE"),
        @JsonSubTypes.Type(value = TimeRangeCondition.class, name = "TIME_RANGE"),
        @JsonSubTypes.Type(value = TimeRangeWithMinutesCondition.class, name = "TIME_RANGE_MINUTES"),
        @JsonSubTypes.Type(value = BooleanCondition.class, name = "BOOLEAN"),
        @JsonSubTypes.Type(value = CountWithinCondition.class, name = "COUNT_WITHIN"),
        @JsonSubTypes.Type(value = SumWithinCondition.class, name = "SUM_WITHIN"),
        @JsonSubTypes.Type(value = DistinctCountCondition.class, name = "DISTINCT_COUNT")
    })
    public abstract static class RuleConditionMixIn {
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer ruleConditionPolymorphicMixIn() {
        return builder -> builder.mixIn(RuleCondition.class, RuleConditionMixIn.class);
    }
}
