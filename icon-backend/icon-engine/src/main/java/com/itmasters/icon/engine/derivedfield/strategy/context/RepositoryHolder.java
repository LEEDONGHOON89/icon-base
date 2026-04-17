package com.itmasters.icon.engine.derivedfield.strategy.context;

import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityAttributeRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityRelationRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Repository Holder
 *
 * Strategy에서 필요한 Repository 접근을 제공합니다.
 */
@Getter
@AllArgsConstructor
public class RepositoryHolder {

    /**
     * Entity 속성 조회용 Repository
     */
    private final EntityAttributeRepository entityAttributeRepository;

    /**
     * Entity 관계 조회용 Repository
     */
    private final EntityRelationRepository entityRelationRepository;
}
