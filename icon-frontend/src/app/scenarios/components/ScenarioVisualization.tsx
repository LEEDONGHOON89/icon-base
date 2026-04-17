"use client";

import { useState, useMemo, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import ReactFlow, {
  Node,
  Edge,
  Controls,
  Background,
  BackgroundVariant,
  useNodesState,
  useEdgesState,
  MarkerType,
  Position,
} from "reactflow";
import "reactflow/dist/style.css";
import { ChevronDownIcon, ChevronUpIcon } from "@heroicons/react/24/outline";
import { fetchScenarioVisualization } from "../api";

interface ScenarioVisualizationProps {
  scenarioId: string;
}

// 커스텀 노드 스타일
const nodeStyles = {
  scenario: {
    background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
    color: "white",
    border: "none",
    borderRadius: "16px",
    padding: "16px 24px",
    fontSize: "16px",
    fontWeight: "bold",
    boxShadow: "0 8px 16px rgba(102, 126, 234, 0.3)",
    minWidth: "200px",
  },
  rule: {
    background: "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)",
    color: "white",
    border: "none",
    borderRadius: "12px",
    padding: "12px 20px",
    fontSize: "14px",
    fontWeight: "600",
    boxShadow: "0 4px 12px rgba(245, 87, 108, 0.3)",
    minWidth: "180px",
  },
  sensor: {
    background: "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)",
    color: "white",
    border: "none",
    borderRadius: "10px",
    padding: "10px 16px",
    fontSize: "13px",
    fontWeight: "500",
    boxShadow: "0 4px 10px rgba(79, 172, 254, 0.3)",
    minWidth: "150px",
  },
};

export default function ScenarioVisualization({
  scenarioId,
}: ScenarioVisualizationProps) {
  const [isExpanded, setIsExpanded] = useState(true);

  // 시나리오 시각화 데이터 조회
  const { data: visualization, isLoading, error } = useQuery({
    queryKey: ["scenarioVisualization", scenarioId],
    queryFn: () => fetchScenarioVisualization(scenarioId),
    enabled: !!scenarioId,
  });

  // 디버깅 로그
  console.log("🎯 [ScenarioVisualization]", {
    scenarioId,
    isLoading,
    error,
    visualization,
    hasRules: visualization?.rules?.length,
  });

  // 노드와 엣지 생성
  const { nodes: initialNodes, edges: initialEdges } = useMemo(() => {
    if (!visualization) return { nodes: [], edges: [] };

    const nodes: Node[] = [];
    const edges: Edge[] = [];

    // 1. 시나리오 노드 (최상단 중앙)
    nodes.push({
      id: "scenario",
      type: "default",
      data: {
        label: (
          <div className="text-center">
            <div className="font-bold text-lg mb-1">{visualization.scenarioName}</div>
            <div className="text-xs opacity-90">{visualization.scenarioId}</div>
          </div>
        ),
      },
      position: { x: 400, y: 50 },
      style: nodeStyles.scenario,
      sourcePosition: Position.Bottom,
      targetPosition: Position.Top,
    });

    // 2. 룰 노드들 (세로로 배치)
    const rules = visualization.rules || [];
    const ruleSpacing = 150;
    const ruleStartY = 200;

    rules.forEach((rule, index) => {
      const ruleId = `rule-${rule.ruleId}`;
      const operator = index > 0 ? rule.scenarioOperator || "AND" : null;

      nodes.push({
        id: ruleId,
        type: "default",
        data: {
          label: (
            <div className="text-center">
              <div className="font-semibold">{rule.ruleName}</div>
              <div className="text-xs opacity-90 mt-1">{rule.ruleId}</div>
              {rule.operator && (
                <div className="text-xs opacity-80 mt-1">
                  {rule.operator}
                  {rule.windowMinutes && ` · ${rule.windowMinutes}분`}
                </div>
              )}
            </div>
          ),
        },
        position: { x: 400, y: ruleStartY + index * ruleSpacing },
        style: nodeStyles.rule,
        sourcePosition: Position.Right,
        targetPosition: Position.Top,
      });

      // 시나리오 → 룰 엣지 (수직)
      edges.push({
        id: `scenario-to-${ruleId}`,
        source: "scenario",
        target: ruleId,
        label: operator || undefined,
        labelStyle: {
          fill: "#6366f1",
          fontWeight: 600,
          fontSize: 12,
        },
        labelBgStyle: {
          fill: "#ffffff",
          fillOpacity: 0.9,
        },
        style: { stroke: "#8b5cf6", strokeWidth: 2 },
        markerEnd: {
          type: MarkerType.ArrowClosed,
          color: "#8b5cf6",
        },
        animated: true,
      });

      // 3. 센서 노드 (각 룰 오른쪽에 같은 레벨로)
      if (rule.predicateSensorId) {
        const sensorId = `sensor-${rule.predicateSensorId}`;

        // 센서 노드가 아직 없으면 추가
        if (!nodes.find(n => n.id === sensorId)) {
          nodes.push({
            id: sensorId,
            type: "default",
            data: {
              label: (
                <div className="text-center">
                  <div className="font-medium">{rule.predicateSensorName || rule.predicateSensorId}</div>
                  <div className="text-xs opacity-80 mt-1">{rule.predicateSensorId}</div>
                </div>
              ),
            },
            position: { x: 700, y: ruleStartY + index * ruleSpacing },
            style: nodeStyles.sensor,
            sourcePosition: Position.Right,
            targetPosition: Position.Left,
          });

          // 룰 → 센서 엣지 (수평)
          edges.push({
            id: `${ruleId}-to-${sensorId}`,
            source: ruleId,
            target: sensorId,
            style: { stroke: "#06b6d4", strokeWidth: 2 },
            markerEnd: {
              type: MarkerType.ArrowClosed,
              color: "#06b6d4",
            },
          });
        }
      }
    });

    return { nodes, edges };
  }, [visualization]);

  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  // initialNodes/Edges 변경 시 업데이트
  useEffect(() => {
    setNodes(initialNodes);
    setEdges(initialEdges);
  }, [initialNodes, initialEdges, setNodes, setEdges]);

  if (isLoading) {
    return (
      <div className="bg-white rounded-3xl shadow-xl p-8">
        <div className="flex justify-center items-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white rounded-3xl shadow-xl p-8">
        <div className="text-center py-12">
          <p className="text-red-600">시각화 데이터를 불러오는데 실패했습니다.</p>
          <p className="text-sm text-gray-500 mt-2">{String(error)}</p>
        </div>
      </div>
    );
  }

  if (!visualization || !visualization.rules || visualization.rules.length === 0) {
    return (
      <div className="bg-white rounded-3xl shadow-xl p-8">
        <div className="text-center py-12">
          <p className="text-gray-500">시나리오에 포함된 룰이 없습니다.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-3xl shadow-xl overflow-hidden">
      {/* 헤더 (접기/펼치기) */}
      <button
        type="button"
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full px-8 py-4 flex items-center justify-between hover:bg-gray-50 transition-colors"
      >
        <div className="flex items-center gap-3">
          <div className="text-xl font-bold text-gray-800">🎯 룰 구성 시각화</div>
          <span className="px-3 py-1 bg-blue-100 text-blue-700 rounded-full text-xs font-semibold">
            {visualization.rules.length}개 룰
          </span>
        </div>
        {isExpanded ? (
          <ChevronUpIcon className="h-6 w-6 text-gray-600" />
        ) : (
          <ChevronDownIcon className="h-6 w-6 text-gray-600" />
        )}
      </button>

      {/* 그래프 영역 */}
      {isExpanded && (
        <div className="border-t border-gray-200">
          <div className="h-[500px] bg-gradient-to-br from-slate-50 to-blue-50">
            <ReactFlow
              nodes={nodes}
              edges={edges}
              onNodesChange={onNodesChange}
              onEdgesChange={onEdgesChange}
              fitView
              attributionPosition="bottom-left"
              defaultViewport={{ x: 0, y: 0, zoom: 0.8 }}
              minZoom={0.5}
              maxZoom={1.5}
              nodesDraggable={false}
              nodesConnectable={false}
              elementsSelectable={true}
            >
              <Background variant={BackgroundVariant.Dots} gap={16} size={1} color="#cbd5e1" />
              <Controls showInteractive={false} />
            </ReactFlow>
          </div>

          {/* 범례 */}
          <div className="px-8 py-4 bg-gray-50 border-t border-gray-200">
            <div className="flex items-center gap-6 text-sm">
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 rounded" style={{ background: nodeStyles.scenario.background }}></div>
                <span className="text-gray-700">시나리오</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 rounded" style={{ background: nodeStyles.rule.background }}></div>
                <span className="text-gray-700">룰 (Aggregate)</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 rounded" style={{ background: nodeStyles.sensor.background }}></div>
                <span className="text-gray-700">센서 (Rule)</span>
              </div>
              <div className="flex items-center gap-2 ml-auto">
                <span className="text-gray-500 text-xs">
                  💡 팁: 마우스 휠로 확대/축소, 드래그로 이동 가능
                </span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
