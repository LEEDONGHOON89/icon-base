package com.itmasters.icon.api.parser.adapter.out.persistence.repository;

import com.itmasters.icon.api.parser.adapter.out.persistence.entity.ParserEntity;
import com.itmasters.icon.common.domain.type.ParserType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// [2026-04-20] 파서 JPA 리포지토리
public interface ParserJpaRepository extends JpaRepository<ParserEntity, String> {

    List<ParserEntity> findByIsActiveTrueOrderByParserNameAsc();

    List<ParserEntity> findByParserTypeAndIsActiveTrue(ParserType parserType);

    boolean existsByParserName(String parserName);
}
