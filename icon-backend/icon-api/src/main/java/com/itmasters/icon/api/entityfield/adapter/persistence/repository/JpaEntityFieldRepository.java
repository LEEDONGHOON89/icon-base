package com.itmasters.icon.api.entityfield.adapter.persistence.repository;

import com.itmasters.icon.api.entityfield.adapter.persistence.entity.EntityFieldEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * EntityField JPA Repository
 */
public interface JpaEntityFieldRepository extends JpaRepository<EntityFieldEntity, String> {
    List<EntityFieldEntity> findByIsActiveTrue();
}
