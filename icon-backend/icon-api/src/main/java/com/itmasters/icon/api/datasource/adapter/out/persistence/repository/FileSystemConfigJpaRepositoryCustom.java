package com.itmasters.icon.api.datasource.adapter.out.persistence.repository;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.FileSystemConfigEntity;

import java.util.Optional;

/**
 * 파일 시스템 설정 커스텀 Repository 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface FileSystemConfigJpaRepositoryCustom {

    /**
     * 특정 데이터소스의 활성화된 설정 조회
     *
     * @param dataSourceId 데이터소스 ID
     * @return 활성화된 파일 시스템 설정 (있으면 Optional.of, 없으면 Optional.empty)
     */
    Optional<FileSystemConfigEntity> findActiveByDataSourceId(String dataSourceId);
}
