package com.itmasters.icon.api.derivedfield.application.strategy.context;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Repository Holder
 *
 * Strategy에서 필요한 Repository 접근을 제공합니다.
 * (향후 EntityRelationRepository, EventStreamRepository 등 추가 예정)
 */
@Getter
@NoArgsConstructor
public class RepositoryHolder {

    // 향후 추가:
    // private final EntityRelationRepository entityRelationRepository;
    // private final EntityAttributeRepository entityAttributeRepository;
    // private final EventStreamRepository eventStreamRepository;

    /**
     * 현재는 placeholder
     * 실제 구현 시 필요한 Repository를 여기에 추가
     */
}
