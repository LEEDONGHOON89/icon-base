package com.itmasters.icon.api.analytics.adapter.out.persistence.repository;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiMappedDataStorageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MappedDataStorageJpaRepository extends JpaRepository<ApiMappedDataStorageEntity, Long> {

    Optional<ApiMappedDataStorageEntity> findByLandingRecordId(Long landingRecordId);

    List<ApiMappedDataStorageEntity> findByLandingRecordIdIn(Collection<Long> landingRecordIds);
}

