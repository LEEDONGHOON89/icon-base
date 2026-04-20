package com.itmasters.icon.engine.adapter.out.persistence.entity;

import com.itmasters.icon.common.domain.type.ParserType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

// [2026-04-20] 룰 엔진용 파서 엔티티 (parsers 테이블 읽기 전용)
// [2026-04-20] 재설계: sourceField, configJson(파서레벨) 추가
@Entity
@Table(name = "parsers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EngineParserEntity {

    @Id
    @Column(name = "parser_id", length = 13)
    private String parserId;

    @Column(name = "parser_name", nullable = false, length = 100)
    private String parserName;

    @Enumerated(EnumType.STRING)
    @Column(name = "parser_type", nullable = false, length = 50)
    private ParserType parserType;

    /** 파싱 대상 원본 필드명 (예: "line") */
    @Column(name = "source_field", length = 100)
    private String sourceField;

    /**
     * 파서 공통 설정 JSON
     * DELIMITER  : {"delimiter":"|"}
     * FIXED_WIDTH: null
     * REGEX      : null
     */
    @Column(name = "config_json", columnDefinition = "jsonb")
    private String configJson;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @OneToMany(mappedBy = "parser", fetch = FetchType.EAGER)
    @OrderBy("ruleOrder ASC")
    @Builder.Default
    private List<EngineParserRuleEntity> rules = new ArrayList<>();
}
