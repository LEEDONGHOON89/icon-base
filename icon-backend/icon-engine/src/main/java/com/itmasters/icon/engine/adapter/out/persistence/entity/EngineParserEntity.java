package com.itmasters.icon.engine.adapter.out.persistence.entity;

import com.itmasters.icon.common.domain.type.ParserType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

// [2026-04-20] 룰 엔진용 파서 엔티티 (parsers 테이블 읽기 전용)
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

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @OneToMany(mappedBy = "parser", fetch = FetchType.EAGER)
    @OrderBy("ruleOrder ASC")
    @Builder.Default
    private List<EngineParserRuleEntity> rules = new ArrayList<>();
}
