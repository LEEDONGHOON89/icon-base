"use client";

import { useQueryWithErrorHandling } from "@/hooks/useQueryWithErrorHandling";
import {
  ArrowPathIcon,
  CheckCircleIcon,
  ChevronRightIcon,
  ClockIcon,
  ComputerDesktopIcon,
  ExclamationCircleIcon,
  MinusCircleIcon,
  PaperAirplaneIcon,
  ServerStackIcon,
  XCircleIcon,
} from "@heroicons/react/24/outline";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import {
  Agent,
  AgentSession,
  AgentStatus,
  AgentTargetConfig,
  AgentTargetConfigUpdateRequest,
  fetchAgentSessions,
  fetchAgentTargetConfigs,
  fetchAgents,
  pushAgentTargetConfig,
  updateAgentTargetConfig,
} from "./api";

// ── Status badge ──────────────────────────────────────────
const STATUS_META: Record<AgentStatus, { label: string; color: string; icon: React.ElementType }> = {
  ACTIVE:         { label: "연결됨",   color: "bg-emerald-100 text-emerald-700", icon: CheckCircleIcon },
  PENDING_CONFIG: { label: "설정 대기", color: "bg-yellow-100 text-yellow-700",  icon: ExclamationCircleIcon },
  DISCONNECTED:   { label: "연결 끊김", color: "bg-gray-100 text-gray-600",      icon: XCircleIcon },
  INACTIVE:       { label: "비활성",   color: "bg-red-100 text-red-600",         icon: MinusCircleIcon },
};

