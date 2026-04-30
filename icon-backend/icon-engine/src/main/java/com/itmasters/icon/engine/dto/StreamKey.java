package com.itmasters.icon.engine.dto;

import com.itmasters.icon.common.constants.EngineConstants;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineProfileEntity;
import lombok.*;

import java.util.Objects;

/**
 * Event Stream 키 - group_key + timestamp_field 조합
 * 
 * Event Stream 설계원칙:
 * - StreamKey = group_key_field + timestamp_field 
 * - 동일한 StreamKey를 사용하는 프로파일들은 중복 제거
 * - Set 자료구조로 자동 중복 제거 처리
 * - 예: CUS_ID + TRX_DT 조합이 같으면 하나의 타임라인만 생성
 * 
 * 중복 제거 효과:
 * - 3개 프로파일이 동일한 StreamKey 사용 시
 * - 3,000건 저장 → 1,000건 저장 (66.7% 절약)
 */
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class StreamKey {
    private String groupKey;    // 그룹핑 필드명 (예: CUS_ID, USER_ID)
    private String timestampKey;   // 타임라인 필드명 (예: TRX_DT, ORDER_DT)


    /**
     * 프로파일로부터 StreamKey 생성
     *
     * TODO: groupKey 컬럼 제거됨 - aggregate.group_by_fields로 재구현 필요
     * 임시로 기본값 사용 (entity_id_field 또는 DEFAULT_GROUP_KEY)
     *
     * [2026-04-23] timestampKey null 시 exception 대신 null 반환으로 소프트 처리
     *   - 호출부(Step2Processor)에서 null 체크 후 스킵 처리
     *   - 프로파일 저장 UI에서 timestampKey 미입력 시 매 수집마다 ERROR 로그 발생하던 문제 해결
     *
     * @return StreamKey 또는 null (timestampKey 미설정 시)
     */
    public static StreamKey of(EngineProfileEntity profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile cannot be null");
        }

        // TODO: aggregate.group_by_fields로 재구현 필요
        // 임시 fallback: entity_id_field가 있으면 사용, 없으면 기본값
        String groupKey = profile.getEntityIdField();
        if (groupKey == null || groupKey.trim().isEmpty()) {
            groupKey = EngineConstants.StreamKey.DEFAULT_GROUP_KEY;
        }

        String timestampKey = profile.getTimestampKey();

        // [2026-04-23] exception 대신 null 반환 - 호출부에서 스킵 처리
        if (timestampKey == null || timestampKey.trim().isEmpty()) {
            return null;
        }

        StreamKey streamKey = new StreamKey();
        streamKey.groupKey = groupKey.trim();
        streamKey.timestampKey = timestampKey.trim();
        return streamKey;
    }
    
    /**
     * 직접 생성 (테스트 또는 기본값 사용 시)
     */
    public static StreamKey of(String groupKey, String timestampKey) {
        if (groupKey == null || groupKey.trim().isEmpty()) {
            throw new IllegalArgumentException("GroupKey cannot be null or empty");
        }
        if (timestampKey == null || timestampKey.trim().isEmpty()) {
            throw new IllegalArgumentException("TimestampKey cannot be null or empty");
        }
        
        StreamKey streamKey = new StreamKey();
        streamKey.groupKey = groupKey.trim();
        streamKey.timestampKey = timestampKey.trim();
        return streamKey;
    }
    
    /**
     * 기본 StreamKey 생성 (활성 프로파일이 없을 때)
     */
    public static StreamKey defaultKey() {
        return of(EngineConstants.StreamKey.DEFAULT_GROUP_KEY,
                  EngineConstants.StreamKey.DEFAULT_TIMESTAMP_KEY);
    }
    
    /**
     * StreamKey 유효성 검증
     */
    public boolean isValid() {
        return groupKey != null && !groupKey.trim().isEmpty() &&
               timestampKey != null && !timestampKey.trim().isEmpty();
    }
    
    /**
     * StreamKey를 문자열로 표현 (디버깅용)
     */
    public String toDisplayString() {
        return String.format("%s%s%s", groupKey,
                EngineConstants.StreamKey.DISPLAY_SEPARATOR, timestampKey);
    }
}