package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

// [2026-04-20] 룰 엔진용 파서 규칙 엔티티 (parser_rules 테이블 읽기 전용)
@Entity
@Table(name = "parser_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EngineParserRuleEntity {

    @Id
    @Column(name = "parser_rule_id", length = 13)
    private String parserRuleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parser_id", nullable = false)
    private EngineParserEntity parser;

    @Column(name = "rule_order", nullable = false)
    private int ruleOrder;

    @Column(name = "config_json", columnDefinition = "jsonb", nullable = false)
    private String configJson;

    @Column(name = "target_standard_field_id", length = 100)
    private String targetStandardFieldId;

    @Column(name = "target_field_name", length = 255)
    private String targetFieldName;
}
