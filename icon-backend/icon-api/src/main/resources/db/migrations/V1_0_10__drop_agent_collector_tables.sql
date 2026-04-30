-- [2026-04-22] agent_collector_configs / agent_collector_file_configs / agent_collector_jdbc_configs 테이블 삭제
-- 에이전트 수집기 설정은 ds_file_system_config / ds_database_config 가 단일 진실 공급원(Single Source of Truth)
-- AgentSnapshotService 가 해당 테이블에서 직접 스냅샷을 빌드하여 에이전트에 푸시
-- 구 CRUD 기반 collector API(/api/rpc/agents/.../collectors)는 완전히 제거됨

-- 자식 테이블 먼저 삭제 (FK 제약 조건 순서)
DROP TABLE IF EXISTS agent_collector_file_configs;
DROP TABLE IF EXISTS agent_collector_jdbc_configs;

-- 부모 테이블 삭제 (idx_collectors_target 인덱스 포함 자동 삭제됨)
DROP TABLE IF EXISTS agent_collector_configs;
