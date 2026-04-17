package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.ExecDsMpEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 데이터소스 필드매핑 실행 로그 Repository 인터페이스
 */
public interface ExecDsMpRepository {
    
    /**
     * 실행 로그 저장
     */
    ExecDsMpEntity save(ExecDsMpEntity entity);
    
    /**
     * 실행 ID로 조회
     */
    Optional<ExecDsMpEntity> findById(Long execDsMpId);
    
    /**
     * 데이터소스별 최근 실행 로그 조회
     */
    List<ExecDsMpEntity> findRecentByDataSourceId(String dataSourceId, int limit);
    
    /**
     * 기간별 실행 로그 조회
     */
    List<ExecDsMpEntity> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 상태별 실행 로그 조회
     */
    List<ExecDsMpEntity> findByStatus(String status);
    
    /**
     * 실행 중인 로그 조회
     */
    List<ExecDsMpEntity> findRunningExecutions();
    
    /**
     * 최근 성공한 실행 조회
     */
    Optional<ExecDsMpEntity> findLastSuccessfulExecution(String dataSourceId);
}