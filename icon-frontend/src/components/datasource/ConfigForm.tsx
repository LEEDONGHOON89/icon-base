"use client";

import { useState, useEffect } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { DataSource, DataSourceType, fetchDataSourceConfig, updateDataSourceConfig, FileSystemConfig, DatabaseConfig } from "@/app/data-sources/api";
// [2026-03-12] 에이전트 조회 API 임포트
import { fetchAgents, Agent } from "@/app/agents/api";
import LoadingButton from "@/components/common/LoadingButton";
import { toast } from "react-hot-toast";
import { useQueryWithErrorHandling } from "@/hooks/useQueryWithErrorHandling";

interface Props {
  dataSource: DataSource;
}

export default function ConfigForm({ dataSource }: Props) {
  const queryClient = useQueryClient();
  const { data: config, isLoading, isFetching, refetch, errorQuery } = useQueryWithErrorHandling({
    queryKey: ["data-source-config", dataSource.dataSourceId],
    queryFn: () => fetchDataSourceConfig(dataSource.dataSourceId),
  });

  const [fs, setFs] = useState<FileSystemConfig>({
    connectionName: "",
    watchDirectory: "",
    filePattern: "*.csv",
    fileEncoding: "UTF-8",
    delimiter: ",",
    hasHeader: true,
    // [2026-03-12] 에이전트 연결 초기값
    agentId: undefined,
  });
  // [2026-04-21] incrementalColumnType 기본값 "DATETIME" 명시 — 미설정 시 null로 저장되는 문제 방지
  const [db, setDb] = useState<DatabaseConfig>({
    connectionName: "",
    databaseType: "POSTGRES",
    port: 5432,
    minPoolSize: 1,
    maxPoolSize: 10,
    connectionTimeoutSeconds: 30,
    idleTimeoutSeconds: 300,
    incrementalColumnType: "DATETIME",
    batchSize: 1000,
  });

  const isFileSystemType = dataSource.sourceType === "FILE_SYSTEM" || dataSource.sourceType === "FILE_SYSTEM_REALTIME";
  const isRealtime = dataSource.sourceType === "FILE_SYSTEM_REALTIME";
  const isDatabaseType = dataSource.sourceType === "DATABASE";

  // [2026-03-12] FILE_SYSTEM_REALTIME 및 DATABASE 에이전트 선택을 위한 목록 조회
  const { data: agents = [] } = useQueryWithErrorHandling<Agent[]>({
    queryKey: ["agents"],
    queryFn: fetchAgents,
    enabled: isRealtime || isDatabaseType,
  });

  useEffect(() => {
    if (!config) return;
    if (isFileSystemType && config.fileSystem) {
      setFs({
        connectionName: config.fileSystem.connectionName || "",
        watchDirectory: config.fileSystem.watchDirectory || "",
        filePattern: config.fileSystem.filePattern || (isRealtime ? "*" : "*.csv"),
        fileEncoding: config.fileSystem.fileEncoding || "UTF-8",
        delimiter: config.fileSystem.delimiter || ",",
        quoteChar: config.fileSystem.quoteChar,
        escapeChar: config.fileSystem.escapeChar,
        hasHeader: config.fileSystem.hasHeader ?? !isRealtime,
        skipLines: config.fileSystem.skipLines,
        processingStrategy: config.fileSystem.processingStrategy,
        moveProcessedFiles: config.fileSystem.moveProcessedFiles,
        processedFilesDirectory: config.fileSystem.processedFilesDirectory,
        // [2026-03-12] 에이전트 연결 정보 초기 로드
        agentId: config.fileSystem.agentId || undefined,
        // [2026-04-22] 폴링 간격(초): pollIntervalMs(ms)→초 변환 우선, 없으면 scanIntervalMinutes(초) 사용
        // FILE_SYSTEM_REALTIME은 통합 폴링 간격(초)을 scanIntervalMinutes에 저장한다.
        pollIntervalMs: config.fileSystem.pollIntervalMs ?? undefined,
        scanIntervalMinutes: config.fileSystem.pollIntervalMs
          ? Math.round(config.fileSystem.pollIntervalMs / 1000)
          : (config.fileSystem.scanIntervalMinutes ?? (isRealtime ? 60 : 3600)),
        maxLinesPerPoll: config.fileSystem.maxLinesPerPoll ?? undefined,
        maxRecordBytes: config.fileSystem.maxRecordBytes ?? undefined,
      });
    }
    if (dataSource.sourceType === "DATABASE" && config.database) {
      setDb({
        connectionName: config.database.connectionName || "",
        databaseType: config.database.databaseType || "POSTGRESQL",
        host: config.database.host,
        port: config.database.port || 5432,
        databaseName: config.database.databaseName,
        schemaName: config.database.schemaName,
        username: config.database.username,
        // password는 반환하지 않음
        minPoolSize: config.database.minPoolSize || 1,
        maxPoolSize: config.database.maxPoolSize || 10,
        connectionTimeoutSeconds: config.database.connectionTimeoutSeconds || 30,
        idleTimeoutSeconds: config.database.idleTimeoutSeconds || 300,
        mainQuery: config.database.mainQuery,
        incrementalColumn: config.database.incrementalColumn,
        incrementalColumnType: config.database.incrementalColumnType,
        // [2026-04-21] 증분 컬럼 초기값 로드
        incrementalColumnInitialValue: config.database.incrementalColumnInitialValue,
        batchSize: config.database.batchSize || 1000,
        // [2026-03-13] 에이전트 연결 정보 초기 로드
        agentId: config.database.agentId || undefined,
        // [2026-04-21] 폴링 설정 초기 로드
        pollIntervalMs: config.database.pollIntervalMs ?? undefined,
        maxLinesPerPoll: config.database.maxLinesPerPoll ?? undefined,
        maxRecordBytes: config.database.maxRecordBytes ?? undefined,
      });
    }
  }, [config, dataSource.sourceType]);

  const saveMutation = useMutation({
    mutationFn: () => {
      // [2026-04-22] FILE_SYSTEM_REALTIME: 폴링 간격을 초 단위로 통합 관리
      // scanIntervalMinutes(초) → pollIntervalMs(ms) 자동 계산하여 함께 저장
      // 에이전트/백엔드 양쪽이 동일 값을 사용한다.
      const fileSystemPayload: FileSystemConfig = isRealtime
        ? { ...fs, pollIntervalMs: (fs.scanIntervalMinutes ?? 60) * 1000 }
        : fs;
      const payload = isFileSystemType ? { fileSystem: fileSystemPayload } : { database: db };
      return updateDataSourceConfig(dataSource.dataSourceId, payload);
    },
    onSuccess: () => {
      toast.success("연결 설정이 저장되었습니다.");
      queryClient.invalidateQueries({ queryKey: ["data-source-config", dataSource.dataSourceId] });
    },
  });

  if (isLoading || isFetching) {
    return (
      <div className="text-center py-10 text-gray-500">설정을 불러오는 중...</div>
    );
  }

  if (errorQuery) {
    return (
      <div className="text-center py-10">
        <p className="text-red-600 mb-3">설정을 불러오지 못했습니다.</p>
        <LoadingButton onClick={() => refetch()} type="button">다시 시도</LoadingButton>
      </div>
    );
  }

  // FILE_SYSTEM / FILE_SYSTEM_REALTIME / DATABASE 외 타입
  if (!isFileSystemType && dataSource.sourceType !== "DATABASE") {
    return (
      <div className="bg-blue-50 border border-blue-200 rounded-xl p-8 text-center">
        <div className="inline-flex items-center justify-center w-16 h-16 bg-blue-100 rounded-full mb-4">
          <svg className="h-8 w-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <h3 className="text-lg font-semibold text-gray-900 mb-2">
          연결 설정을 사용할 수 없습니다
        </h3>
        <p className="text-gray-600 mb-4">
          현재 데이터소스 타입({dataSource.sourceType})은 연결 설정이 필요하지 않습니다.
        </p>
        <p className="text-sm text-gray-500">
          &quot;원본 필드&quot; 탭에서 데이터 필드를 관리하거나, &quot;프로파일 관리&quot; 탭에서 데이터 처리 프로파일을 설정하세요.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* ── FILE_SYSTEM (배치) ── */}
      {dataSource.sourceType === "FILE_SYSTEM" && (
        <div className="space-y-4">
          <h3 className="text-lg font-semibold text-gray-900">파일 시스템 설정</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <LabeledInput label="연결 이름" value={fs.connectionName} onChange={(v) => setFs({ ...fs, connectionName: v })} />
            <LabeledInput label="감시 디렉토리" value={fs.watchDirectory} onChange={(v) => setFs({ ...fs, watchDirectory: v })} />
            <LabeledInput label="파일 패턴" value={fs.filePattern || ""} onChange={(v) => setFs({ ...fs, filePattern: v })} />
            <LabeledInput label="인코딩" value={fs.fileEncoding || ""} onChange={(v) => setFs({ ...fs, fileEncoding: v })} />
            <LabeledInput label="구분자" value={fs.delimiter || ""} onChange={(v) => setFs({ ...fs, delimiter: v })} />
            <LabeledCheckbox label="헤더 포함" checked={!!fs.hasHeader} onChange={(c) => setFs({ ...fs, hasHeader: c })} />
            <LabeledInput label="스캔 간격(분)" type="number" value={String(fs.scanIntervalMinutes ?? "")} onChange={(v) => setFs({ ...fs, scanIntervalMinutes: v ? Number(v) : undefined })} />
            <LabeledCheckbox label="처리된 파일 이동" checked={!!fs.moveProcessedFiles} onChange={(c) => setFs({ ...fs, moveProcessedFiles: c })} />
            <LabeledInput label="처리된 파일 디렉토리" value={fs.processedFilesDirectory || ""} onChange={(v) => setFs({ ...fs, processedFilesDirectory: v })} />
          </div>
        </div>
      )}

      {/* ── FILE_SYSTEM_REALTIME (실시간 tail 수집) ── */}
      {dataSource.sourceType === "FILE_SYSTEM_REALTIME" && (
        <div className="space-y-4">
          <div className="flex items-center gap-2">
            <h3 className="text-lg font-semibold text-gray-900">파일 시스템 실시간 설정</h3>
            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
              실시간 증분 수집
            </span>
          </div>
          <p className="text-sm text-gray-500">
            지정한 디렉토리의 파일에 추가되는 신규 라인을 폴링 간격마다 수집합니다.
            마지막 읽기 위치(오프셋)는 <code className="bg-gray-100 px-1 rounded text-xs">data/file-realtime/{"{dataSourceId}"}/positions.json</code> 에 저장됩니다.
          </p>

          {/* 기본 설정 */}
          <div>
            <h4 className="text-sm font-semibold text-gray-700 mb-3">기본 설정</h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <LabeledInput
                label="연결 이름"
                value={fs.connectionName}
                onChange={(v) => setFs({ ...fs, connectionName: v })}
              />
              <LabeledInput
                label="감시 디렉토리"
                value={fs.watchDirectory}
                onChange={(v) => setFs({ ...fs, watchDirectory: v })}
                placeholder="예: /var/log/app  또는  C:\logs\app"
              />
              <LabeledInput
                label="파일 패턴 (glob)"
                value={fs.filePattern || ""}
                onChange={(v) => setFs({ ...fs, filePattern: v })}
                placeholder="예: *.log  또는  app-*.csv"
              />
              {/* [2026-04-22] 폴링 간격: 초 단위 통합. 에이전트/백엔드 모두 이 값 사용 */}
              <LabeledInput
                label="폴링 간격 (초)"
                type="number"
                value={String(fs.scanIntervalMinutes ?? "60")}
                onChange={(v) => setFs({ ...fs, scanIntervalMinutes: v ? Number(v) : 60 })}
                placeholder="기본값: 60초 (1분)"
              />
            </div>
          </div>

          {/* [2026-03-12] 에이전트 연결 설정 */}
          <div>
            <h4 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
              에이전트 연결
              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
                선택사항
              </span>
            </h4>
            <p className="text-xs text-gray-500 mb-3">
              에이전트를 선택하면 연결 설정 저장 시 해당 에이전트에 수집기 정보가 자동으로 전달됩니다.
            </p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* 에이전트 선택 */}
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">에이전트</label>
                <select
                  className="w-full p-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 text-sm"
                  value={fs.agentId || ""}
                  onChange={(e) => {
                    const selected = e.target.value;
                    setFs({ ...fs, agentId: selected || undefined });
                  }}
                >
                  <option value="">-- 에이전트 선택 (선택 시 에이전트에서 수집) --</option>
                  {agents.map((agent) => (
                    <option key={agent.agentId} value={agent.agentId}>
                      {agent.displayName || agent.hostname} ({agent.status})
                    </option>
                  ))}
                </select>
              </div>

              {/* 연결 상태 표시 */}
              {fs.agentId && (
                <div className="md:col-span-2">
                  <div className="flex items-center gap-2 px-3 py-2 bg-green-50 border border-green-200 rounded-md">
                    <svg className="h-4 w-4 text-green-600 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                    <span className="text-xs text-green-700">
                      저장 시 선택된 에이전트에 파일 수집기가 자동 생성/업데이트되고 동기화됩니다.
                    </span>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* [2026-04-22] 에이전트 수집 설정 (에이전트 선택 시만 표시) — 폴링 간격은 기본 설정과 통합 */}
          {fs.agentId && (
            <div>
              <h4 className="text-sm font-semibold text-gray-700 mb-3">에이전트 수집 설정</h4>
              <p className="text-xs text-gray-500 mb-3">
                에이전트가 파일을 폴링할 때 적용되는 세부 설정입니다. 폴링 간격은 위 "기본 설정"의 값을 공통으로 사용합니다.
              </p>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">폴 당 최대 처리 라인 수</label>
                  <input
                    type="number"
                    className="w-full p-2 border border-gray-300 rounded-md text-sm"
                    placeholder="예: 10000"
                    value={fs.maxLinesPerPoll ?? ""}
                    onChange={(e) => setFs({ ...fs, maxLinesPerPoll: e.target.value ? Number(e.target.value) : undefined })}
                  />
                  <p className="text-xs text-gray-400 mt-0.5">1회 폴링 시 읽을 최대 줄 수. 미입력 시 기본값 10,000 적용</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">최대 레코드 크기 (bytes)</label>
                  <input
                    type="number"
                    className="w-full p-2 border border-gray-300 rounded-md text-sm"
                    placeholder="예: 65536 (64KB).  0 = 제한 없음"
                    value={fs.maxRecordBytes ?? ""}
                    onChange={(e) => setFs({ ...fs, maxRecordBytes: e.target.value ? Number(e.target.value) : undefined })}
                  />
                  <p className="text-xs text-gray-400 mt-0.5">줄 크기가 이 값을 초과하면 잘라냅니다. 0 입력 시 제한 없음</p>
                </div>
              </div>
            </div>
          )}

          {/* 파일 형식 */}
          <div>
            <h4 className="text-sm font-semibold text-gray-700 mb-3">파일 형식</h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">파일 형식</label>
                <select
                  className="w-full p-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 text-sm"
                  value={
                    (fs.filePattern || "").toLowerCase().endsWith(".csv")
                      ? "CSV"
                      : (fs.filePattern || "").toLowerCase().endsWith(".json")
                      ? "JSON"
                      : "LOG"
                  }
                  onChange={(e) => {
                    const fmt = e.target.value;
                    setFs({
                      ...fs,
                      filePattern:
                        fmt === "CSV"
                          ? "*.csv"
                          : fmt === "JSON"
                          ? "*.json"
                          : "*.log",
                      hasHeader: fmt === "CSV",
                    });
                  }}
                >
                  <option value="LOG">LOG (텍스트, 1줄=1레코드)</option>
                  <option value="CSV">CSV (헤더 자동 감지)</option>
                  <option value="JSON">JSON (줄 단위 JSON)</option>
                </select>
              </div>
              <LabeledInput
                label="인코딩"
                value={fs.fileEncoding || "UTF-8"}
                onChange={(v) => setFs({ ...fs, fileEncoding: v })}
                placeholder="UTF-8"
              />
              {(fs.filePattern || "").toLowerCase().endsWith(".csv") && (
                <>
                  <LabeledInput
                    label="구분자"
                    value={fs.delimiter || ","}
                    onChange={(v) => setFs({ ...fs, delimiter: v })}
                  />
                  <LabeledCheckbox
                    label="헤더 포함 (첫 번째 줄)"
                    checked={!!fs.hasHeader}
                    onChange={(c) => setFs({ ...fs, hasHeader: c })}
                  />
                </>
              )}
            </div>
          </div>

          {/* 상태 정보 (읽기 전용) */}
          {(config?.fileSystem?.connectionStatus || config?.fileSystem?.lastErrorMessage) && (
            <div className="bg-gray-50 rounded-lg p-4 space-y-2">
              <h4 className="text-sm font-semibold text-gray-700">수집 상태</h4>
              {config.fileSystem?.connectionStatus && (
                <div className="flex items-center gap-2">
                  <span className="text-xs text-gray-500">상태:</span>
                  <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                    config.fileSystem.connectionStatus === "IDLE"
                      ? "bg-gray-100 text-gray-700"
                      : config.fileSystem.connectionStatus === "ERROR"
                      ? "bg-red-100 text-red-700"
                      : "bg-green-100 text-green-700"
                  }`}>
                    {config.fileSystem.connectionStatus}
                  </span>
                </div>
              )}
              {config.fileSystem?.lastErrorMessage && (
                <p className="text-xs text-red-600 break-all">{config.fileSystem.lastErrorMessage}</p>
              )}
            </div>
          )}
        </div>
      )}

      {/* [2026-03-13] DATABASE 설정 — DB 타입 select, 증분 타입 select, 에이전트 연결 추가 */}
      {dataSource.sourceType === "DATABASE" && (
        <div className="space-y-6">
          <h3 className="text-lg font-semibold text-gray-900">데이터베이스 설정</h3>

          {/* 연결 정보 */}
          <div>
            <h4 className="text-sm font-semibold text-gray-700 mb-3">연결 정보</h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <LabeledInput
                label="연결 이름"
                value={db.connectionName}
                onChange={(v) => setDb({ ...db, connectionName: v })}
              />
              {/* DB 타입 — 지원 대상: MARIADB, POSTGRESQL, ORACLE */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">DB 타입</label>
                <select
                  className="w-full p-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 text-sm"
                  value={db.databaseType || "POSTGRESQL"}
                  onChange={(e) => {
                    const type = e.target.value;
                    const defaultPort = type === "MARIADB" ? 3306 : type === "ORACLE" ? 1521 : 5432;
                    setDb({ ...db, databaseType: type, port: defaultPort });
                  }}
                >
                  <option value="POSTGRESQL">PostgreSQL</option>
                  <option value="MARIADB">MariaDB</option>
                  <option value="ORACLE">Oracle</option>
                </select>
              </div>
              <LabeledInput
                label="호스트"
                value={db.host || ""}
                onChange={(v) => setDb({ ...db, host: v })}
                placeholder="예: localhost 또는 192.168.1.10"
              />
              <LabeledInput
                label="포트"
                type="number"
                value={String(db.port ?? "")}
                onChange={(v) => setDb({ ...db, port: v ? Number(v) : undefined })}
              />
              <LabeledInput
                label="데이터베이스명"
                value={db.databaseName || ""}
                onChange={(v) => setDb({ ...db, databaseName: v })}
              />
              <LabeledInput
                label="스키마명"
                value={db.schemaName || ""}
                onChange={(v) => setDb({ ...db, schemaName: v })}
                placeholder="PostgreSQL 전용 (선택사항)"
              />
              <LabeledInput
                label="사용자"
                value={db.username || ""}
                onChange={(v) => setDb({ ...db, username: v })}
              />
              <LabeledInput
                label="비밀번호"
                type="password"
                value={db.password || ""}
                onChange={(v) => setDb({ ...db, password: v })}
              />
              <LabeledInput
                label="최소 Pool"
                type="number"
                value={String(db.minPoolSize ?? "")}
                onChange={(v) => setDb({ ...db, minPoolSize: v ? Number(v) : undefined })}
              />
              <LabeledInput
                label="최대 Pool"
                type="number"
                value={String(db.maxPoolSize ?? "")}
                onChange={(v) => setDb({ ...db, maxPoolSize: v ? Number(v) : undefined })}
              />
              <LabeledInput
                label="연결 타임아웃(초)"
                type="number"
                value={String(db.connectionTimeoutSeconds ?? "")}
                onChange={(v) => setDb({ ...db, connectionTimeoutSeconds: v ? Number(v) : undefined })}
              />
              <LabeledInput
                label="유휴 타임아웃(초)"
                type="number"
                value={String(db.idleTimeoutSeconds ?? "")}
                onChange={(v) => setDb({ ...db, idleTimeoutSeconds: v ? Number(v) : undefined })}
              />
            </div>
          </div>

          {/* 적재 설정 */}
          <div>
            <h4 className="text-sm font-semibold text-gray-700 mb-3">적재 설정</h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="md:col-span-2">
                <LabeledInput
                  label="메인 쿼리"
                  textarea
                  value={db.mainQuery || ""}
                  onChange={(v) => setDb({ ...db, mainQuery: v })}
                  placeholder={"예: SELECT * FROM table WHERE updated_at > ? ORDER BY updated_at ASC"}
                />
              </div>
              <LabeledInput
                label="증분 컬럼"
                value={db.incrementalColumn || ""}
                onChange={(v) => setDb({ ...db, incrementalColumn: v })}
                placeholder="예: updated_at 또는 seq_no"
              />
              {/* 증분 컬럼 타입 select */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">증분 컬럼 타입</label>
                <select
                  className="w-full p-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 text-sm"
                  value={db.incrementalColumnType || "DATETIME"}
                  onChange={(e) => setDb({ ...db, incrementalColumnType: e.target.value })}
                >
                  <option value="DATETIME">DATETIME (날짜/시간)</option>
                  <option value="NUMBER">NUMBER (숫자 시퀀스/ID)</option>
                </select>
              </div>
              {/* [2026-04-21] 증분 컬럼 초기값 — 첫 수집 시작점 설정 */}
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  증분 컬럼 초기값
                  <span className="ml-2 text-xs font-normal text-gray-400">(선택사항 — 첫 수집 시작 기준값)</span>
                </label>
                <input
                  className="w-full p-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 text-sm"
                  type="text"
                  value={db.incrementalColumnInitialValue || ""}
                  onChange={(e) => setDb({ ...db, incrementalColumnInitialValue: e.target.value || undefined })}
                  placeholder={
                    (db.incrementalColumnType || "DATETIME") === "NUMBER"
                      ? "예: 0  (이 값보다 큰 레코드부터 수집)"
                      : "예: 2024-01-01 00:00:00  (이 시각 이후 레코드부터 수집)"
                  }
                />
                <p className="mt-1 text-xs text-gray-400">
                  미입력 시 DATETIME은 1970-01-01, NUMBER는 0부터 수집합니다. 이미 수집이 시작된 경우 이 값은 무시됩니다.
                </p>
              </div>
              <LabeledInput
                label="배치 크기"
                type="number"
                value={String(db.batchSize ?? "")}
                onChange={(v) => setDb({ ...db, batchSize: v ? Number(v) : undefined })}
                placeholder="기본값: 1000"
              />
              {/* [2026-04-22] 폴링 간격(초) — 에이전트/백엔드 직접 폴링 모두 적용 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  폴링 간격 (초)
                </label>
                <input
                  type="number"
                  className="w-full p-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 text-sm"
                  placeholder="예: 300 (5분)"
                  value={db.pollIntervalMs != null ? Math.round(db.pollIntervalMs / 1000) : ""}
                  onChange={(e) => setDb({ ...db, pollIntervalMs: e.target.value ? Number(e.target.value) * 1000 : undefined })}
                />
                <p className="text-xs text-gray-400 mt-0.5">미입력 시 기본값 300초 (5분) 적용</p>
              </div>
              {/* [2026-04-22] 폴 당 최대 처리 행 수 — 에이전트/백엔드 모두 적용 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  폴 당 최대 처리 행 수
                  <span className="ml-2 text-xs font-normal text-gray-400">(maxLinesPerPoll)</span>
                </label>
                <input
                  type="number"
                  className="w-full p-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 text-sm"
                  placeholder="예: 1000"
                  value={db.maxLinesPerPoll ?? ""}
                  onChange={(e) => setDb({ ...db, maxLinesPerPoll: e.target.value ? Number(e.target.value) : undefined })}
                />
                <p className="text-xs text-gray-400 mt-0.5">1회 폴링 시 가져올 최대 행 수. 미입력 시 기본값 1,000 적용</p>
              </div>
            </div>
          </div>

          {/* [2026-03-13] 에이전트 연결 설정 */}
          <div>
            <h4 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
              에이전트 연결
              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
                선택사항
              </span>
            </h4>
            <p className="text-xs text-gray-500 mb-3">
              에이전트를 선택하면 해당 에이전트가 데이터베이스에 직접 접근하여 JDBC 폴링 후 수집 결과를 Push합니다.
              미선택 시 엔진이 직접 JDBC 폴링합니다.
            </p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">에이전트</label>
                <select
                  className="w-full p-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 text-sm"
                  value={db.agentId || ""}
                  onChange={(e) => setDb({ ...db, agentId: e.target.value || undefined })}
                >
                  <option value="">-- 에이전트 선택 (미선택 시 엔진 직접 폴링) --</option>
                  {agents.map((agent) => (
                    <option key={agent.agentId} value={agent.agentId}>
                      {agent.displayName || agent.hostname} ({agent.status})
                    </option>
                  ))}
                </select>
              </div>
              {db.agentId && (
                <div className="md:col-span-2">
                  <div className="flex items-center gap-2 px-3 py-2 bg-green-50 border border-green-200 rounded-md">
                    <svg className="h-4 w-4 text-green-600 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                    <span className="text-xs text-green-700">
                      저장 시 선택된 에이전트에 전체 수집기 스냅샷이 자동 동기화됩니다.
                    </span>
                  </div>
                </div>
              )}
              {/* [2026-04-22] 에이전트 수집 설정 — 폴링 간격은 "적재 설정"과 통합 */}
              {db.agentId && (
                <>
                  <div className="md:col-span-2">
                    <div className="flex items-start gap-2 px-3 py-2 bg-blue-50 border border-blue-200 rounded-md">
                      <svg className="h-4 w-4 text-blue-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      <span className="text-xs text-blue-700">
                        폴링 간격 · 최대 처리 행 수는 위 <strong>적재 설정</strong>에서 공통으로 설정합니다.
                      </span>
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">최대 레코드 크기 (bytes)</label>
                    <input
                      type="number"
                      className="w-full p-2 border border-gray-300 rounded-md text-sm"
                      placeholder="예: 65536 (64KB).  0 = 제한 없음"
                      value={db.maxRecordBytes ?? ""}
                      onChange={(e) => setDb({ ...db, maxRecordBytes: e.target.value ? Number(e.target.value) : undefined })}
                    />
                    <p className="text-xs text-gray-400 mt-0.5">레코드가 이 값을 초과하면 잘라냅니다. 0 입력 시 제한 없음</p>
                  </div>
                </>
              )}
            </div>
          </div>

          {/* 상태 정보 (읽기 전용) */}
          {(config?.database?.connectionStatus || config?.database?.lastErrorMessage) && (
            <div className="bg-gray-50 rounded-lg p-4 space-y-2">
              <h4 className="text-sm font-semibold text-gray-700">수집 상태</h4>
              {config.database?.connectionStatus && (
                <div className="flex items-center gap-2">
                  <span className="text-xs text-gray-500">상태:</span>
                  <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                    config.database.connectionStatus === "IDLE"
                      ? "bg-gray-100 text-gray-700"
                      : config.database.connectionStatus === "ERROR"
                      ? "bg-red-100 text-red-700"
                      : "bg-green-100 text-green-700"
                  }`}>
                    {config.database.connectionStatus}
                  </span>
                </div>
              )}
              {config.database?.lastErrorMessage && (
                <p className="text-xs text-red-600 break-all">{config.database.lastErrorMessage}</p>
              )}
            </div>
          )}
        </div>
      )}

      <div className="pt-4">
        <LoadingButton
          loading={saveMutation.isPending}
          onClick={() => saveMutation.mutate()}
          className="w-full"
        >
          저장
        </LoadingButton>
      </div>
    </div>
  );
}

function LabeledInput({ label, value, onChange, type = "text", textarea = false, placeholder }: { label: string; value: string; onChange: (v: string) => void; type?: string; textarea?: boolean; placeholder?: string; }) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      {textarea ? (
        <textarea className="w-full p-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 text-sm" rows={4} value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} />
      ) : (
        <input className="w-full p-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 text-sm" type={type} value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} />
      )}
    </div>
  );
}

function LabeledCheckbox({ label, checked, onChange }: { label: string; checked: boolean; onChange: (c: boolean) => void; }) {
  return (
    <label className="inline-flex items-center gap-2 text-sm mt-6">
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} />
      {label}
    </label>
  );
}
