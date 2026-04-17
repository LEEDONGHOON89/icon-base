package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;
import com.itmasters.icon.engine.repository.RuleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * RuleRepository 인터페이스의 Spring Data JPA 구현체
 */
@Repository
public class RuleRepositoryImpl implements RuleRepository {

    private final JpaRuleRepository jpaRuleRepository;

    public RuleRepositoryImpl(JpaRuleRepository jpaRuleRepository) {
        this.jpaRuleRepository = jpaRuleRepository;
    }

    @Override
    public List<RuleEntity> findAllActive() {
        return jpaRuleRepository.findByIsActiveTrue();
    }

    @Override
    public List<RuleEntity> findActiveByRuleIds(java.util.Collection<String> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            return java.util.List.of();
        }
        return jpaRuleRepository.findActiveByRuleIds(ruleIds);
    }
}
