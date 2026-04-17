package com.itmasters.icon.engine.datasource;

import com.itmasters.icon.common.util.TsidGenerator;
import com.itmasters.icon.common.constants.EngineConstants;
import com.itmasters.icon.engine.datasource.realtime.FileSystemRealtimeService;
import com.itmasters.icon.engine.datasource.repository.FileSystemConfigRepository;
import com.itmasters.icon.engine.datasource.service.DatabaseService;
import com.itmasters.icon.engine.datasource.service.FileSystemService;
import com.itmasters.icon.engine.datasource.domain.FileSystemConfig;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineDataSourceRepository;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceEntity;
import com.itmasters.icon.engine.reader.DataReaderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 데이터소스 리더
 * DataSource 엔티티 기반으로 데이터를 읽기만 담당
 * 파일 시스템 연결 정보와 처리 이력을 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSourceReader {

    private final DataReaderFactory dataReaderFactory;
    private final EngineDataSourceRepository engineDataSourceRepository;
    private final FileSystemService fileSystemService;
    private final FileSystemRealtimeService fileSystemRealtimeService;
    private final FileSystemConfigRepository fileSystemConfigRepository;
    // [2026-03-13] DATABASE 타입 직접 JDBC 폴링 서비스
    private final DatabaseService databaseService;

    /**
     * DataSource ID로 데이터 읽기 (실제 운영용)
     *
     * @param dataSourceId 데이터소스 ID
     * @return 읽은 데이터
     */
    public List<Map<String, Object>> readByDataSourceId(String dataSourceId) {
        log.info("데이터소스 조회 및 읽기 시작 - DataSourceId: {}", dataSourceId);

        // DataSource 엔티티 조회
        var dataSource = engineDataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("데이터소스를 찾을 수 없습니다: " + dataSourceId));

        // 활성화 상태 확인
        if (!dataSource.getIsActive()) {
            throw new IllegalStateException("비활성화된 데이터소스입니다: " + dataSourceId);
        }

        return read(dataSource);
    }

    /**
     * DataSource 엔티티 기반으로 데이터 읽기
     *
     * @param dataSource 데이터소스 엔티티
     * @return 읽은 데이터
     */
    public List<Map<String, Object>> read(EngineDataSourceEntity dataSource) {
        log.info("데이터 읽기 시작 - DataSourceId: {}, Type: {}",
                dataSource.getDataSourceId(), dataSource.getSourceType());
        try {
            switch (dataSource.getSourceType()) {
                case FILE_SYSTEM:
                    return readFromFileSystem(dataSource.getDataSourceId());
                case FILE_SYSTEM_REALTIME:
                    return readFromFileSystemRealtime(dataSource.getDataSourceId());
                case CLOUD_STORAGE:
                    throw new UnsupportedOperationException("CLOUD STORAGE 미구현");
                case FTP:
                    throw new UnsupportedOperationException("FTP 미구현");
                case API:
                    // TODO: API 호출 구현
                    log.warn("API 데이터 소스는 아직 구현되지 않았습니다.");
                    throw new UnsupportedOperationException("API 데이터 소스 미구현");

                case DATABASE:
                    // [2026-03-13] JDBC 하이워터마크 폴링 구현
                    return readFromDatabase(dataSource.getDataSourceId());

                case MESSAGE_QUEUE:
                    // TODO: 메시지 큐 구현
                    log.warn("MESSAGE_QUEUE 데이터 소스는 아직 구현되지 않았습니다.");
                    throw new UnsupportedOperationException("MESSAGE_QUEUE 데이터 소스 미구현");

                case LOG_SERVER:
                case SYSLOG:
                case ELASTIC_SEARCH:
                case SPLUNK:
                    // TODO: 로그 서버 구현
                    log.warn("{} 데이터 소스는 아직 구현되지 않았습니다.", dataSource.getSourceType());
                    throw new UnsupportedOperationException(dataSource.getSourceType() + " 데이터 소스 미구현");

                default:
                    throw new IllegalArgumentException("지원하지 않는 데이터소스 타입: " + dataSource.getSourceType());
            }
        } catch (Exception e) {
            log.error("데이터 읽기 실패 - Type: {}", dataSource.getSourceType(), e);
            throw new RuntimeException("데이터 읽기 실패", e);
        }
    }

    /**
     * 파일 시스템에서 데이터 읽기
     * 중복 처리 방지 및 처리 이력 관리
     */
    private List<Map<String, Object>> readFromFileSystem(String dataSourceId) {
        log.info("파일 시스템에서 데이터 읽기 시작 - DataSourceId: {}", dataSourceId);
        
        // 처리할 파일 목록 조회 (중복 처리 방지)
        List<Path> unprocessedFiles = fileSystemService.getUnprocessedFiles(dataSourceId);
        
        if (unprocessedFiles.isEmpty()) {
            log.info("처리할 새 파일이 없습니다 - DataSourceId: {}", dataSourceId);
            return new ArrayList<>();
        }
        
        List<Map<String, Object>> allData = new ArrayList<>();
        
        for (Path filePath : unprocessedFiles) {
            try {
                log.info("파일 처리 시작: {}", filePath);
                
                // 파일 읽기
                List<Map<String, Object>> fileData = dataReaderFactory.read(filePath.toString());
                allData.addAll(fileData);
                
                // 성공 로그 기록
                String logId = generateLogId();
                fileSystemService.recordProcessingSuccess(dataSourceId, filePath, fileData.size(), logId);
                
                log.info("파일 처리 완료: {} ({} rows)", filePath, fileData.size());
                
            } catch (Exception e) {
                log.error("파일 처리 실패: {}", filePath, e);
                
                // 실패 로그 기록
                String logId = generateLogId();
                fileSystemService.recordProcessingFailure(dataSourceId, filePath, e.getMessage(), logId);
                
                // 파일 하나가 실패해도 다른 파일은 계속 처리
                continue;
            }
        }
        
        log.info("파일 시스템 데이터 읽기 완료 - DataSourceId: {}, 총 {}건", dataSourceId, allData.size());
        return allData;
    }
    
    /**
     * 파일 시스템 실시간(증분) 데이터 읽기
     * - agentId가 설정된 경우: 에이전트 수집 모드(Push) — 로컬 파일 읽기 스킵, 빈 목록 반환
     *   (에이전트가 RpcClient를 통해 데이터를 push하므로 pipeline은 handleBatch에서 직접 호출)
     * - agentId 미설정: 기존 로컬 파일 증분 읽기 수행
     */
    private List<Map<String, Object>> readFromFileSystemRealtime(String dataSourceId) {
        log.info("파일 시스템 실시간 증분 읽기 시작 - DataSourceId: {}", dataSourceId);
        boolean isAgentMode = fileSystemConfigRepository.findAllByDataSourceId(dataSourceId).stream()
                .anyMatch(c -> c.getAgentId() != null && !c.getAgentId().isBlank());
        if (isAgentMode) {
            log.info("[{}] 에이전트 수집 모드 - 로컬 파일 읽기 스킵 (에이전트 push 대기)", dataSourceId);
            return List.of();
        }
        List<Map<String, Object>> data = fileSystemRealtimeService.readIncrementalData(dataSourceId);
        log.info("파일 시스템 실시간 증분 읽기 완료 - DataSourceId: {}, {} 건", dataSourceId, data.size());
        return data;
    }

    /**
     * [2026-03-13] DATABASE에서 증분 데이터 읽기
     * - agent_id 설정 시: 에이전트 Push 모드 (빈 목록 반환)
     * - agent_id 미설정: DatabaseService를 통해 직접 JDBC 폴링
     */
    private List<Map<String, Object>> readFromDatabase(String dataSourceId) {
        log.info("DATABASE 증분 읽기 시작 - DataSourceId: {}", dataSourceId);
        List<Map<String, Object>> data = databaseService.readIncrementalData(dataSourceId);
        log.info("DATABASE 증분 읽기 완료 - DataSourceId: {}, {} 건", dataSourceId, data.size());
        return data;
    }

    /**
     * 로그 ID 생성 (TSID 사용, 13자 보장)
     */
    private String generateLogId() {
        // TsidGenerator를 사용하여 정확히 13자의 유니크한 ID 생성
        // 파일 시스템 로그 전용 노드 ID 사용
        return TsidGenerator.generate(EngineConstants.NodeId.FILE_SYSTEM_LOG);
    }

}