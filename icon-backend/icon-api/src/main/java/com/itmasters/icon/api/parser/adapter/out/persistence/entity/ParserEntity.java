package com.itmasters.icon.api.parser.adapter.out.persistence.entity;

import com.itmasters.icon.common.domain.type.ParserType;
import com.itmasters.icon.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

// [2026-04-20] 파서 정의 JPA 엔티티
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
