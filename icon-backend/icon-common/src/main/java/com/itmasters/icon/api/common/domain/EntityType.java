package com.itmasters.icon.common.domain;

/**
 * 시스템에서 사용하는 엔티티 타입
 * ID 생성 시 엔티티 타입을 구분하기 위해 사용
 */
public enum EntityType {
    USER("User"),
    DATA_SOURCE("DataSource"),
    DATA_SOURCE_SCHEMA("DataSourceSchema"),
    RULE("Rule"),
    SCENARIO("Scenario"),
    SCENARIO_RULE("ScenarioRule"),
    ROLE("Role"),
    COMPANY("Company"),
    PROFILE_SCHEMA("ProfileSchema"),
    STANDARD_FIELD("StandardField"),
    RULE_HISTORY("RuleHistory"),
    REFRESH_TOKEN("RefreshToken"),
    // [2026-04-20] 파서 기능 추가
    PARSER("Parser"),
    PARSER_RULE("ParserRule"),
    ;

    private final String displayName;

    EntityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 문자열로부터 EntityType을 찾음
     *
     * @param value 엔티티 타입 문자열 (대소문자 구분 없음)
     * @return EntityType 또는 null
     */
    public static EntityType fromString(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.toUpperCase()
                .replace("-", "_")
                .replace(" ", "_");

        try {
            return EntityType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // 특수 케이스 처리
            return switch (normalized) {
                case "DATASOURCE" -> DATA_SOURCE;
                case "SCENARIORULE" -> SCENARIO_RULE;
                case "PROFILESCHEMA" -> PROFILE_SCHEMA;
                case "STANDARDFIELD" -> STANDARD_FIELD;
                default -> null;
            };
        }
    }
}