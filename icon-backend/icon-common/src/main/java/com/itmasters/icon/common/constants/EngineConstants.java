package com.itmasters.icon.common.constants;

/**
 * 엔진 모듈에서 사용하는 공통 상수
 * 하드코딩 제거를 위한 중앙 관리
 */
public final class EngineConstants {

    private EngineConstants() {
        throw new AssertionError("Constants class cannot be instantiated");
    }

    /**
     * TSID 노드 ID 관련 상수
     */
    public static final class NodeId {
        private NodeId() {}

        /**
         * 파일 시스템 로그 전용 노드 ID
         * 파일 시스템에서 생성되는 로그 ID에 사용
         */
        public static final int FILE_SYSTEM_LOG = 999;
    }

    /**
     * 필드 관련 상수
     */
    public static final class Field {
        private Field() {}

        /**
         * 메타데이터 필드 접두사
         * 메타데이터 필드는 언더스코어(_)로 시작
         * 예: _source, _timestamp, _metadata
         */
        public static final String METADATA_PREFIX = "_";

        /**
         * 메타데이터 필드 여부 확인
         */
        public static boolean isMetadataField(String fieldName) {
            return fieldName != null && fieldName.startsWith(METADATA_PREFIX);
        }
    }

    /**
     * GroupKey 관련 상수
     */
    public static final class GroupKey {
        private GroupKey() {}

        /**
         * 복합 키(COMPOSITE) 구분자
         * 여러 필드를 조합할 때 사용
         * 예: CUS_ID + ACCOUNT_NO → "CUS001_ACC123"
         */
        public static final String COMPOSITE_SEPARATOR = "_";

        /**
         * 기본 그룹 키 이름
         * group_key가 설정되지 않았을 때 사용
         */
        public static final String DEFAULT_GROUP_NAME = "default";

        /**
         * 커스텀 그룹 키 기본값
         * 커스텀 키 값이 없을 때 사용
         */
        public static final String CUSTOM_DEFAULT_VALUE = "custom";

        /**
         * Null 값 대체 문자열
         * group_key 생성 시 null 값을 표현
         */
        public static final String NULL_VALUE_PLACEHOLDER = "null";
    }

    /**
     * StreamKey 관련 상수
     */
    public static final class StreamKey {
        private StreamKey() {}

        /**
         * StreamKey 표시 구분자
         * group_key + timestamp_key 조합 표시용
         * 예: "CUS_ID+TRX_DT"
         */
        public static final String DISPLAY_SEPARATOR = "+";

        /**
         * 기본 그룹 키 필드
         * 활성 프로파일이 없을 때 사용
         */
        public static final String DEFAULT_GROUP_KEY = "CUS_ID";

        /**
         * 기본 타임스탬프 필드
         * 활성 프로파일이 없을 때 사용
         */
        public static final String DEFAULT_TIMESTAMP_KEY = "TRX_DT";
    }
}