function StatusBadge({ status }: { status: AgentStatus }) {
  const meta = STATUS_META[status] ?? STATUS_META.INACTIVE;
  const Icon = meta.icon;
  return (
    <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${meta.color}`}>
      <Icon className="h-3.5 w-3.5" />
      {meta.label}
    </span>
  );
}

function fmt(dt: string | null) {
  if (!dt) return "-";
  return new Date(dt).toLocaleString("ko-KR", {
    year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", second: "2-digit",
  });
}

// ── Session history modal ────────────────────────────────
function SessionModal({ agent, onClose }: { agent: Agent; onClose: () => void }) {
  const { data: sessions = [], isLoading } = useQuery({
    queryKey: ["agentSessions", agent.agentId],
    queryFn: () => fetchAgentSessions(agent.agentId, 30),
  });
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[80vh] flex flex-col">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div className="flex items-center gap-2">
            <ClockIcon className="h-5 w-5 text-gray-500" />
            <h2 className="text-base font-semibold text-gray-900">
              세션 이력 — {agent.displayName || agent.agentId}
            </h2>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl font-bold">×</button>
        </div>
        <div className="overflow-y-auto flex-1 px-6 py-4">
          {isLoading ? (
            <p className="text-sm text-gray-500 text-center py-8">로딩 중…</p>
          ) : sessions.length === 0 ? (
            <p className="text-sm text-gray-500 text-center py-8">세션 이력이 없습니다.</p>
          ) : (
            <table className="w-full text-xs">
              <thead>
                <tr className="text-left text-gray-500 border-b border-gray-100">
                  <th className="pb-2 font-medium">상태</th>
                  <th className="pb-2 font-medium">접속 IP</th>
                  <th className="pb-2 font-medium">연결 시각</th>
                  <th className="pb-2 font-medium">해제 시각</th>
                  <th className="pb-2 font-medium">사유</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {sessions.map((s: AgentSession) => (
                  <tr key={s.sessionId} className="hover:bg-gray-50">
                    <td className="py-2 pr-3">
                      <span className={`inline-block px-2 py-0.5 rounded-full font-medium ${
                        s.status === "CONNECTED" ? "bg-emerald-100 text-emerald-700" : "bg-gray-100 text-gray-600"
                      }`}>
                        {s.status === "CONNECTED" ? "연결" : "해제"}
                      </span>
                    </td>
                    <td className="py-2 pr-3 text-gray-700">{s.remoteAddress}</td>
                    <td className="py-2 pr-3 text-gray-600">{fmt(s.connectedAt)}</td>
                    <td className="py-2 pr-3 text-gray-600">{fmt(s.disconnectedAt)}</td>
                    <td className="py-2 text-gray-400">{s.disconnectReason || "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

// ── Target config edit panel ─────────────────────────────
function TargetConfigPanel({ agent, onClose }: { agent: Agent; onClose: () => void }) {
  const queryClient = useQueryClient();

  const { data: configs = [], isLoading } = useQuery({
    queryKey: ["agentTargetConfigs", agent.agentId],
    queryFn: () => fetchAgentTargetConfigs(agent.agentId),
  });

  const [form, setForm] = useState<AgentTargetConfigUpdateRequest | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);

  useEffect(() => {
    if (configs.length > 0 && !editingId) {
      const cfg = configs[0];
      setEditingId(cfg.targetConfigId);
      initForm(cfg);
    }
  }, [configs]);

  const initForm = (cfg: AgentTargetConfig) => {
    setForm({
      rpcEndpoint:          cfg.rpcEndpoint,
      compress:             cfg.compress,
      tlsKeystorePath:      cfg.tlsKeystorePath ?? "",
      tlsKeystorePassword:  "",
      tlsTruststorePath:    cfg.tlsTruststorePath ?? "",
      tlsTruststorePassword:"",
      queueCapacity:        cfg.queueCapacity,
      maxBatchSize:         cfg.maxBatchSize,
      maxBatchMs:           cfg.maxBatchMs,
      maxBatchBytes:        cfg.maxBatchBytes,
      maxBatchesPerSecond:  cfg.maxBatchesPerSecond,
    });
  };

  const saveMutation = useMutation({
    mutationFn: () => {
      // [2026-04-21] 빈 비밀번호를 undefined로 변환 - 빈 문자열로 기존 비밀번호 덮어쓰는 버그 수정
      const payload = {
        ...form!,
        tlsKeystorePassword:  form!.tlsKeystorePassword  || undefined,
        tlsTruststorePassword: form!.tlsTruststorePassword || undefined,
      };
      return updateAgentTargetConfig(agent.agentId, editingId!, payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["agentTargetConfigs", agent.agentId] });
      toast.success("설정이 저장되었습니다.");
    },
    onError: () => toast.error("저장에 실패했습니다."),
  });

  const pushMutation = useMutation({
    mutationFn: () => pushAgentTargetConfig(agent.agentId, editingId!),
    onSuccess: (data) => {
      toast.success(data.pushed ? "에이전트에 적용되었습니다." : "에이전트가 연결되어 있지 않습니다.");
    },
    onError: () => toast.error("적용에 실패했습니다."),
  });

  const handleSaveAndPush = async () => {
    await saveMutation.mutateAsync();
    pushMutation.mutate();
  };

  const field = (
    label: string,
    key: keyof AgentTargetConfigUpdateRequest,
    type = "text",
    disabled = false
  ) => (
    <div>
      <label className="block text-xs font-medium text-gray-600 mb-1">{label}</label>
      {type === "checkbox" ? (
        <input
          type="checkbox"
          checked={!!(form as any)?.[key]}
          onChange={(e) => setForm((f) => f ? { ...f, [key]: e.target.checked } : f)}
          disabled={disabled}
          className="h-4 w-4 rounded border-gray-300 text-indigo-600"
        />
      ) : (
        <input
          type={type}
          value={(form as any)?.[key] ?? ""}
          onChange={(e) => {
            const val = type === "number" ? Number(e.target.value) : e.target.value;
            setForm((f) => f ? { ...f, [key]: val } : f);
          }}
          disabled={disabled}
          className="w-full px-3 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100 disabled:text-gray-500 disabled:cursor-not-allowed"
        />
      )}
    </div>
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-xl max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div>
            <h2 className="text-base font-semibold text-gray-900">타겟 설정</h2>
            <p className="text-xs text-gray-500 mt-0.5">{agent.displayName || agent.agentId}</p>
          </div>
          <div className="flex items-center gap-2">
            <StatusBadge status={agent.status} />
            <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl font-bold ml-2">×</button>
          </div>
        </div>

        {/* Body */}
        <div className="overflow-y-auto flex-1 px-6 py-5">
          {isLoading ? (
            <p className="text-sm text-gray-400 text-center py-8">로딩 중…</p>
          ) : configs.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-8">설정 정보가 없습니다.</p>
          ) : (
            <>
              {/* Config selector */}
              {configs.length > 1 && (
                <div className="mb-4 flex gap-2 flex-wrap">
                  {configs.map((c) => (
                    <button
                      key={c.targetConfigId}
                      onClick={() => { setEditingId(c.targetConfigId); initForm(c); }}
                      className={`px-3 py-1 rounded-lg text-xs font-medium border transition-colors ${
                        editingId === c.targetConfigId
                          ? "bg-indigo-600 text-white border-indigo-600"
                          : "border-gray-300 text-gray-600 hover:bg-gray-50"
                      }`}
                    >
                      {c.targetId}
                    </button>
                  ))}
                </div>
              )}

              {form && (
                <div className="space-y-4">
                  <div className="bg-gray-50 rounded-xl p-4 space-y-3">
                    <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">RPC 연결 설정</p>
                    {field("RPC Endpoint", "rpcEndpoint", "text", true)}
                    <div className="flex items-center gap-3">
                      <label className="text-xs font-medium text-gray-600">GZIP 압축</label>
                      {field("", "compress", "checkbox")}
                    </div>
                  </div>
                  <div className="bg-gray-50 rounded-xl p-4 space-y-3">
                    <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">TLS 설정</p>
                    {field("Keystore 경로", "tlsKeystorePath")}
                    {field("Keystore 비밀번호", "tlsKeystorePassword", "password")}
                    {field("Truststore 경로", "tlsTruststorePath")}
                    {field("Truststore 비밀번호", "tlsTruststorePassword", "password")}
                  </div>
                  <div className="bg-gray-50 rounded-xl p-4 space-y-3">
                    <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">배치 정책</p>
                    <div className="grid grid-cols-2 gap-3">
                      {field("Queue Capacity", "queueCapacity", "number")}
                      {field("Max Batch Size (건)", "maxBatchSize", "number")}
                      {field("Max Batch Ms (ms)", "maxBatchMs", "number")}
                      {field("Max Batch Bytes (0=무제한)", "maxBatchBytes", "number")}
                    </div>
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer */}
        {form && editingId && (
          <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between gap-3">
            <p className="text-xs text-gray-400">
              {agent.status === "ACTIVE"
                ? "에이전트가 연결되어 있습니다. 저장 후 즉시 적용할 수 있습니다."
                : "에이전트가 오프라인입니다. 저장만 됩니다."}
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => saveMutation.mutate()}
                disabled={saveMutation.isPending}
                className="px-4 py-2 text-sm font-medium rounded-lg border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 disabled:opacity-50"
              >
                저장
              </button>
              <button
                onClick={handleSaveAndPush}
                disabled={saveMutation.isPending || pushMutation.isPending}
                className="flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50"
              >
                <PaperAirplaneIcon className="h-4 w-4" />
                저장 후 에이전트 적용
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ── Agent card ───────────────────────────────────────────
function AgentCard({
  agent, isSelected, onSelect, onSessionClick,
}: {
  agent: Agent;
  isSelected: boolean;
  onSelect: (a: Agent) => void;
  onSessionClick: (a: Agent) => void;
}) {
  return (
    <div
      onClick={() => onSelect(agent)}
      className={`bg-white rounded-xl border shadow-sm hover:shadow-md transition-all p-5 flex flex-col gap-3 cursor-pointer ${
        isSelected ? "border-indigo-400 ring-2 ring-indigo-200" : "border-gray-200"
      }`}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-3 min-w-0">
          <div className="w-10 h-10 bg-gradient-to-br from-indigo-500 to-blue-600 rounded-xl flex items-center justify-center shrink-0">
            <ComputerDesktopIcon className="h-5 w-5 text-white" />
          </div>
          <div className="min-w-0">
            <p className="font-semibold text-gray-900 truncate">{agent.displayName || agent.agentId}</p>
            <p className="text-xs text-gray-400 font-mono truncate">{agent.agentId}</p>
          </div>
        </div>
        <StatusBadge status={agent.status} />
      </div>

      <dl className="grid grid-cols-2 gap-x-4 gap-y-1.5 text-xs">
        <div>
          <dt className="text-gray-400">호스트명</dt>
          <dd className="text-gray-700 font-medium truncate">{agent.hostname || "-"}</dd>
        </div>
        <div>
          <dt className="text-gray-400">IP 주소</dt>
          <dd className="text-gray-700 font-medium">{agent.ipAddress || "-"}</dd>
        </div>
        <div>
          <dt className="text-gray-400">버전</dt>
          <dd className="text-gray-700 font-medium">{agent.agentVersion || "-"}</dd>
        </div>
        <div>
          <dt className="text-gray-400">OS</dt>
          <dd className="text-gray-700 font-medium truncate">{agent.osInfo || "-"}</dd>
        </div>
        <div className="col-span-2">
          <dt className="text-gray-400">마지막 연결</dt>
          <dd className="text-gray-700 font-medium">{fmt(agent.lastConnectedAt)}</dd>
        </div>
      </dl>

      <div className="pt-1 border-t border-gray-100 flex items-center justify-between">
        <button
          onClick={(e) => { e.stopPropagation(); onSessionClick(agent); }}
          className="text-xs text-indigo-600 hover:text-indigo-800 font-medium flex items-center gap-1"
        >
          <ClockIcon className="h-3.5 w-3.5" />
          세션 이력
        </button>
        <span className="text-xs text-gray-400 flex items-center gap-1">
          타겟 설정 <ChevronRightIcon className="h-3 w-3" />
        </span>
      </div>
    </div>
  );
}

// ── Main page ────────────────────────────────────────────
export default function AgentsPage() {
  const queryClient = useQueryClient();
  const [sessionAgent, setSessionAgent] = useState<Agent | null>(null);
  const [configAgent, setConfigAgent] = useState<Agent | null>(null);
  const [statusFilter, setStatusFilter] = useState<AgentStatus | "ALL">("ALL");
  const [searchTerm, setSearchTerm] = useState("");

  const { data: agents = [], isLoading } = useQueryWithErrorHandling({
    queryKey: ["agents"],
    queryFn: fetchAgents,
  });

  const handleRefresh = () => {
    queryClient.invalidateQueries({ queryKey: ["agents"] });
    toast.success("새로고침 완료");
  };

  const filtered = agents.filter((a: Agent) => {
    const matchStatus = statusFilter === "ALL" || a.status === statusFilter;
    const q = searchTerm.toLowerCase();
    const matchSearch =
      !q ||
      (a.agentId?.toLowerCase().includes(q) ?? false) ||
      (a.hostname?.toLowerCase().includes(q) ?? false) ||
      (a.ipAddress?.toLowerCase().includes(q) ?? false) ||
      (a.displayName?.toLowerCase().includes(q) ?? false);
    return matchStatus && matchSearch;
  });

  const stats = {
    total:        agents.length,
    active:       agents.filter((a: Agent) => a.status === "ACTIVE").length,
    disconnected: agents.filter((a: Agent) => a.status === "DISCONNECTED").length,
    pending:      agents.filter((a: Agent) => a.status === "PENDING_CONFIG").length,
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-gradient-to-br from-indigo-500 to-blue-600 rounded-xl flex items-center justify-center">
            <ServerStackIcon className="h-6 w-6 text-white" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">에이전트 관리</h1>
            <p className="text-sm text-gray-500">연결된 수집 에이전트 목록 및 설정을 관리합니다.</p>
          </div>
        </div>
        <button
          onClick={handleRefresh}
          className="flex items-center gap-2 px-4 py-2 rounded-lg border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50 shadow-sm"
        >
          <ArrowPathIcon className="h-4 w-4" />
          새로고침
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {[
          { label: "전체",     value: stats.total,        color: "from-blue-500 to-blue-600" },
          { label: "연결됨",   value: stats.active,       color: "from-emerald-500 to-emerald-600" },
          { label: "연결 끊김", value: stats.disconnected, color: "from-gray-400 to-gray-500" },
          { label: "설정 대기", value: stats.pending,      color: "from-yellow-400 to-yellow-500" },
        ].map((s) => (
          <div key={s.label} className="bg-white rounded-xl border border-gray-200 shadow-sm p-4">
            <p className="text-xs text-gray-500 mb-1">{s.label}</p>
            <p className={`text-2xl font-bold bg-gradient-to-r ${s.color} bg-clip-text text-transparent`}>
              {s.value}
            </p>
          </div>
        ))}
      </div>

      {/* Search / filter */}
      <div className="flex flex-col sm:flex-row gap-3">
        <input
          type="text"
          placeholder="에이전트 ID, 호스트명, IP 검색…"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="flex-1 px-4 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        <div className="flex gap-2 flex-wrap">
          {(["ALL", "ACTIVE", "DISCONNECTED", "PENDING_CONFIG", "INACTIVE"] as const).map((s) => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                statusFilter === s
                  ? "bg-indigo-600 text-white"
                  : "bg-white border border-gray-300 text-gray-600 hover:bg-gray-50"
              }`}
            >
              {s === "ALL" ? "전체" : STATUS_META[s as AgentStatus]?.label ?? s}
            </button>
          ))}
        </div>
      </div>

      {/* List */}
      {isLoading ? (
        <div className="text-center py-16 text-gray-400 text-sm">로딩 중…</div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <ServerStackIcon className="h-12 w-12 mx-auto mb-3 opacity-40" />
          <p className="text-sm">
            {agents.length === 0 ? "아직 연결된 에이전트가 없습니다." : "검색 조건에 맞는 에이전트가 없습니다."}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
          {filtered.map((agent: Agent) => (
            <AgentCard
              key={agent.agentId}
              agent={agent}
              isSelected={configAgent?.agentId === agent.agentId}
              onSelect={setConfigAgent}
              onSessionClick={setSessionAgent}
            />
          ))}
        </div>
      )}

      {/* Modals */}
      {sessionAgent && (
        <SessionModal agent={sessionAgent} onClose={() => setSessionAgent(null)} />
      )}
      {configAgent && (
        <TargetConfigPanel agent={configAgent} onClose={() => setConfigAgent(null)} />
      )}
    </div>
  );
}
