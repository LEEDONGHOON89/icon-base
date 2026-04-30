package com.itmasters.icon.api.datasource.application.service;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DatabaseConfigEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DataSourceEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.FileSystemConfigEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.DatabaseConfigJpaRepository;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.FileSystemConfigJpaRepository;
import com.itmasters.icon.api.datasource.application.port.out.DataSourceRepository;
import com.itmasters.icon.api.datasource.dto.DataSourceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 데이터 소스 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    // [2026-04-21] 활성화/비활성화 시 FileSystemConfigEntity·DatabaseConfigEntity 동기화 및 에이전트 COLLECTORS_SYNC 푸시
    private final FileSystemConfigJpaRepository fileSystemConfigJpaRepository;
    private final DatabaseConfigJpaRepository databaseConfigJpaRepository;
    private final AgentSnapshotService agentSnapshotService;
    
    /**
     * 데이터 소스 생성
     */
    @Transactional
    public DataSourceDto.Info createDataSource(DataSourceDto.CreateCommand command) {
        log.info("Creating data source: {}", command.getName());
        
        // 동일 이름의 데이터 소스가 있는지 확인
        if (dataSourceRepository.existsByName(command.getName())) {
            throw new RuntimeException("이미 존재하는 데이터 소스 이름입니다: " + command.getName());
        }
        
        // DataSourceEntity 생성
        DataSourceEntity dataSource = DataSourceEntity.from(command);
        
        DataSourceEntity savedDataSource = dataSourceRepository.save(dataSource);
        log.info("Data source created successfully: {}", savedDataSource.getDataSourceId());
        
        // DEFAULT Profile 자동 생성
//        try {
//            dataProfileUseCase.createProfile(
//                savedDataSource.getDataSourceId(),
//                "DEFAULT",
//                ProfilePurpose.DEFAULT,
//                "자동 생성된 기본 프로파일",
//                0
//            );
//            log.info("DEFAULT profile created for data source: {}", savedDataSource.getDataSourceId());
//        } catch (Exception e) {
//            log.warn("Failed to create DEFAULT profile for data source {}: {}",
//                savedDataSource.getDataSourceId(), e.getMessage());
//            // Profile 생성 실패가 DataSource 생성을 막지 않도록 함
//        }
        
        return savedDataSource.toInfo();
    }
    
    /**
     * 데이터 소스 수정
     */
    @Transactional
    public DataSourceDto.Info updateDataSource(DataSourceDto.UpdateCommand command) {
        DataSourceEntity dataSource = dataSourceRepository.findById(command.getDataSourceId())
                .orElseThrow(() -> new RuntimeException("데이터 소스를 찾을 수 없습니다. ID: " + command.getDataSourceId()));
        
        // 이름 중복 검사 (자신 제외)
        if (command.getName() != null && !command.getName().equals(dataSource.getName())) {
            if (dataSourceRepository.existsByName(command.getName())) {
                throw new RuntimeException("이미 존재하는 데이터 소스 이름입니다: " + command.getName());
            }
        }
        
        // Entity의 업데이트 메서드 사용
        dataSource.update(command);
        
        DataSourceEntity updatedDataSource = dataSourceRepository.save(dataSource);
        log.info("Data source updated successfully: {}", updatedDataSource.getDataSourceId());
        
        // TODO: Rule mapping count 구현 필요
        Long ruleCount = 0L;
        return updatedDataSource.toInfoWithRuleCount(ruleCount);
    }
    
    /**
     * 데이터 소스 단건 조회
     */
    public DataSourceDto.Info getDataSource(String dataSourceId) {
        DataSourceEntity dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("데이터 소스를 찾을 수 없습니다. ID: " + dataSourceId));
        
        // TODO: Rule mapping count 구현 필요
        Long ruleCount = 0L;
        return dataSource.toInfoWithRuleCount(ruleCount);
    }
    
    /**
     * 전체 데이터 소스 목록 조회
     */
    public List<DataSourceDto.Info> getAllDataSources() {
        List<DataSourceEntity> dataSources = dataSourceRepository.findAll();
        
        return dataSources.stream()
                .map(dataSource -> {
                    // TODO: Rule mapping count 구현 필요
                    Long ruleCount = 0L;
                    return dataSource.toInfoWithRuleCount(ruleCount);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 데이터 소스 삭제
     */
    @Transactional
    public void deleteDataSource(String dataSourceId) {
        log.info("Deleting data source: {}", dataSourceId);
        
        DataSourceEntity dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("데이터 소스를 찾을 수 없습니다. ID: " + dataSourceId));
        
        // TODO: 매핑된 규칙 체크 구현 필요
        // long mappingCount = mappingRepository.countByDataSourceId(dataSourceId);
        // if (mappingCount > 0) {
        //     throw new RuntimeException("매핑된 규칙이 있어 삭제할 수 없습니다. 매핑 수: " + mappingCount);
        // }
        
        dataSourceRepository.delete(dataSource);
        log.info("Data source deleted successfully: {}", dataSourceId);
    }
    
    /**
     * 데이터 소스 활성화
     */
    @Transactional
    public DataSourceDto.Info activateDataSource(String dataSourceId) {
        DataSourceEntity dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("데이터 소스를 찾을 수 없습니다. ID: " + dataSourceId));

        dataSource.activate();
        DataSourceEntity savedDataSource = dataSourceRepository.save(dataSource);

        // [2026-04-21] FileSystemConfigEntity / DatabaseConfigEntity isActive 동기화
        //              DataSourceEntity.isActive 변경이 스냅샷·COLLECTORS_SYNC에 반영되도록
        syncConfigActiveState(dataSourceId, true);

        Long ruleCount = 0L;
        return savedDataSource.toInfoWithRuleCount(ruleCount);
    }

    /**
     * 데이터 소스 비활성화
     */
    @Transactional
    public DataSourceDto.Info deactivateDataSource(String dataSourceId) {
        DataSourceEntity dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("데이터 소스를 찾을 수 없습니다. ID: " + dataSourceId));

        dataSource.deactivate();
        DataSourceEntity savedDataSource = dataSourceRepository.save(dataSource);

        // [2026-04-21] FileSystemConfigEntity / DatabaseConfigEntity isActive 동기화
        syncConfigActiveState(dataSourceId, false);

        Long ruleCount = 0L;
        return savedDataSource.toInfoWithRuleCount(ruleCount);
    }

    /**
     * [2026-04-21] ds_file_system_config / ds_database_config 의 is_active 를 DataSourceEntity와 동기화하고,
     * 에이전트가 연결된 경우 COLLECTORS_SYNC 를 자동 푸시한다.
     */
    private void syncConfigActiveState(String dataSourceId, boolean active) {
        String agentId = null;

        var fsOpt = fileSystemConfigJpaRepository.findByDataSourceId(dataSourceId);
        if (fsOpt.isPresent()) {
            FileSystemConfigEntity fs = fsOpt.get();
            if (active) { fs.activate(); } else { fs.deactivate(); }
            fileSystemConfigJpaRepository.save(fs);
            if (agentId == null) agentId = fs.getAgentId();
        }

        var dbOpt = databaseConfigJpaRepository.findByDataSourceId(dataSourceId);
        if (dbOpt.isPresent()) {
            DatabaseConfigEntity db = dbOpt.get();
            if (active) { db.activate(); } else { db.deactivate(); }
            databaseConfigJpaRepository.save(db);
            if (agentId == null) agentId = db.getAgentId();
        }

        if (agentId != null && !agentId.isBlank()) {
            log.info("[DataSource] isActive={} 변경 → 에이전트 COLLECTORS_SYNC 푸시: agentId={}, dataSourceId={}",
                    active, agentId, dataSourceId);
            agentSnapshotService.pushSnapshot(agentId);
        }
    }
    
    // 기본 데이터 소스 설정 기능 제거
}