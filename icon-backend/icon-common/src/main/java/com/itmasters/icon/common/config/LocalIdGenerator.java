package com.itmasters.icon.common.config;

import com.itmasters.icon.common.domain.EntityType;
import com.itmasters.icon.common.domain.IdGenerator;
import com.itmasters.icon.common.util.TsidGenerator;
import org.springframework.stereotype.Component;

/**
 * 로컬 ID 생성기 구현체
 * TSID를 사용하여 도메인별로 고유한 ID를 생성
 * icon-api는 모든 엔티티 타입을 지원

 * Node ID 할당:
 * - User: 100
 * - DataSource: 200
 * - Rule: 400
 * - Scenario: 500
 * - ScenarioRule: 550
 * - Role: 600
 * - Company: 700
 * - DataSourceSchema: 900
 * - ProfileSchema: 950
 * - StandardField: 1000

 * MSA 환경에서는 이 구현체 대신 RemoteIdGenerator를 사용
 */
@Component
public class LocalIdGenerator implements IdGenerator {

    // 각 도메인별 Node ID 상수
    private static final int NODE_ID_USER = 100;
    private static final int NODE_ID_DATA_SOURCE = 200;
    private static final int NODE_ID_RULE = 400;
    private static final int NODE_ID_RULE_HISTORY = 410;
    private static final int NODE_ID_SCENARIO = 500;
    private static final int NODE_ID_SCENARIO_RULE = 550;
    private static final int NODE_ID_ROLE = 600;
    private static final int NODE_ID_COMPANY = 700;
    private static final int NODE_ID_DATA_SOURCE_SCHEMA = 900;
    private static final int NODE_ID_PROFILE_SCHEMA = 950;
    private static final int NODE_ID_STANDARD_FIELD = 1000;
    // TSID node id must be in [0, 1023]
    private static final int NODE_ID_REFRESH_TOKEN = 1001;


    @Override
    public String generateId(EntityType entityType) {
        return switch (entityType) {
            case USER -> TsidGenerator.generate(NODE_ID_USER);
            case DATA_SOURCE -> TsidGenerator.generate(NODE_ID_DATA_SOURCE);
            case RULE -> TsidGenerator.generate(NODE_ID_RULE);
            case SCENARIO -> TsidGenerator.generate(NODE_ID_SCENARIO);
            case SCENARIO_RULE -> TsidGenerator.generate(NODE_ID_SCENARIO_RULE);
            case ROLE -> TsidGenerator.generate(NODE_ID_ROLE);
            case COMPANY -> TsidGenerator.generate(NODE_ID_COMPANY);
            case DATA_SOURCE_SCHEMA -> TsidGenerator.generate(NODE_ID_DATA_SOURCE_SCHEMA);
            case PROFILE_SCHEMA -> TsidGenerator.generate(NODE_ID_PROFILE_SCHEMA);
            case STANDARD_FIELD -> TsidGenerator.generate(NODE_ID_STANDARD_FIELD);
            case RULE_HISTORY -> TsidGenerator.generate(NODE_ID_RULE_HISTORY);
            case REFRESH_TOKEN -> TsidGenerator.generate(NODE_ID_REFRESH_TOKEN);
        };
    }
}
