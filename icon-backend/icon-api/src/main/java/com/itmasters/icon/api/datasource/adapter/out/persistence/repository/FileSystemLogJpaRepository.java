package com.itmasters.icon.api.datasource.adapter.out.persistence.repository;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.FileSystemLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 파일 시스템 로그 JPA Repository
 *
 * QueryDSL 기반 복잡한 쿼리는 FileSystemLogJpaRepositoryCustom에서 구현
 */
public interface FileSystemLogJpaRepository extends JpaRepository<FileSystemLogEntity, String>, FileSystemLogJpaRepositoryCustom {

    /**
     * 특정 파일 경로가 이미 처리되었는지 확인
     */
    boolean existsByFilePath(String filePath);

    /**
     * 특정 파일 경로가 성공적으로 처리되었는지 확인 - QueryDSL로 구현
     */
    // @Query 제거됨 - FileSystemLogJpaRepositoryCustom 인터페이스에서 선언, FileSystemLogJpaRepositoryImpl에서 구현
    // boolean existsSuccessfulProcessingByFilePath(String filePath);

    /**
     * 특정 설정 ID의 처리 로그 목록 조회 (최신순)
     */
    List<FileSystemLogEntity> findByDsFileSystemConfigIdOrderByProcessedAtDesc(String dsFileSystemConfigId);

    /**
     * 특정 설정 ID의 성공한 처리 로그만 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - FileSystemLogJpaRepositoryCustom 인터페이스에서 선언, FileSystemLogJpaRepositoryImpl에서 구현
    // List<FileSystemLogEntity> findSuccessfulProcessingByConfigId(String configId);

    /**
     * 특정 기간 동안의 처리 로그 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - FileSystemLogJpaRepositoryCustom 인터페이스에서 선언, FileSystemLogJpaRepositoryImpl에서 구현
    // List<FileSystemLogEntity> findByConfigIdAndDateRange(String configId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 마지막 성공한 처리 로그 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - FileSystemLogJpaRepositoryCustom 인터페이스에서 선언, FileSystemLogJpaRepositoryImpl에서 구현
    // Optional<FileSystemLogEntity> findLatestSuccessfulProcessing(String configId);

    /**
     * 실패한 처리 로그 목록 조회 (최신순) - QueryDSL로 구현
     */
    // @Query 제거됨 - FileSystemLogJpaRepositoryCustom 인터페이스에서 선언, FileSystemLogJpaRepositoryImpl에서 구현
    // List<FileSystemLogEntity> findFailedProcessingByConfigId(String configId);
}