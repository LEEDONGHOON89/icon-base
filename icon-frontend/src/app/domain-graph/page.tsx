"use client";

import { useCallback, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import ReactFlow, {
  Node,
  Edge,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  MarkerType,
  Panel,
} from "reactflow";
import "reactflow/dist/style.css";
import {
  AdjustmentsHorizontalIcon,
  ArrowPathIcon,
} from "@heroicons/react/24/outline";
import { fetchDetectionAreas, DetectionArea } from "../domain-settings/api";
import { fetchRelationRules, RelationRule } from "../relation-rules/api";
import Alert from "@/components/common/Alert";

// 노드 색상 매핑
const colorMap: Record<string, string> = {
  blue: "#3B82F6",
  green: "#10B981",
  purple: "#8B5CF6",
  orange: "#F97316",
  red: "#EF4444",
  yellow: "#EAB308",
  gray: "#6B7280",
};

export default function DomainGraphPage() {
  const [filterDataSource, setFilterDataSource] = useState("");
  const [showInactive, setShowInactive] = useState(false);

  // 탐지영역 조회
  const { data: detectionAreas, isLoading: isLoadingAreas } = useQuery({
    queryKey: ["detection-areas"],
    queryFn: fetchDetectionAreas,
  });

  // 관계 규칙 조회
  const {
    data: relationRules,
    isLoading: isLoadingRules,
    error,
  } = useQuery({
    queryKey: ["relation-rules", filterDataSource],
    queryFn: () => fetchRelationRules(filterDataSource || undefined),
  });

  // 필터링된 데이터
  const filteredAreas = useMemo(() => {
    if (!detectionAreas) return [];
    return showInactive
      ? detectionAreas
      : detectionAreas.filter((d) => d.isActive);
  }, [detectionAreas, showInactive]);

  const filteredRules = useMemo(() => {
    if (!relationRules) return [];
    return showInactive
      ? relationRules
      : relationRules.filter((r) => r.isActive);
  }, [relationRules, showInactive]);

  // 노드 생성
  const initialNodes = useMemo((): Node[] => {
    if (!filteredAreas.length) return [];

    // 원형 레이아웃
    const centerX = 400;
    const centerY = 300;
    const radius = 250;

    return filteredAreas.map((area, index) => {
      const angle = (2 * Math.PI * index) / filteredAreas.length;
      const x = centerX + radius * Math.cos(angle);
      const y = centerY + radius * Math.sin(angle);

      return {
        id: area.detectionAreaId,
        type: "default",
        position: { x, y },
        data: {
          label: (
            <div className="text-center leading-tight">
              <div className="font-bold text-sm">{area.areaName}</div>
              <div className="text-xs mt-1 opacity-90">{area.detectionAreaId}</div>
            </div>
          ),
        },
        style: {
          background: colorMap[area.color || "gray"] || colorMap.gray,
          color: "#fff",
          border: "2px solid #fff",
          borderRadius: "50%",
          width: 130,
          height: 130,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: "8px",
          boxShadow: "0 4px 6px rgba(0,0,0,0.1)",
        },
      };
    });
  }, [filteredAreas]);

  // 엣지 생성
  const initialEdges = useMemo((): Edge[] => {
    if (!filteredRules.length) return [];

    return filteredRules.map((rule, index) => ({
      id: `edge-${rule.ruleId}`,
      source: rule.fromEntityType,
      target: rule.toEntityType,
      label: rule.relationType,
      type: "smoothstep",
      animated: true,
      style: {
        stroke: rule.isActive ? "#6B7280" : "#D1D5DB",
        strokeWidth: 2,
      },
      markerEnd: {
        type: MarkerType.ArrowClosed,
        color: rule.isActive ? "#6B7280" : "#D1D5DB",
      },
      labelStyle: {
        fill: "#374151",
        fontSize: 11,
        fontWeight: 500,
      },
      labelBgStyle: {
        fill: "#fff",
        fillOpacity: 0.9,
      },
    }));
  }, [filteredRules]);

  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  // 노드/엣지 업데이트
  useMemo(() => {
    setNodes(initialNodes);
    setEdges(initialEdges);
  }, [initialNodes, initialEdges, setNodes, setEdges]);

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  // 레이아웃 리셋
  const handleResetLayout = () => {
    setNodes(initialNodes);
  };

  // 고유 데이터소스 목록
  const uniqueDataSources = useMemo(() => {
    if (!relationRules) return [];
    return Array.from(new Set(relationRules.map((r) => r.dataSourceId))).sort();
  }, [relationRules]);

  const isLoading = isLoadingAreas || isLoadingRules;

  if (isLoading) {
    return (
      <div className="h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="h-screen flex flex-col">
      {/* Header */}
      <div className="bg-gradient-to-r from-blue-600 to-indigo-600 px-8 py-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-white">탐지영역 관계 그래프</h1>
            <p className="mt-2 text-blue-100">
              탐지영역 간 관계를 시각적으로 확인합니다
            </p>
          </div>

          {/* 통계 */}
          <div className="flex gap-6 text-white">
            <div className="text-center">
              <div className="text-3xl font-bold">{filteredAreas.length}</div>
              <div className="text-sm text-blue-100">탐지영역</div>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold">{filteredRules.length}</div>
              <div className="text-sm text-blue-100">관계</div>
            </div>
          </div>
        </div>
      </div>

      {/* 에러 표시 */}
      {error && (
        <div className="px-8 py-4">
          <Alert message="데이터를 불러오지 못했습니다." />
        </div>
      )}

      {/* 그래프 영역 */}
      <div className="flex-1 relative">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          fitView
          attributionPosition="bottom-left"
        >
          <Background />
          <Controls />

          {/* 필터 패널 */}
          <Panel position="top-right" className="bg-white rounded-lg shadow-lg p-4 space-y-4">
            <div className="flex items-center gap-2 text-gray-700 font-semibold">
              <AdjustmentsHorizontalIcon className="h-5 w-5" />
              필터
            </div>

            {/* 데이터소스 필터 */}
            <div>
              <label className="block text-xs text-gray-600 mb-1">
                데이터소스
              </label>
              <select
                value={filterDataSource}
                onChange={(e) => setFilterDataSource(e.target.value)}
                className="w-full px-3 py-1.5 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="">전체</option>
                {uniqueDataSources.map((ds) => (
                  <option key={ds} value={ds}>
                    {ds}
                  </option>
                ))}
              </select>
            </div>

            {/* 비활성 항목 표시 */}
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={showInactive}
                onChange={(e) => setShowInactive(e.target.checked)}
                className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
              />
              <span className="text-sm text-gray-700">비활성 항목 표시</span>
            </label>

            {/* 레이아웃 리셋 */}
            <button
              onClick={handleResetLayout}
              className="w-full flex items-center justify-center gap-2 px-3 py-2 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
            >
              <ArrowPathIcon className="h-4 w-4" />
              레이아웃 초기화
            </button>
          </Panel>

          {/* 범례 패널 */}
          <Panel position="bottom-right" className="bg-white rounded-lg shadow-lg p-4">
            <div className="text-sm font-semibold text-gray-700 mb-3">범례</div>
            <div className="space-y-2 text-xs text-gray-600">
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 rounded-full bg-blue-500"></div>
                <span>탐지영역</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-8 h-0.5 bg-gray-600"></div>
                <span>활성 관계</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-8 h-0.5 bg-gray-300"></div>
                <span>비활성 관계</span>
              </div>
            </div>
            <div className="mt-3 pt-3 border-t text-xs text-gray-500">
              <div>• 노드를 드래그하여 이동</div>
              <div>• 마우스 휠로 줌</div>
              <div>• 빈 영역을 드래그하여 이동</div>
            </div>
          </Panel>
        </ReactFlow>
      </div>
    </div>
  );
}
