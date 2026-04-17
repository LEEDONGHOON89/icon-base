package com.icon.agent.config;

/**
 * FILE_SYSTEM_REALTIME 수집기 설정.
 *
 * FileCollectorConfig와 동일한 tail 기반 증분 수집 동작을 수행하며,
 * DataSourceType.FILE_SYSTEM_REALTIME 타입에 대응한다.
 *
 * - pollIntervalMs 주기마다 디렉토리를 스캔하여 패턴에 매칭되는 파일을 감시
 * - 각 파일의 마지막 읽기 바이트 오프셋을 data/{targetId}/{collectorId}/positions.dat 에 저장
 * - 재기동 시 저장된 오프셋 이후부터 재개 (증분 수집)
 * - 파일 로테이션(rename / copytruncate) 자동 감지 후 오프셋 리셋
 */
public class FileSystemRealtimeCollectorConfig extends FileCollectorConfig {

    public FileSystemRealtimeCollectorConfig() {
        setType("FILE_SYSTEM_REALTIME");
    }
}
