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

// ── Collectors (에이전트 데이터소스) ────────────────────────
export interface CollectorFileDetail {
  directory: string;
  fileNamePattern: string;
  fileFormat: string;
  csvHasHeader: boolean;
  csvDelimiter: string;
  csvColumns: string | null;
  charset: string;
}

export interface CollectorJdbcDetail {
  url: string;
  username: string;
  password: string;
  query: string;
  field1: string;
  field1Type: string;
  field1InitialValue: string | null;
  field2: string | null;
  field2Type: string | null;
  field2InitialValue: string | null;
}

export interface AgentCollector {
  collectorConfigId: string;
  targetConfigId: string;
  collectorType: "FILE" | "JDBC";
  name: string;
  enabled: boolean;
  pollIntervalMs: number;
  maxLinesPerPoll: number;
  maxRecordBytes: number;
  createdAt: string | null;
  updatedAt: string | null;
  fileDetail?: CollectorFileDetail | null;
  jdbcDetail?: CollectorJdbcDetail | null;
}

export interface AgentCollectorCreateRequest {
  collectorType: "FILE" | "JDBC";
  name: string;
  enabled?: boolean;
  pollIntervalMs?: number;
  maxLinesPerPoll?: number;
  maxRecordBytes?: number;
  directory?: string;
  fileNamePattern?: string;
  fileFormat?: string;
  csvHasHeader?: boolean;
  csvDelimiter?: string;
  csvColumns?: string;
  charset?: string;
  url?: string;
  username?: string;
  password?: string;
  query?: string;
  field1?: string;
  field1Type?: string;
  field1InitialValue?: string;
  field2?: string;
  field2Type?: string;
  field2InitialValue?: string;
}

export interface AgentCollectorUpdateRequest {
  name?: string;
  enabled?: boolean;
  pollIntervalMs?: number;
  maxLinesPerPoll?: number;
  maxRecordBytes?: number;
  directory?: string;
  fileNamePattern?: string;
  fileFormat?: string;
  csvHasHeader?: boolean;
  csvDelimiter?: string;
  csvColumns?: string;
  charset?: string;
  url?: string;
  username?: string;
  password?: string;
  query?: string;
  field1?: string;
  field1Type?: string;
  field1InitialValue?: string;
  field2?: string;
  field2Type?: string;
  field2InitialValue?: string;
}

export const fetchAgentCollectors = async (
  agentId: string,
  targetConfigId: string
): Promise<AgentCollector[]> => {
  const res = await api.get(
    `/api/rpc/agents/${agentId}/target-configs/${targetConfigId}/collectors`
  );
  return res.data;
};

export const createAgentCollector = async (
  agentId: string,
  targetConfigId: string,
  req: AgentCollectorCreateRequest
): Promise<AgentCollector> => {
  const res = await api.post(
    `/api/rpc/agents/${agentId}/target-configs/${targetConfigId}/collectors`,
    req
  );
  return res.data;
};

export const updateAgentCollector = async (
  agentId: string,
  targetConfigId: string,
  collectorConfigId: string,
  req: AgentCollectorUpdateRequest
): Promise<AgentCollector> => {
  const res = await api.put(
    `/api/rpc/agents/${agentId}/target-configs/${targetConfigId}/collectors/${collectorConfigId}`,
    req
  );
  return res.data;
};

export const deleteAgentCollector = async (
  agentId: string,
  targetConfigId: string,
  collectorConfigId: string
): Promise<void> => {
  await api.delete(
    `/api/rpc/agents/${agentId}/target-configs/${targetConfigId}/collectors/${collectorConfigId}`
  );
};
