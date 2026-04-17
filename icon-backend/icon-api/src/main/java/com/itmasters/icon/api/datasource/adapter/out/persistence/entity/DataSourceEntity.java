package com.itmasters.icon.api.datasource.adapter.out.persistence.entity;

import com.itmasters.icon.entity.Auditable;
import com.itmasters.icon.api.datasource.dto.DataSourceDto;
import com.itmasters.icon.common.domain.type.DataSourceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 데이터 소스 엔티티
 * 룰이 실행될 데이터 소스를 정의합니다.
 */
@Entity
@Table(name = "data_sources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DataSourceEntity extends Auditable {

    @Id
    @Column(name = "data_source_id")
    private String dataSourceId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private DataSourceType sourceType;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "transaction_id_field", length = 100)
    private String transactionIdField;

    @Builder
    private DataSourceEntity(String dataSourceId, String name, String description, DataSourceType sourceType, boolean isActive, String transactionIdField) {
        this.dataSourceId = dataSourceId;
        this.name = name;
        this.description = description;
        this.sourceType = sourceType;
        this.isActive = isActive;
        this.transactionIdField = transactionIdField;
    }
    
    // ========== 정적 팩토리 메서드 ==========
    
    /**
     * 새로운 데이터 소스 생성
     */
    public static DataSourceEntity create(String name, String description, DataSourceType sourceType) {
        validateName(name);
        validateSourceType(sourceType);
        
        return DataSourceEntity.builder()
                .name(name)
                .description(description)
                .sourceType(sourceType)
                .isActive(true)
                .build();
    }
    
    /**
     * CreateCommand로부터 생성
     */
    public static DataSourceEntity from(DataSourceDto.CreateCommand command) {
        DataSourceEntity entity = create(command.getName(), command.getDescription(), command.getSourceType());
        entity.transactionIdField = command.getTransactionIdField();
        return entity;
    }
    
    // ========== ID 관리 ==========
    
    /**
     * ID 할당 (Repository에서 사용)
     */
    public void assignId(String id) {
        if (this.dataSourceId != null) {
            throw new IllegalStateException("ID가 이미 할당되었습니다");
        }
        this.dataSourceId = id;
    }
    
    // ========== 비즈니스 메서드 ==========
    
    /**
     * 데이터 소스 정보 업데이트
     */
    public void update(DataSourceDto.UpdateCommand command) {
        if (command.getName() != null && !command.getName().equals(this.name)) {
            changeName(command.getName());
        }
        
        if (command.getDescription() != null) {
            changeDescription(command.getDescription());
        }
        
        if (command.getTransactionIdField() != null) {
            this.transactionIdField = command.getTransactionIdField();
        }
        
        if (command.getIsActive() != null) {
            if (command.getIsActive()) {
                activate();
            } else {
                deactivate();
            }
        }
    }
    
    /**
     * 이름 변경
     */
    public void changeName(String name) {
        validateName(name);
        this.name = name;
    }
    
    /**
     * 설명 변경
     */
    public void changeDescription(String description) {
        this.description = description;
    }
    
    /**
     * 활성화
     */
    public void activate() {
        this.isActive = true;
    }
    
    /**
     * 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }
    
    // ========== DTO 변환 메서드 ==========
    
    /**
     * Entity → Info DTO 변환
     */
    public DataSourceDto.Info toInfo() {
        return DataSourceDto.Info.builder()
                .dataSourceId(this.dataSourceId)
                .name(this.name)
                .description(this.description)
                .sourceType(this.sourceType)
                .isActive(this.isActive)
                .transactionIdField(this.transactionIdField)
                .ruleCount(0L) // TODO: Rule mapping count 구현 필요
                .build();
    }
    
    /**
     * Entity → Info DTO 변환 (규칙 수 포함)
     */
    public DataSourceDto.Info toInfoWithRuleCount(Long ruleCount) {
        return DataSourceDto.Info.builder()
                .dataSourceId(this.dataSourceId)
                .name(this.name)
                .description(this.description)
                .sourceType(this.sourceType)
                .isActive(this.isActive)
                .transactionIdField(this.transactionIdField)
                .ruleCount(ruleCount)
                .build();
    }
    
    // ========== 유효성 검증 메서드 ==========
    
    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("데이터 소스 이름은 필수입니다.");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("데이터 소스 이름은 100자를 초과할 수 없습니다.");
        }
    }
    
    private static void validateSourceType(DataSourceType sourceType) {
        if (sourceType == null) {
            throw new IllegalArgumentException("데이터 소스 타입은 필수입니다.");
        }
    }
}