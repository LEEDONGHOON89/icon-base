package com.itmasters.icon.api.derivedfield.application.config;

import com.itmasters.icon.api.derivedfield.application.strategy.EntityRelationLookupStrategy;
import com.itmasters.icon.api.derivedfield.application.strategy.FieldComparisonStrategy;
import com.itmasters.icon.api.derivedfield.application.strategy.FieldComputationStrategy;
import com.itmasters.icon.api.derivedfield.application.strategy.context.RepositoryHolder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 파생 필드 설정
 *
 * Strategy 패턴 팩토리 및 캐싱 설정
 */
@Configuration
@EnableCaching
public class DerivedFieldConfig {

    /**
     * Strategy Factory
     *
     * computation_type -> Strategy 매핑
     */
    @Bean
    public Map<String, FieldComputationStrategy> fieldComputationStrategies(
            List<FieldComputationStrategy> strategies) {

        Map<String, FieldComputationStrategy> strategyMap = new HashMap<>();

        for (FieldComputationStrategy strategy : strategies) {
            strategyMap.put(strategy.getSupportedComputationType(), strategy);
        }

        return strategyMap;
    }

    /**
     * Repository Holder
     *
     * Strategy에서 필요한 Repository 접근 제공
     */
    @Bean
    public RepositoryHolder repositoryHolder() {
        // TODO: 실제 Repository 주입
        // return new RepositoryHolder(
        //     entityRelationRepository,
        //     entityAttributeRepository,
        //     eventStreamRepository
        // );
        return new RepositoryHolder();
    }
}
