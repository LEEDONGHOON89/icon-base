package com.itmasters.icon.engine.datasource.service;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDsFileSystemConfigEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDsFileSystemLogEntity;
import com.itmasters.icon.engine.datasource.repository.FileSystemConfigRepository;
import com.itmasters.icon.engine.datasource.repository.FileSystemLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 파일 시스템 관련 서비스
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class FileSystemService {

    private final FileSystemConfigRepository configRepository;
    private final FileSystemLogRepository fileSystemLogRepository;

    /**
     * 데이터소스 ID로 파일 시스템 설정 조회 (데이터베이스 기반)
     */
    public List<String> getFilePathsByDataSourceId(String dataSourceId) {
        List<EngineDsFileSystemConfigEntity> configs = configRepository.findAllByDataSourceId(dataSourceId);
        if (configs.isEmpty()) {
            log.warn("No file system config found for dataSourceId: {}", dataSourceId);
            return List.of();
        }

        List<String> foundFiles = new ArrayList<>();
        for (EngineDsFileSystemConfigEntity config : configs) {
            try {
                String watchDirectory = config.getWatchDirectory();
                String filePattern = config.getFilePattern();
                
                log.debug("Searching files in directory: {} with pattern: {}", watchDirectory, filePattern);
                
                Path directory = Paths.get(watchDirectory);
                if (!Files.exists(directory)) {
                    log.warn("Directory does not exist: {}", watchDirectory);
                    continue;
                }
                
                // 파일 패턴에 따라 파일 검색
                List<Path> matchingFiles = findMatchingFiles(directory, filePattern);
                for (Path file : matchingFiles) {
                    foundFiles.add(file.toAbsolutePath().toString());
                }
                
            } catch (Exception e) {
                log.error("Error searching files for config: {} - {}", config.getId(), e.getMessage());
            }
        }
        
        if (foundFiles.isEmpty()) {
            log.warn("No matching files found for dataSourceId: {}", dataSourceId);
        }
        else {
            log.info("Found {} files for dataSourceId: {}", foundFiles.size(), dataSourceId);
        }
        
        return foundFiles;
    }

    /**
     * 디렉토리에서 패턴에 맞는 파일 검색
     */
    private List<Path> findMatchingFiles(Path directory, String filePattern) throws IOException {
        List<Path> matchingFiles = new ArrayList<>();
        
        // 파일 패턴을 glob 패턴으로 변환 (*.csv 등)
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + filePattern);
        
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                 .filter(path -> matcher.matches(path.getFileName()))
                 .forEach(matchingFiles::add);
        }
        
        return matchingFiles;
    }
    
    /**
     * 첫 번째 파일 경로 반환 (기존 메서드와의 호환성)
     */
    public Optional<String> getFilePathByDataSourceId(String dataSourceId) {
        List<String> files = getFilePathsByDataSourceId(dataSourceId);
        return files.isEmpty() ? Optional.empty() : Optional.of(files.get(0));
    }

    /**
     * 처리할 파일 경로 조회 (중복 처리 방지) - 데이터베이스 기반
     */
    public List<Path> getUnprocessedFiles(String dataSourceId) {
        List<String> allFiles = getFilePathsByDataSourceId(dataSourceId);
        if (allFiles.isEmpty()) {
            log.warn("No files found for dataSourceId: {}", dataSourceId);
            return List.of();
        }

        List<Path> unprocessedFiles = new ArrayList<>();
        for (String filePathStr : allFiles) {
            Path filePath = Paths.get(filePathStr);
            
            // 파일이 존재하고 아직 처리되지 않았으면 추가
            if (Files.exists(filePath) && !fileSystemLogRepository.isSuccessfullyProcessed(filePath.toString())) {
                unprocessedFiles.add(filePath);
                log.debug("Found unprocessed file: {}", filePath);
            } else {
                log.debug("File already processed or does not exist: {}", filePath);
            }
        }

        if (unprocessedFiles.isEmpty()) {
            log.info("No unprocessed files for dataSourceId: {}", dataSourceId);
        } else {
            log.info("Found {} unprocessed files for dataSourceId: {}", unprocessedFiles.size(), dataSourceId);
        }
        
        return unprocessedFiles;
    }

    /**
     * 파일 처리 성공 로그 기록 (간단한 구현)
     */
    @Transactional
    public void recordProcessingSuccess(String dataSourceId, Path filePath, int rowsProcessed, String logId) {
        try {
            long fileSize = Files.size(filePath);
            String fileName = filePath.getFileName().toString();
            String absolutePath = filePath.toAbsolutePath().toString();
            String configId = getConfigIdByDataSourceId(dataSourceId);

            EngineDsFileSystemLogEntity successLog = EngineDsFileSystemLogEntity.of(
                    logId,
                    configId,
                    absolutePath,
                    fileName,
                    fileSize,
                    java.time.LocalDateTime.now(),
                    rowsProcessed,
                    EngineDsFileSystemLogEntity.ProcessingStatus.SUCCESS,
                    null
            );

            fileSystemLogRepository.save(successLog);
            log.info("Recorded successful processing: {} ({} rows)", fileName, rowsProcessed);

        } catch (IOException e) {
            log.error("Failed to get file size for: {}", filePath, e);
            // 파일 크기를 가져올 수 없어도 로그는 기록
            String fileName = filePath.getFileName().toString();
            String absolutePath = filePath.toAbsolutePath().toString();
            String configId = getConfigIdByDataSourceId(dataSourceId);

            EngineDsFileSystemLogEntity successLog = EngineDsFileSystemLogEntity.of(
                    logId,
                    configId,
                    absolutePath,
                    fileName,
                    0L,
                    java.time.LocalDateTime.now(),
                    rowsProcessed,
                    EngineDsFileSystemLogEntity.ProcessingStatus.SUCCESS,
                    null
            );

            fileSystemLogRepository.save(successLog);
        }
    }

    /**
     * 파일 처리 실패 로그 기록 (간단한 구현)
     */
    @Transactional
    public void recordProcessingFailure(String dataSourceId, Path filePath, String errorMessage, String logId) {
        String fileName = filePath.getFileName().toString();
        String absolutePath = filePath.toAbsolutePath().toString();
        String configId = getConfigIdByDataSourceId(dataSourceId);

            EngineDsFileSystemLogEntity failedLog = EngineDsFileSystemLogEntity.of(
                    logId,
                    configId,
                    absolutePath,
                    fileName,
                    null,
                    java.time.LocalDateTime.now(),
                    null,
                    EngineDsFileSystemLogEntity.ProcessingStatus.FAILED,
                    errorMessage
            );

            fileSystemLogRepository.save(failedLog);
            log.error("Recorded failed processing: {} - {}", fileName, errorMessage);
    }

    /**
     * 처리 이력 조회 (간단한 구현)
     */
    public List<EngineDsFileSystemLogEntity> getProcessingHistory(String dataSourceId) {
        String configId = getConfigIdByDataSourceId(dataSourceId);
        return fileSystemLogRepository.findByConfigId(configId);
    }

    /**
     * 데이터소스 ID에 따른 첫 번째 config ID 조회 (데이터베이스 기반)
     */
    private String getConfigIdByDataSourceId(String dataSourceId) {
        List<EngineDsFileSystemConfigEntity> configs = configRepository.findAllByDataSourceId(dataSourceId);
        if (configs.isEmpty()) {
            log.warn("No config found for dataSourceId: {}, using default", dataSourceId);
            return "0MFILEDEFAULT";
        }
        
        // 첫 번째 설정의 config ID 반환
        String configId = configs.get(0).getId();
        log.debug("Found config ID: {} for dataSourceId: {}", configId, dataSourceId);
        return configId;
    }

}
