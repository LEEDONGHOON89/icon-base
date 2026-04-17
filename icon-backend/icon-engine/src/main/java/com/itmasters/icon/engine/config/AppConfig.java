package com.itmasters.icon.engine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.common.domain.rule.ParseCondition;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ModelMapper 설정
 */
@Configuration
@RequiredArgsConstructor
public class AppConfig {
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;


    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }


    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // 매핑 전략 설정
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)  // 엄격한 매칭
                .setFieldMatchingEnabled(true)
                .setSkipNullEnabled(true)  // null 값 건너뛰기
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);  // private 필드 접근

        return modelMapper;
    }


    @Bean
    public ParseCondition parseCondition() {
        return new ParseCondition(objectMapper);
    }
}