package com.itmasters.icon.api.parser.adapter.out.persistence.entity;

import com.itmasters.icon.common.domain.type.ParserType;
import com.itmasters.icon.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

// [2026-04-20] 파서 정의 JPA 엔티티
// [2026-04-21] config_json @JdbcTypeCode(SqlTypes.JSON) 추가 — JSONB 타입 바인딩 오류 수정
// [2026-04-20] 재설계: source_field(파싱 대상 필드명), config_json(파서레벨 공통설정) 추가
@Entity
@Table(name = "parsers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParserEntity extends Auditable {

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
     * FIXED_WIDTH: null (규칙별 byteLength 사용)
     * REGEX      : null (규칙별 pattern/group 사용)
     */
    // [2026-04-21] JSONB 타입 바인딩을 위해 @JdbcTypeCode(SqlTypes.JSON) 추가
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", columnDefinition = "jsonb")
    private String configJson;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "parser", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("ruleOrder ASC")
    @Builder.Default
    private List<ParserRuleEntity> rules = new ArrayList<>();
}
