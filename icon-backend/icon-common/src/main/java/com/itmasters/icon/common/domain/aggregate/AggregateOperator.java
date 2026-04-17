package com.itmasters.icon.common.domain.aggregate;

import lombok.Getter;

/**
 * 집계 연산자 정의
 * 
 * 📌 센서 컬럼 사용 규칙
 * 각 연산자는 rules 테이블의 센서 컬럼을 다르게 사용합니다:

   * -predicate_sensor_id: 집계/확인 대상 센서 (대부분의 연산자)
   * -anchor_sensor_id: 앵커/트리거 센서 (ABSENCE_WITHIN 선택적)
   * -prev_sensor_id: 시퀀스 이전 센서 (SEQUENCE_WITHIN 전용)
   * -next_sensor_id: 시퀀스 다음 센서 (SEQUENCE_WITHIN 전용)

 * 
 * 📋 연산자별 사용 컬럼
 * 
 *   연산자사용 컬럼
 *   COUNT/SUM/AVG/MAX/MIN/DISTINCT/FIRSTpredicate_sensor_id
 *   ABSENCE_WITHINpredicate_sensor_id, anchor_sensor_id (선택)
 *   SEQUENCE_WITHINprev_sensor_id, next_sensor_id
 * 
 * 
 * 💡 프로그래밍 활용
 * 
 * // 사용 예시: 연산자가 어떤 컬럼을 사용하는지 확인
 * String[] columns = AggregateOperator.COUNT_WITHIN.getUsedColumns();
 * // columns = ["predicate_sensor_id"]
 * 
 * String[] columns2 = AggregateOperator.SEQUENCE_WITHIN.getUsedColumns();
 * // columns2 = ["prev_sensor_id", "next_sensor_id"]
 * 
 */
@Getter
public enum AggregateOperator {
    /**
     * 시간 내 횟수
     * 의미: window_minutes 기간 내 predicate 센서 이벤트가 threshold_count 이상인지 확인
     * 예시: 7일 내 고액 이체(R_HIGH_AMOUNT) 5회 이상
     */
    COUNT_WITHIN("시간 내 횟수", "predicate_sensor_id"),
    
    /**
     * 시간 내 합계
     * 의미: window_minutes 기간 내 predicate 센서 이벤트의 aggregation_field 합계가 threshold_amount 이상인지 확인
     * 예시: 1일 내 이체 금액 합계 1억 이상
     */
    SUM_WITHIN("시간 내 합계", "predicate_sensor_id"),
    
    /**
     * 시간 내 평균
     * 의미: window_minutes 기간 내 predicate 센서 이벤트의 aggregation_field 평균
     */
    AVG_WITHIN("시간 내 평균", "predicate_sensor_id"),
    
    /**
     * 시간 내 최댓값
     * 의미: window_minutes 기간 내 predicate 센서 이벤트의 aggregation_field 최댓값
     */
    MAX_WITHIN("시간 내 최댓값", "predicate_sensor_id"),
    
    /**
     * 시간 내 최솟값
     * 의미: window_minutes 기간 내 predicate 센서 이벤트의 aggregation_field 최솟값
     */
    MIN_WITHIN("시간 내 최솟값", "predicate_sensor_id"),
    
    /**
     * 시간 내 고유값 횟수
     * 의미: window_minutes 기간 내 predicate 센서 이벤트의 aggregation_field 고유값 개수
     */
    DISTINCT_COUNT_WITHIN("시간 내 고유값 횟수", "predicate_sensor_id"),
    
    /**
     * 순서 내 발생 (A → B 시퀀스)
     * 의미: prev 센서 이벤트 발생 후 window_minutes 기간 내 next 센서 이벤트가 발생했는지 확인
     * 예시: 로그인 실패 후 5분 내 고액 이체 발생
     */
    SEQUENCE_WITHIN("순서 내 발생", "prev_sensor_id", "next_sensor_id"),
    
    /**
     * 기간 내 부재 (ABSENCE 체크)
     * 의미: anchor 센서 발생 시점부터 window_minutes 기간 내 predicate 센서 이벤트가 0건인지 확인
     * ⚠️ 중요: predicate는 "확인 대상", anchor는 "트리거"
     * 예시: 신용등급 하락(anchor) 후 7일 내 매도 거래(predicate) 미실행
     */
    ABSENCE_WITHIN("기간 내 부재", "predicate_sensor_id", "anchor_sensor_id"),
    
    /**
     * 최초 발생
     * 의미: predicate 센서 이벤트의 최초 발생 탐지
     */
    FIRST_OCCURRENCE("최초 발생", "predicate_sensor_id");

    private final String description;
    private final String[] usedColumns;

    AggregateOperator(String description, String... usedColumns) {
        this.description = description;
        this.usedColumns = usedColumns;
    }
}
