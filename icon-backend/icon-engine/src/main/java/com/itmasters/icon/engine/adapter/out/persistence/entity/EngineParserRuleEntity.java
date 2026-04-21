package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

// [2026-04-20] 룰 엔진용 파서 규칙 엔티티 (parser_rules 테이블 읽기 전용)
// [2026-04-20] 재설계: config_json nullable (DELIMITER는 파서레벨 config 사용)
// [2026-04-21] target_standard_field_id 제거
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

    /** 추출 순서 (DELIMITER: split 인덱스, FIXED_WIDTH: 바이트 순서) */
    @Column(name = "rule_order", nullable = false)
    private int ruleOrder;

    /**
     * 규칙별 설정 JSON (nullable)
     * DELIMITER  : null (파서레벨 delimiter 사용)
     * FIXED_WIDTH: {"byteLength":4}
     * REGEX      : {"pattern":"^(\\w+)","group":1}
     */
    @Column(name = "config_json", columnDefinition = "jsonb")
    private String configJson;

    /** 출력 필드명 (예: COLUMN1) */
    @Column(name = "target_field_name", length = 255)
    private String targetFieldName;
}
