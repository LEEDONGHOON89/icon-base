import api from "@/lib/api";

export type AgentStatus = "PENDING_CONFIG" | "ACTIVE" | "DISCONNECTED" | "INACTIVE";

export interface Agent {
  agentId: string;
  hostname: string;
  ipAddress: string;
  agentVersion: string;
  osInfo: string;
  displayName: string;
  description: string | null;
  status: AgentStatus;
  firstConnectedAt: string | null;
  lastConnectedAt: string | null;
  lastDisconnectedAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface AgentSession {
  sessionId: string;
  agentId: string;
  remoteAddress: string;
  agentVersion: string;
  status: string;
  connectedAt: string;
  lastHeartbeatAt: string | null;
  disconnectedAt: string | null;
  disconnectReason: string | null;
}

export interface AgentUpdateRequest {
  displayName?: string;
  description?: string;
}

export const fetchAgents = async (): Promise<Agent[]> => {
  const res = await api.get("/api/rpc/agents");
  return res.data;
};

export const fetchAgent = async (agentId: string): Promise<Agent> => {
  const res = await api.get(`/api/rpc/agents/${agentId}`);
  return res.data;
};

export const fetchAgentSessions = async (
  agentId: string,
  limit = 20
): Promise<AgentSession[]> => {
  const res = await api.get(`/api/rpc/agents/${agentId}/sessions`, {
    params: { limit },
  });
  return res.data;
};

export const updateAgent = async (
  agentId: string,
  req: AgentUpdateRequest
): Promise<Agent> => {
  const res = await api.patch(`/api/rpc/agents/${agentId}`, req);
  return res.data;
};

// [2026-04-24] 에이전트 삭제 — DISCONNECTED / INACTIVE / PENDING_CONFIG 상태만 허용
export const deleteAgent = async (agentId: string): Promise<void> => {
  await api.delete(`/api/rpc/agents/${agentId}`);
};

// ── Target Config ──────────────────────────────────────────
export interface AgentTargetConfig {
  targetConfigId: string;
  agentId: string;
  targetId: string;
  rpcEndpoint: string;
  compress: boolean;
  tlsKeystorePath: string | null;
  tlsTruststorePath: string | null;
  queueCapacity: number;
  maxBatchSize: number;
  maxBatchMs: number;
  maxBatchBytes: number;
  // [2026-04-22] 초당 최대 배치 전송 수
  maxBatchesPerSecond: number;
  isActive: boolean;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface AgentTargetConfigUpdateRequest {
  rpcEndpoint: string;
  compress: boolean;
  tlsKeystorePath?: string;
  tlsKeystorePassword?: string;
  tlsTruststorePath?: string;
  tlsTruststorePassword?: string;
  queueCapacity: number;
  maxBatchSize: number;
  maxBatchMs: number;
  maxBatchBytes: number;
  // [2026-04-22] 초당 최대 배치 전송 수
  maxBatchesPerSecond: number;
}

export const fetchAgentTargetConfigs = async (
  agentId: string
): Promise<AgentTargetConfig[]> => {
  const res = await api.get(`/api/rpc/agents/${agentId}/target-configs`);
  return res.data;
};

export const updateAgentTargetConfig = async (
  agentId: string,
  targetConfigId: string,
  req: AgentTargetConfigUpdateRequest
): Promise<AgentTargetConfig> => {
  const res = await api.put(
    `/api/rpc/agents/${agentId}/target-configs/${targetConfigId}`,
    req
  );
  return res.data;
};

export const pushAgentTargetConfig = async (
  agentId: string,
  targetConfigId: string
): Promise<{ pushed: boolean; message: string }> => {
  const res = await api.post(
    `/api/rpc/agents/${agentId}/target-configs/${targetConfigId}/push`
  );
  return res.data;
};

// ── Snapshot (Phase 3) ─────────────────────────────────────
// [2026-04-21] 에이전트 스냅샷 — ds_file_system_config / ds_database_config 기반

export interface AgentSnapshotItem {
  id: string;
  type: "FILE" | "JDBC";
  name: string;
  enabled: boolean;
  pollIntervalMs: number;
  maxLinesPerPoll: number;
  maxRecordBytes: number;
  // FILE 수집기 필드
  path?: string;
  file?: string;
  format?: string;
  csvHasHeader?: boolean;
  csvDelimiter?: string;
  charset?: string;
  // JDBC 수집기 필드
  url?: string;
  username?: string;
  query?: string;
  field1?: string;
  field1_type?: string;
  field1_value?: string;
}

export interface AgentSyncResult {
  agentId: string;
  connected: boolean;
  snapshotSize: number;
  status: "pushed" | "queued";
}

/**
 * [2026-04-21] 에이전트 수집기 스냅샷 즉시 동기화.
 * ds_file_system_config + ds_database_config → COLLECTORS_SYNC 전송
 */
export const syncAgent = async (agentId: string): Promise<AgentSyncResult> => {
  const res = await api.post<AgentSyncResult>(`/api/v1/agents/${agentId}/sync`);
  return res.data;
};

/**
 * [2026-04-21] 에이전트 수집기 스냅샷 조회 (전송 없이 목록만).
 */
export const fetchAgentSnapshot = async (agentId: string): Promise<AgentSnapshotItem[]> => {
  const res = await api.get<AgentSnapshotItem[]>(`/api/v1/agents/${agentId}/snapshot`);
  return res.data;
};
