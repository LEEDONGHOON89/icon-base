package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.LandingRawRecordEntity;

import java.util.List;
import java.util.Optional;

public interface LandingRawRecordRepository {

    LandingRawRecordEntity save(LandingRawRecordEntity entity);

    List<LandingRawRecordEntity> saveAll(List<LandingRawRecordEntity> entities);

    Optional<LandingRawRecordEntity> findById(Long landingRecordId);

    List<LandingRawRecordEntity> findByExecDsMpIdOrderByRowIndex(Long execDsMpId);

    List<LandingRawRecordEntity> findByExecDsMpIdAndIngestionStatus(Long execDsMpId,
                                                                    LandingRawRecordEntity.IngestionStatus status);
}
