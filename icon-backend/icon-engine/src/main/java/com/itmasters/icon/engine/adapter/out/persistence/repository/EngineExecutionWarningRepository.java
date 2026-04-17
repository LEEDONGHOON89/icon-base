package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineExecutionWarningEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EngineExecutionWarningRepository extends JpaRepository<EngineExecutionWarningEntity, Long> {
}

