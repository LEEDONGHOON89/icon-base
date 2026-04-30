package com.itmasters.icon.api.standardfield.application.port.out;

import com.itmasters.icon.api.standardfield.domain.StandardField;
import com.itmasters.icon.common.domain.rule.FieldCategory;
import com.itmasters.icon.common.domain.type.FieldDataType;

import java.util.List;
import java.util.Optional;

/**
 * 표준 필드 Repository 포트
 */
public interface StandardFieldRepository {
    
    /**
     * 표준 필드 저장
     */
    StandardField save(StandardField standardField);
    
    /**
     * 표준 필드 ID로 조회
     */
    Optional<StandardField> findById(String fieldId);
    
    /**
     * 필드명으로 조회
     */
    Optional<StandardField> findByFieldName(String fieldName);
    
    /**
     * 활성화된 표준 필드 목록 조회
     */
    List<StandardField> findByIsActiveTrue();
    
    /**
     * 모든 표준 필드 조회
     */
    List<StandardField> findAll();
    
    /**
     * 표준 필드 삭제
     */
    void deleteById(String fieldId);
    
    /**
     * 표준 필드 존재 여부 확인
     */
    boolean existsById(String fieldId);
    
    /**
     * 필드명 중복 체크
     */
    boolean existsByFieldName(String fieldName);
    
    /**
     * 카테고리별 표준 필드 조회
     */
    List<StandardField> findByCategory(FieldCategory category);
    
    /**
     * 데이터 타입별 표준 필드 조회
     */
    List<StandardField> findByDataType(FieldDataType dataType);
    
    /**
     * 키워드로 표준 필드 검색
     */
    List<StandardField> searchByKeyword(String keyword);
}