package com.itmasters.icon.engine.adapter.out.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 자동 발견된 관계 패턴 엔티티
 *
 * 목적: 데이터 분석을 통해 자동으로 발견된 엔티티 간 관계 패턴 저장
 *
 * 예시:

   * -DEVICE-ACCOUNT 조합이 1,234회 발견 → "DEVICE --USES--> ACCOUNT" 패턴 (신뢰도 95.3%)
   * -CUSTOMER-MERCHANT 조합이 856회 발견 → "CUSTOMER --BUYS_FROM--> MERCHANT" 패턴 (신뢰도 87.1%)

 *
 * 워크플로우:

   * -PREP-5에서 패턴 자동 발견 → PENDING 상태로 저장
   * -사용자가 검토 → APPROVED 또는 REJECTED
   * -APPROVED → entity_relation_rules로 승격

 *
 * @since 2025-02-10
 */
@Entity
@Table(
    name = "pattern_relations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_pattern",
            columnNames = {"data_source_id", "from_entity_type", "from_id_field", "relation_type", "to_entity_type", "to_id_field"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatternRelationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pattern_id")
    private Long patternId;

    @Column(name = "data_source_id", nullable = false, length = 50)
    private String dataSourceId;

    // 발견된 패턴 정보
    @Column(name = "from_entity_type", nullable = false, length = 50)
    private String fromEntityType;

    @Column(name = "from_id_field", nullable = false, length = 100)
    private String fromIdField;

    @Column(name = "relation_type", nullable = false, length = 50)
    private String relationType;

    @Column(name = "to_entity_type", nullable = false, length = 50)
    private String toEntityType;

    @Column(name = "to_id_field", nullable = false, length = 100)
    private String toIdField;

    // 신뢰도 정보
    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "occurrence_count", nullable = false)
    private Integer occurrenceCount = 0;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    // 패턴 근거
    @Column(name = "detection_method", length = 50)
    private String detectionMethod;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pattern_evidence", columnDefinition = "json")
    private JsonNode patternEvidence;

    // 검증 상태
    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus = "PENDING";

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    // 메타데이터
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 새 패턴 발견 생성 (정적 팩토리 메서드)
     */
    public static PatternRelationEntity of(
            String dataSourceId,
            String fromEntityType,
            String fromIdField,
            String relationType,
            String toEntityType,
            String toIdField,
            BigDecimal confidenceScore,
            Integer occurrenceCount,
            String detectionMethod) {

        PatternRelationEntity entity = new PatternRelationEntity();
        entity.dataSourceId = dataSourceId;
        entity.fromEntityType = fromEntityType;
        entity.fromIdField = fromIdField;
        entity.relationType = relationType;
        entity.toEntityType = toEntityType;
        entity.toIdField = toIdField;
        entity.confidenceScore = confidenceScore;
        entity.occurrenceCount = occurrenceCount;
        entity.detectionMethod = detectionMethod;
        entity.approvalStatus = "PENDING";

        LocalDateTime now = LocalDateTime.now();
        entity.firstSeenAt = now;
        entity.lastSeenAt = now;
        entity.createdAt = now;
        entity.updatedAt = now;

        return entity;
    }

    /**
     * 패턴 발견 빈도 업데이트
     */
    public void incrementOccurrence() {
        this.occurrenceCount++;
        this.lastSeenAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 신뢰도 업데이트
     */
    public void updateConfidence(BigDecimal newConfidence) {
        this.confidenceScore = newConfidence;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 패턴 승인
     */
    public void approve(String reviewer) {
        this.approvalStatus = "APPROVED";
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 패턴 거부
     */
    public void reject(String reviewer, String reason) {
        this.approvalStatus = "REJECTED";
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.rejectReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 패턴 근거 데이터 설정
     */
    public void setPatternEvidence(JsonNode evidence) {
        this.patternEvidence = evidence;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.approvalStatus == null) {
            this.approvalStatus = "PENDING";
        }
        if (this.occurrenceCount == null) {
            this.occurrenceCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
