package com.itmasters.icon.common.domain;

/**
 * ID 생성 인터페이스
 * 각 시스템에서 필요한 엔티티 타입에 대해서만 구현
 * 
 * 구현체는 각 시스템에서 제공:
 * - 로컬 환경: TSID 기반 구현
 * - MSA 환경: ID 채번 서비스 호출 구현
 * - 테스트 환경: Mock 구현
 * 
 * 예시:
 * - icon-api: 모든 엔티티 타입 지원
 * - icon-collector: DATA_SOURCE, RULE만 지원
 * - icon-analyzer: RULE, SCENARIO만 지원
 */
public interface IdGenerator {
    /**
     * 엔티티 타입에 따른 ID 생성
     * @param entityType 엔티티 타입
     * @return 생성된 ID
     * @throws UnsupportedOperationException 지원하지 않는 엔티티 타입인 경우
     */
    String generateId(EntityType entityType);
    
    /**
     * 특정 엔티티 타입을 지원하는지 확인
     * @param entityType 엔티티 타입
     * @return 지원 여부
     */
    default boolean supports(EntityType entityType) {
        try {
            generateId(entityType);
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }
}