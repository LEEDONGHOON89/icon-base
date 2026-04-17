package com.itmasters.icon.api.datasource.adapter.out.persistence.repository;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.FileSystemLogEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 파일 시스템 로그 커스텀 Repository 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface FileSystemLogJpaRepositoryCustom {

    /**
     * 특정 파일 경로가 성공적으로 처리되었는지 확인
     *
     * @param filePath 파일 경로
     * @return 성공적인 처리 존재 여부
     */
    boolean existsSuccessfulProcessingByFilePath(String filePath);

    /**
     * 특정 설정 ID의 성공한 처리 로그만 조회
     *
     * @param configId 설정 ID
     * @return 성공한 처리 로그 목록 (최신순)
     */
    List<FileSystemLogEntity> findSuccessfulProcessingByConfigId(String configId);

    /**
     * 특정 기간 동안의 처리 로그 조회
     *
     * @param configId 설정 ID
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @return 기간 내 처리 로그 목록 (최신순)
     */
    List<FileSystemLogEntity> findByConfigIdAndDateRange(String configId,
                                                         LocalDateTime startDate,
                                                         LocalDateTime endDate);

    /**
     * 마지막 성공한 처리 로그 조회
     *
     * @param configId 설정 ID
     * @return 마지막 성공 로그 (있으면 Optional.of, 없으면 Optional.empty)
     */
    Optional<FileSystemLogEntity> findLatestSuccessfulProcessing(String configId);

    /**
     * 실패한 처리 로그 목록 조회 (최신순)
     *
     * @param configId 설정 ID
     * @return 실패한 처리 로그 목록
     */
    List<FileSystemLogEntity> findFailedProcessingByConfigId(String configId);
}
