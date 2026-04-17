package com.itmasters.icon.common.domain;

import java.util.List;
import java.util.Map;

/**
 * 룰 도메인과 연산자 카테고리 간의 매핑 정의
 * 1단계 도메인에서 사용 가능한 2단계 카테고리들을 정의
 */
public class RuleDomainCategoryMapping {
    
    private static final Map<RuleDomain, List<OperatorCategory>> DOMAIN_CATEGORY_MAP = Map.of(
        // 로그인/인증: 횟수, 상태, 빈도 체크 가능
        RuleDomain.LOGIN, List.of(
            OperatorCategory.NUMERIC_COMPARISON,  // login_failure_count >= 5
            OperatorCategory.TEXT_COMPARISON,     // login_status = "FAILED"
            OperatorCategory.FREQUENCY_CHECK      // 1시간 내 로그인 실패 3회
        ),
        
        // ATM: 금액, 횟수, 빈도 체크 가능
        RuleDomain.ATM, List.of(
            OperatorCategory.NUMERIC_COMPARISON,  // atm_amount >= 1000000, atm_count >= 5
            OperatorCategory.FREQUENCY_CHECK      // 1시간 내 ATM 이용 5회
        ),
        
        // 금융거래: 금액, 시간대, 빈도 체크 가능
        RuleDomain.FINANCIAL_TRANSACTION, List.of(
            OperatorCategory.NUMERIC_COMPARISON,  // transaction_amount >= 10000000
            OperatorCategory.TIME_RANGE,          // 야간시간대 거래 (22:00-06:00)
            OperatorCategory.FREQUENCY_CHECK,     // 1시간 내 고액거래 3건
            OperatorCategory.BOOLEAN_CHECK        // is_night_time = true
        ),
        

        // 디바이스보안: 변경 상태, 빈도
        RuleDomain.DEVICE_SECURITY, List.of(
            OperatorCategory.BOOLEAN_CHECK,       // device_changed = true
            OperatorCategory.FREQUENCY_CHECK      // 1일 내 디바이스 변경 2회
        )
    );
    
    /**
     * 특정 도메인에서 지원하는 연산자 카테고리 목록 조회
     */
    public static List<OperatorCategory> getSupportedCategories(RuleDomain domain) {
        return DOMAIN_CATEGORY_MAP.getOrDefault(domain, List.of());
    }
    
    /**
     * 특정 도메인이 특정 카테고리를 지원하는지 확인
     */
    public static boolean supports(RuleDomain domain, OperatorCategory category) {
        return getSupportedCategories(domain).contains(category);
    }
    
    /**
     * 모든 도메인-카테고리 매핑 조회 (메타데이터 API용)
     */
    public static Map<RuleDomain, List<OperatorCategory>> getAllMappings() {
        return DOMAIN_CATEGORY_MAP;
    }
}