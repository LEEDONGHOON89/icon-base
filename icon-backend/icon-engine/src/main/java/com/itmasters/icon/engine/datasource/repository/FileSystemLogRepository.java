package com.itmasters.icon.engine.datasource.repository;


import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDsFileSystemLogEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 파일 시스템 로그 Repository 포트 (엔티티 직접 반환)
 */
public interface FileSystemLogRepository {

    EngineDsFileSystemLogEntity save(EngineDsFileSystemLogEntity log);

    Optional<EngineDsFileSystemLogEntity> findById(String id);

    boolean isAlreadyProcessed(String filePath);

    boolean isSuccessfullyProcessed(String filePath);

    List<EngineDsFileSystemLogEntity> findByConfigId(String configId);

    List<EngineDsFileSystemLogEntity> findSuccessfulProcessingByConfigId(String configId);

    List<EngineDsFileSystemLogEntity> findByConfigIdAndDateRange(String configId, LocalDateTime startDate, LocalDateTime endDate);

    Optional<EngineDsFileSystemLogEntity> findLatestSuccessfulProcessing(String configId);

    List<EngineDsFileSystemLogEntity> findFailedProcessingByConfigId(String configId);
}
