package com.itmasters.icon.engine.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 동적 표현식 평가 유틸리티

 * Spring Expression Language (SpEL)을 사용하여 런타임에 표현식을 평가합니다.
 * entity_update_rules의 동적 필드 계산에 사용됩니다.

 * 지원 기능:
 * 1. 필드 값 추출: "customer_id", "trade_quantity"
 * 2. 산술 연산: "quantity - trade_quantity", "price * quantity"
 * 3. 문자열 조합: "customer_id + '_' + security_id"
 * 4. 조건식 평가: "quantity <= 0", "trade_type == 'SELL'"
 * 5. 복합 표현식: "(quantity - trade_quantity) * price"
 */
@Slf4j
@Component
public class ExpressionEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 표현식을 평가하여 문자열 결과 반환
     *
     * @param expression 평가할 표현식 (예: "customer_id", "quantity - trade_quantity")
     * @param context    표현식 평가에 사용할 컨텍스트 데이터 (Map<String, Object>)
     * @return 평가 결과 (문자열)
     */
    public String evaluateAsString(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }

        try {
            Object result = evaluate(expression, context, Object.class);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            log.error("표현식 평가 실패 (String) - expression: {}, context: {}", expression, context, e);
            throw new ExpressionEvaluationException("표현식 평가 실패: " + expression, e);
        }
    }

    /**
     * 표현식을 평가하여 숫자 결과 반환
     *
     * @param expression 평가할 표현식 (예: "quantity - trade_quantity")
     * @param context    표현식 평가에 사용할 컨텍스트 데이터
     * @return 평가 결과 (숫자)
     */
    public Number evaluateAsNumber(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }

        try {
            Object result = evaluate(expression, context, Object.class);

            if (result == null) {
                return null;
            }

            if (result instanceof Number) {
                return (Number) result;
            }

            // 문자열을 숫자로 변환 시도
            try {
                return Double.parseDouble(result.toString());
            } catch (NumberFormatException e) {
                throw new ExpressionEvaluationException("표현식 결과를 숫자로 변환할 수 없음: " + result);
            }
        } catch (Exception e) {
            log.error("표현식 평가 실패 (Number) - expression: {}, context: {}", expression, context, e);
            throw new ExpressionEvaluationException("표현식 평가 실패: " + expression, e);
        }
    }

    /**
     * 표현식을 평가하여 Boolean 결과 반환
     *
     * @param expression 평가할 표현식 (예: "quantity <= 0", "trade_type == 'SELL'")
     * @param context    표현식 평가에 사용할 컨텍스트 데이터
     * @return 평가 결과 (Boolean)
     */
    public Boolean evaluateAsBoolean(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }

        try {
            return evaluate(expression, context, Boolean.class);
        } catch (Exception e) {
            log.error("표현식 평가 실패 (Boolean) - expression: {}, context: {}", expression, context, e);
            throw new ExpressionEvaluationException("표현식 평가 실패: " + expression, e);
        }
    }

    /**
     * 표현식을 평가하여 지정된 타입의 결과 반환 (내부 메서드)
     *
     * @param expression 평가할 표현식
     * @param context    표현식 평가에 사용할 컨텍스트 데이터
     * @param returnType 반환 타입
     * @param <T>        반환 타입
     * @return 평가 결과
     */
    private <T> T evaluate(String expression, Map<String, Object> context, Class<T> returnType) {
        try {
            // SpEL 표현식 파싱
            Expression exp = parser.parseExpression(expression);

            // 평가 컨텍스트 생성 (context 데이터를 변수로 등록)
            StandardEvaluationContext evalContext = new StandardEvaluationContext();

            if (context != null) {
                // Map의 각 항목을 SpEL 변수로 등록
                context.forEach(evalContext::setVariable);

                // Map 자체도 root object로 설정하여 #변수명 없이도 접근 가능
                evalContext.setRootObject(context);
            }

            // 표현식 평가
            T result = exp.getValue(evalContext, returnType);

            log.debug("표현식 평가 성공 - expression: {}, result: {}", expression, result);
            return result;

        } catch (Exception e) {
            log.error("표현식 평가 중 오류 - expression: {}, context: {}", expression, context, e);
            throw e;
        }
    }

    /**
     * JSON 조건식 평가 (event_condition, entity_filter 등에 사용)

     * 예: {"trade_type": "SELL", "quantity": "> 0"}
     *
     * @param conditionJson JSON 형식의 조건 (Map으로 파싱된 상태)
     * @param context       평가 대상 데이터
     * @return 모든 조건이 만족되면 true
     */
    public boolean evaluateJsonCondition(Map<String, Object> conditionJson, Map<String, Object> context) {
        if (conditionJson == null || conditionJson.isEmpty()) {
            return true; // 조건이 없으면 항상 true
        }

        try {
            for (Map.Entry<String, Object> entry : conditionJson.entrySet()) {
                String fieldName = entry.getKey();
                Object expectedValue = entry.getValue();

                // 컨텍스트에서 실제 값 가져오기
                Object actualValue = context.get(fieldName);

                // 값 비교
                if (!matchesCondition(actualValue, expectedValue)) {
                    log.debug("조건 불일치 - field: {}, expected: {}, actual: {}",
                             fieldName, expectedValue, actualValue);
                    return false;
                }
            }

            return true; // 모든 조건 만족

        } catch (Exception e) {
            log.error("JSON 조건식 평가 실패 - condition: {}, context: {}", conditionJson, context, e);
            return false;
        }
    }

    /**
     * 조건 매칭 (단순 비교 및 표현식 평가)
     *
     * @param actualValue   실제 값
     * @param expectedValue 기대 값 (또는 조건 표현식)
     * @return 조건 만족 여부
     */
    private boolean matchesCondition(Object actualValue, Object expectedValue) {
        // null 체크
        if (expectedValue == null) {
            return actualValue == null;
        }

        // 문자열이면서 연산자를 포함하는 경우 (예: "> 0", "!= null")
        String expectedStr = expectedValue.toString().trim();

        if (expectedStr.startsWith(">") || expectedStr.startsWith("<") ||
            expectedStr.startsWith("=") || expectedStr.startsWith("!")) {
            // 표현식 평가: "actualValue > 0" 형태로 변환
            String expression = String.format("#actualValue %s", expectedStr);

            try {
                StandardEvaluationContext evalContext = new StandardEvaluationContext();
                evalContext.setVariable("actualValue", actualValue);

                Expression exp = parser.parseExpression(expression);
                Boolean result = exp.getValue(evalContext, Boolean.class);

                return result != null && result;
            } catch (Exception e) {
                log.warn("표현식 평가 실패, 단순 비교로 폴백 - expression: {}", expression, e);
                return String.valueOf(actualValue).equals(expectedStr);
            }
        }

        // 단순 값 비교
        if (actualValue == null) {
            return false;
        }

        // 타입이 다른 경우 문자열로 변환하여 비교
        return actualValue.toString().equals(expectedStr);
    }

    /**
     * 표현식 평가 예외
     */
    public static class ExpressionEvaluationException extends RuntimeException {
        public ExpressionEvaluationException(String message) {
            super(message);
        }

        public ExpressionEvaluationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
