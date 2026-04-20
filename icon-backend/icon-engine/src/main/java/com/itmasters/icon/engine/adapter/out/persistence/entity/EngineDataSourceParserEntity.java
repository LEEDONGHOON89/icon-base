package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

// [2026-04-20] 룰 엔진용 데이터소스-파서 연결 엔티티 (data_source_parsers 테이블 읽기 전용)
@Entity
@Table(name = "data_source_parsers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EngineDataSourceParserEntity {

    @Id
    @Column(name = "data_source_parser_id", length = 13)
    private String dataSourceParserId;

    @Column(name = "data_source_id", nullable = false, length = 100)
    private String dataSourceId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parser_id", nullable = false)
    private EngineParserEntity parser;

    @Column(name = "parser_order", nullable = false)
    private int parserOrder;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
