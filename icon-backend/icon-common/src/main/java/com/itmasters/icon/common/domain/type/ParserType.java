package com.itmasters.icon.common.domain.type;

// [2026-04-20] 파서 타입 enum 추가
public enum ParserType {

    /**
     * 구분자 기반 파서
     * config_json: {"delimiter": "|", "index": 0}
     * 예) "KR|SEOUL|USER001" → delimiter="|", index=1 → "SEOUL"
     */
    DELIMITER("구분자 파서", "지정 구분자로 값을 분리하여 인덱스로 추출"),

    /**
     * 고정 바이트 길이 파서
     * config_json: {"startByte": 0, "byteLength": 6}
     * 예) "KR SEOULUSR" → startByte=3, byteLength=5 → "SEOUL"
     */
    FIXED_WIDTH("고정폭 파서", "바이트 위치와 길이로 값을 추출"),

    /**
     * 정규식 파서
     * config_json: {"pattern": "^(\\w+)\\|(\\d+)", "group": 1}
     * 예) "KR|20240115" → pattern="^(\\w+)\\|(\\d+)", group=1 → "KR"
     */
    REGEX("정규식 파서", "정규 표현식의 캡처 그룹으로 값을 추출");

    private final String displayName;
    private final String description;

    ParserType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
