package com.itmasters.icon.api.analytics.adapter.out.persistence.repository;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiExecDsMpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecDsMpJpaRepository extends JpaRepository<ApiExecDsMpEntity, Long> {
}
