package com.itmasters.icon.api.metadata.application.service;

import com.itmasters.icon.api.metadata.adapter.in.web.dto.DataSourceTypeMetadataResponse;
import com.itmasters.icon.api.metadata.adapter.in.web.dto.EntityTypeMetadataResponse;
import com.itmasters.icon.api.metadata.adapter.in.web.dto.FieldMetadataResponse;
import com.itmasters.icon.api.metadata.adapter.in.web.dto.OperatorMetadataResponse;
import com.itmasters.icon.api.metadata.adapter.in.web.dto.ParameterDefinitionResponse;
import com.itmasters.icon.api.metadata.adapter.in.web.dto.RiskLevelMetadataResponse;
import com.itmasters.icon.api.metadata.adapter.in.web.dto.RuleDomainMetadataResponse;
import com.itmasters.icon.api.metadata.domain.RuleMetadataConstants;
import com.itmasters.icon.common.domain.RiskLevel;
import com.itmasters.icon.api.standardfield.adapter.out.persistence.entity.StandardFieldEntity;
import com.itmasters.icon.api.standardfield.adapter.out.persistence.repository.StandardFieldJpaRepository;
import com.itmasters.icon.common.domain.RuleDomain;
import com.itmasters.icon.common.domain.rule.ParameterDefinition;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.common.domain.type.DataSourceType;
import com.itmasters.icon.common.domain.type.FieldDataType;
import com.itmasters.icon.common.domain.type.FieldType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetadataService {
    private final StandardFieldJpaRepository standardFieldJpaRepository;
    
    /**
     * 사용 가능한 필드 목록을 DB(standard_fields)에서 조회합니다.
     * allowed_operators(text[])가 설정된 경우 그 배열을 사용하고,
     * 없으면 데이터 타입을 기반으로 기본 연산자 집합을 제공합니다.
     */
    public List<FieldMetadataResponse> getAvailableFields() {
        List<StandardFieldEntity> entities = standardFieldJpaRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsActive()))
                .collect(Collectors.toList());

        return entities.stream()
                .map(this::toFieldMetadata)
                .collect(Collectors.toList());
    }

    private FieldMetadataResponse toFieldMetadata(StandardFieldEntity e) {
        List<OperatorMetadataResponse> availableOperators = resolveOperators(e).stream()
                .map(operator -> OperatorMetadataResponse.builder()
                        .value(operator.name())
                        .symbol(operator.getSymbol())
                        .label(operator.getLabel())
                        .category(operator.getCategory())
                        .description(operator.getDescription())
                        .supportedTypes(operator.getSupportedTypeStrings())
                        .requiresParameters(operator.requiresParameters())
                        .parameterFormat(operator.getParameterFormat())
                        .isAggregateOperator(operator.isAggregateOperator())
                        .parameterDefinitions(convertParameterDefinitions(operator))
                        .build())
                .collect(Collectors.toList());

        String categoryLabel = e.getCategory() == null ? null : e.getCategory().getLabel();
        return FieldMetadataResponse.builder()
                .name(e.getStandardFieldId())
                .label(e.getDisplayName() != null ? e.getDisplayName() : e.getStandardFieldId())
                .category(categoryLabel)
                .description(e.getDescription())
                .valueOptions(Collections.emptyList())
                .availableOperators(availableOperators)
                .build();
    }

    private List<RuleOperator> resolveOperators(StandardFieldEntity e) {
        String[] allowed = e.getAllowedOperators();
        if (allowed != null && allowed.length > 0) {
            return Arrays.stream(allowed)
                    .map(val -> {
                        try {
                            return RuleOperator.valueOf(val);
                        } catch (Exception ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        FieldType ft = mapToRuleFieldType(e.getDataType());
        return Arrays.stream(RuleOperator.values())
                .filter(op -> op.getSupportedTypes().contains(ft) || op.getSupportedTypes().contains(FieldType.ANY))
                .collect(Collectors.toList());
    }

    private FieldType mapToRuleFieldType(FieldDataType dt) {
        if (dt == null) return FieldType.STRING;
        switch (dt) {
            case NUMBER:
            case DECIMAL:
                return FieldType.NUMBER;
            case BOOLEAN:
                return FieldType.BOOLEAN;
            case TIME:
            case DATE:
            case DATETIME:
                return FieldType.TIME;
            case STRING:
            case JSON:
            case ARRAY:
            case OBJECT:
            default:
                return FieldType.STRING;
        }
    }
    
    /**
     * 사용 가능한 연산자 목록 조회
     */
    public List<OperatorMetadataResponse> getAvailableOperators() {
        return RuleMetadataConstants.getAvailableOperators();
    }
    
    
    /**
     * ParameterDefinition을 ParameterDefinitionResponse로 변환
     */
    private List<ParameterDefinitionResponse> convertParameterDefinitions(RuleOperator operator) {
        if (!operator.requiresParameters()) {
            return Collections.emptyList();
        }
        
        return operator.getParameterDefinitions().stream()
            .map(def -> ParameterDefinitionResponse.builder()
                .name(def.getName())
                .type(def.getType().getCode())
                .required(def.isRequired())
                .description(def.getDescription())
                .placeholder(generatePlaceholder(def))
                .unit(extractUnit(def))
                .validation(generateValidationRule(def))
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * 파라미터에 대한 placeholder 생성
     */
    private String generatePlaceholder(ParameterDefinition def) {
        switch (def.getType()) {
            case NUMBER:
                if (def.getName().contains("시간윈도우")) {
                    return "예: 30";
                } else if (def.getName().contains("임계값") || def.getName().contains("횟수")) {
                    return "예: 5";
                } else if (def.getName().contains("금액")) {
                    return "예: 1000000";
                }
                return "숫자 입력";
            case TIME:
                return "예: 09:00 또는 30m";
            case STRING:
                return "문자열 입력";
            case BOOLEAN:
                return "true 또는 false";
            default:
                return "";
        }
    }
    
    /**
     * 파라미터에서 단위 추출
     */
    private String extractUnit(ParameterDefinition def) {
        if (def.getName().contains("시간윈도우")) {
            return "분";
        } else if (def.getName().contains("금액")) {
            return "원";
        } else if (def.getName().contains("횟수") || def.getName().contains("개수")) {
            return "개";
        }
        return null;
    }
    
    /**
     * 파라미터에 대한 유효성 검사 규칙 생성
     */
    private ParameterDefinitionResponse.ValidationRule generateValidationRule(ParameterDefinition def) {
        ParameterDefinitionResponse.ValidationRule.ValidationRuleBuilder builder = 
            ParameterDefinitionResponse.ValidationRule.builder();
        
        switch (def.getType()) {
            case NUMBER:
                if (def.getName().contains("시간윈도우")) {
                    builder.min(1.0).max(1440.0); // 1분 ~ 24시간
                } else if (def.getName().contains("횟수") || def.getName().contains("개수")) {
                    builder.min(1.0).max(10000.0);
                } else if (def.getName().contains("금액")) {
                    builder.min(0.0).max(999999999999.0);
                } else {
                    builder.min(0.0); // 기본적으로 0 이상
                }
                break;
            case TIME:
                builder.format("HH:mm");
                break;
            case STRING:
                builder.minLength(1).maxLength(255);
                break;
        }
        
        return builder.build();
    }
    
    /**
     * 데이터 소스 타입 메타데이터 조회
     */
    public List<DataSourceTypeMetadataResponse> getDataSourceTypes() {
        return Arrays.stream(DataSourceType.values())
            .map(type -> DataSourceTypeMetadataResponse.builder()
                .value(type.getValue())
                .label(type.getLabel())
                .description(type.getDescription())
                .iconType(type.getIconType())
                .isDatabaseType(type.isDatabaseType())
                .isLogType(type.isLogType())
                .isFileBasedType(type.isFileBasedType())
                .isStreamingType(type.isStreamingType())
                .build())
            .collect(Collectors.toList());
    }
    
    // 더 이상 RuleField(enum) 기반 목록은 노출하지 않습니다.

    /**
     * 룰 도메인 메타데이터 조회
     */
    public List<RuleDomainMetadataResponse> getRuleDomains() {
        return Arrays.stream(RuleDomain.values())
                .map(d -> RuleDomainMetadataResponse.builder()
                        .value(d.name())
                        .label(d.getLabel())
                        .description(d.getDescription())
                        .fieldDatetime(d.getFieldDatetime())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 위험 레벨 메타데이터 조회
     */
    public List<RiskLevelMetadataResponse> getRiskLevels() {
        return Arrays.stream(RiskLevel.values())
                .map(r -> RiskLevelMetadataResponse.builder()
                        .value(r.getId())
                        .levelCode(r.getLevelCode())
                        .label(r.getDisplayName())
                        .actionType(r.getActionType())
                        .description(getDescriptionForRiskLevel(r))
                        .build())
                .collect(Collectors.toList());
    }

    private String getDescriptionForRiskLevel(RiskLevel level) {
        switch (level) {
            case MONITOR:
                return "일반 모니터링 대상 - 정기적 확인 필요";
            case INTENSIVE:
                return "집중 관찰 필요 - 담당자 주의 요망";
            case REVIEW:
                return "담당자 심사 필요 - 거래 보류 가능";
            case BLOCK:
                return "거래 차단 - 즉시 조치 필요";
            default:
                return "";
        }
    }

    /**
     * 엔티티 타입 메타데이터 조회 (시나리오의 주요 엔티티 타입)
     */
    public List<EntityTypeMetadataResponse> getEntityTypes() {
        return Arrays.asList(
                EntityTypeMetadataResponse.builder()
                        .value("CUSTOMER")
                        .label("고객")
                        .description("고객 기반 탐지 - 고객 ID를 기준으로 집계")
                        .build(),
                EntityTypeMetadataResponse.builder()
                        .value("ACCOUNT")
                        .label("계좌")
                        .description("계좌 기반 탐지 - 계좌번호를 기준으로 집계")
                        .build(),
                EntityTypeMetadataResponse.builder()
                        .value("AUTHENTICATION")
                        .label("인증")
                        .description("인증 기반 탐지 - 인증 세션을 기준으로 집계")
                        .build()
        );
    }
}
