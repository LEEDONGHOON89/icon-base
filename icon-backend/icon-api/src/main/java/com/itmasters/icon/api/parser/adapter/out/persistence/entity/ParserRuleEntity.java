package com.itmasters.icon.api.parser.adapter.out.persistence.entity;

import com.itmasters.icon.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

// [2026-04-20] 파서 추출 규칙 JPA 엔티티
// [2026-04-20] 재설계: config_json nullable (DELIMITER는 파서레벨 설정 사용)
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

    /** 추출 순서 (DELIMITER: 구분 인덱스, FIXED_WIDTH: 바이트 순서) */
    @Column(name = "rule_order", nullable = false)
    private int ruleOrder;

    /**
     * 규칙별 설정 JSON (타입에 따라 사용)
     * DELIMITER  : null (파서레벨 delimiter 사용, 인덱스는 rule_order)
     * FIXED_WIDTH: {"byteLength":4}
     * REGEX      : {"pattern":"^(\\w+)","group":1}
     */
    @Column(name = "config_json", columnDefinition = "jsonb")
    private String configJson;

    /** 추출 결과를 저장할 표준 필드 ID (선택) */
    @Column(name = "target_standard_field_id", length = 100)
    private String targetStandardFieldId;

    /** 출력 필드명 (예: COLUMN1, COLUMN2) */
    @Column(name = "target_field_name", length = 255)
    private String targetFieldName;
}
