package com.itmasters.icon.api.parser.adapter.out.persistence.entity;

import com.itmasters.icon.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

// [2026-04-20] 파서 추출 규칙 JPA 엔티티
@Entity
@Table(name = "parser_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParserRuleEntity extends Auditable {

    @Id
    @Column(name = "parser_rule_id", length = 13)
    private String parserRuleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parser_id", nullable = false)
    private ParserEntity parser;

    @Column(name = "rule_order", nullable = false)
    private int ruleOrder;

    // DELIMITER: {"delimiter":"|","index":0}
    // FIXED_WIDTH: {"startByte":0,"byteLength":6}
    // REGEX: {"pattern":"^(\\w+)","group":1}
    @Column(name = "config_json", columnDefinition = "jsonb", nullable = false)
    private String configJson;

    @Column(name = "target_standard_field_id", length = 100)
    private String targetStandardFieldId;

    @Column(name = "target_field_name", length = 255)
    private String targetFieldName;
}
