import api from "@/lib/api";

// 타입 정의
export interface Message {
  id: number;
  role: "user" | "assistant";
  content: string;
  createdAt: string;
}

export interface SessionSummary {
  id: number;
  sessionId: string;
  title: string;
  messageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface SessionDetail {
  id: number;
  sessionId: string;
  title: string;
  messages: Message[];
  createdAt: string;
  updatedAt: string;
}

export interface SendMessageRequest {
  sessionId?: string;
  message: string;
}

export interface SendMessageResponse {
  sessionId: string;
  response: string;
  timestamp: string;
  isError?: boolean;
}

// API 함수
export async function sendMessage(request: SendMessageRequest): Promise<SendMessageResponse> {
  const response = await api.post<SendMessageResponse>("/api/v1/ai-chat/message", request, {
    timeout: 600000, // 10분 (AI 응답 대기 시간)
  });
  return response.data;
}

export async function getSessions(): Promise<SessionSummary[]> {
  const response = await api.get<SessionSummary[]>("/api/v1/ai-chat/sessions");
  return response.data;
}

export async function getSessionDetail(sessionId: string): Promise<SessionDetail> {
  const response = await api.get<SessionDetail>(`/api/v1/ai-chat/sessions/${sessionId}`);
  return response.data;
}

export async function createSession(): Promise<SessionSummary> {
  const response = await api.post<SessionSummary>("/api/v1/ai-chat/sessions");
  return response.data;
}

export async function updateSessionTitle(sessionId: string, title: string): Promise<void> {
  await api.patch(`/api/v1/ai-chat/sessions/${sessionId}/title`, { title });
}

export async function deleteSession(sessionId: string): Promise<void> {
  await api.delete(`/api/v1/ai-chat/sessions/${sessionId}`);
}

// AI 학습 관련 타입 및 API
export interface RuleSnapshotResponse {
  success: boolean;
  message: string;
  ruleCount: number;
}

export async function sendRuleSnapshot(): Promise<RuleSnapshotResponse> {
  const response = await api.post<RuleSnapshotResponse>("/api/v1/ai/rule-snapshot/send");
  return response.data;
}
