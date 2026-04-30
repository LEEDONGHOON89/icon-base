package com.itmasters.icon.api.parser.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// [2026-04-20] 데이터소스-파서 연결 엔티티 (data_source_parsers 테이블)
@Entity
@Table(name = "data_source_parsers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class DataSourceParserEntity {

    @Id
    @Column(name = "data_source_parser_id", length = 13)
    private String dataSourceParserId;

    /** 데이터소스 ID */
    @Column(name = "data_source_id", nullable = false, length = 100)
    private String dataSourceId;

    /** 연결된 파서 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parser_id", nullable = false)
    private ParserEntity parser;

    /** 파서 적용 순서 (낮을수록 먼저 실행) */
    @Column(name = "parser_order", nullable = false)
    @Builder.Default
    private int parserOrder = 0;

    /** 활성 여부 */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;
}
