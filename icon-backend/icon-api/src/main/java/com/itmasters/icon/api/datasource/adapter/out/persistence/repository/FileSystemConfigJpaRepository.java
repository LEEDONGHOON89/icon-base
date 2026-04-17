package com.itmasters.icon.api.datasource.adapter.out.persistence.repository;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.FileSystemConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 파일 시스템 설정 JPA Repository
 *
 * QueryDSL 기반 복잡한 쿼리는 FileSystemConfigJpaRepositoryCustom에서 구현
 */
public interface FileSystemConfigJpaRepository extends JpaRepository<FileSystemConfigEntity, String>, FileSystemConfigJpaRepositoryCustom {

    /**
     * 데이터소스 ID로 파일 시스템 설정 조회 (첫 번째만)
     */
    Optional<FileSystemConfigEntity> findByDataSourceId(String dataSourceId);

    /**
     * 데이터소스 ID로 모든 파일 시스템 설정 조회
     */
    List<FileSystemConfigEntity> findAllByDataSourceId(String dataSourceId);

    /**
     * 활성화된 파일 시스템 설정 목록 조회
     */
    List<FileSystemConfigEntity> findByIsActiveTrue();

    /**
     * 특정 데이터소스의 활성화된 설정 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - FileSystemConfigJpaRepositoryCustom 인터페이스에서 선언, FileSystemConfigJpaRepositoryImpl에서 구현
    // Optional<FileSystemConfigEntity> findActiveByDataSourceId(String dataSourceId);

    /**
     * 감시 디렉토리로 설정 조회 (중복 방지용)
     */
    List<FileSystemConfigEntity> findByWatchDirectory(String watchDirectory);
}