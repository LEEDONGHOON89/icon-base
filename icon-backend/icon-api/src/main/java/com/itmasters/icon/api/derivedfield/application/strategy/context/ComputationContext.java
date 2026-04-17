package com.itmasters.icon.api.derivedfield.application.strategy.context;

import com.itmasters.icon.entity.DerivedRuleEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * 파생 필드 계산 컨텍스트
 *
 * Strategy에서 필요한 모든 데이터와 의존성을 제공합니다.
 */
@Getter
@AllArgsConstructor
public class ComputationContext {

    /**
     * 현재 이벤트 데이터 (mapped_data)
     */
    private final Map<String, Object> currentEventData;

    /**
     * 적용할 파생 필드 규칙
     */
    private final DerivedRuleEntity rule;

    /**
     * 각 Strategy에서 필요한 Repository를 가져오기 위한 Holder
     * (EntityRelationLookupStrategy, EventStreamLookupStrategy 등에서 사용)
     */
    private final RepositoryHolder repositoryHolder;

    /**
     * 이벤트 데이터에서 필드 값 추출 (null-safe)
     *
     * @param fieldName 필드명
     * @return 필드 값 (없으면 null)
     */
    public Object getFieldValue(String fieldName) {
        return currentEventData.get(fieldName);
    }

    /**
     * 이벤트 데이터에서 String 값 추출
     *
     * @param fieldName 필드명
     * @return String 값 (없거나 null이면 빈 문자열)
     */
    public String getStringValue(String fieldName) {
        Object value = currentEventData.get(fieldName);
        return value != null ? value.toString() : "";
    }

    /**
     * Rule의 computation_config JSON 파싱 없이 원본 반환
     *
     * @return computation_config JSON 문자열
     */
    public String getComputationConfigJson() {
        return rule.getComputationConfig();
    }
}
