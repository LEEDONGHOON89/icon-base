"use client";

import { useMemo, useRef } from "react";
import CytoscapeComponent from "react-cytoscapejs";
import cytoscape from "cytoscape";
import { GraphNode, GraphEdge } from "@/services/analytics.service";

interface EntityRelationshipGraphProps {
  nodes: GraphNode[];
  edges: GraphEdge[];
  rootEntityId?: string;
}

// 엔티티 타입별 색상 매핑
const entityTypeColors: Record<string, { bg: string; border: string }> = {
  EMPLOYEE: { bg: "#1e293b", border: "#0f172a" }, // slate-800/900
  ACCOUNT: { bg: "#3b82f6", border: "#2563eb" }, // blue
  CUSTOMER: { bg: "#10b981", border: "#059669" }, // green
  DEVICE: { bg: "#f59e0b", border: "#d97706" }, // amber
  TRANSACTION: { bg: "#8b5cf6", border: "#7c3aed" }, // violet
  DEFAULT: { bg: "#6b7280", border: "#4b5563" }, // gray
};

// 관계 타입별 색상
const relationTypeColors: Record<string, string> = {
  OWNS: "#8b5cf6", // violet
  FAMILY_ACCOUNT: "#ec4899", // pink
  TRANSFERS_TO: "#f59e0b", // amber
  FAMILY: "#ec4899", // pink
  DEFAULT: "#64748b", // gray
};

export default function EntityRelationshipGraph({
  nodes: graphNodes,
  edges: graphEdges,
  rootEntityId,
}: EntityRelationshipGraphProps) {
  const cyRef = useRef<cytoscape.Core | null>(null);
  const initialZoomRef = useRef<number | null>(null);

  // 관계 타입별로 엣지를 그룹화하는 함수
  const groupEdgesByRelation = (
    nodes: GraphNode[],
    edges: GraphEdge[]
  ): { nodes: any[]; edges: any[] } => {
    // 소스별, 관계타입별로 엣지 그룹화
    const edgeGroups = new Map<string, GraphEdge[]>();

    edges.forEach((edge) => {
      const key = `${edge.source}::${edge.type || edge.label}`;
      if (!edgeGroups.has(key)) {
        edgeGroups.set(key, []);
      }
      edgeGroups.get(key)!.push(edge);
    });

    const newNodes: any[] = [];
    const newEdges: any[] = [];

    // 기존 노드들 추가
    nodes.forEach((node) => {
      newNodes.push({
        data: {
          id: node.id,
          label: node.label,
          type: node.type,
          isEntity: true,
        },
      });
    });

    // 그룹화된 엣지 처리
    edgeGroups.forEach((groupEdges, key) => {
      if (groupEdges.length === 1) {
        // 엣지가 1개면 그냥 직접 연결
        const edge = groupEdges[0];
        newEdges.push({
          data: {
            id: edge.id,
            source: edge.source,
            target: edge.target,
            label: edge.label,
            type: edge.type,
          },
        });
      } else {
        // 엣지가 여러 개면 중간에 그룹 노드 추가
        const relationType = groupEdges[0].type || groupEdges[0].label;
        const sourceId = groupEdges[0].source;
        const groupNodeId = `group-${sourceId}-${relationType}`;

        // 그룹 노드 추가
        newNodes.push({
          data: {
            id: groupNodeId,
            label: `${relationType}\n(${groupEdges.length})`,
            type: relationType,
            isGroup: true,
            count: groupEdges.length,
          },
        });

        // 소스 → 그룹 노드
        newEdges.push({
          data: {
            id: `edge-${sourceId}-${groupNodeId}`,
            source: sourceId,
            target: groupNodeId,
            label: "",
            type: relationType,
            isGroupEdge: true,
          },
        });

        // 그룹 노드 → 각 타겟들
        groupEdges.forEach((edge, idx) => {
          newEdges.push({
            data: {
              id: `${groupNodeId}-${edge.target}-${idx}`,
              source: groupNodeId,
              target: edge.target,
              label: "",
              type: relationType,
              isGroupEdge: true,
            },
          });
        });
      }
    });

    return { nodes: newNodes, edges: newEdges };
  };

  // Cytoscape 엘리먼트로 변환 (그룹화 적용)
  const elements = useMemo(() => {
    const { nodes, edges } = groupEdgesByRelation(graphNodes, graphEdges);
    return [...nodes, ...edges];
  }, [graphNodes, graphEdges]);

  // 루트 노드 ID 찾기
  const rootNodeId = useMemo(() => {
    if (rootEntityId) {
      const rootNode = graphNodes.find(
        (n) => n.label === rootEntityId || n.id.includes(rootEntityId)
      );
      return rootNode?.id;
    }
    return graphNodes[0]?.id;
  }, [graphNodes, rootEntityId]);

  // 레이아웃 설정
  const layout = {
    name: "breadthfirst",
    directed: true,
    roots: rootNodeId ? [rootNodeId] : undefined,
    padding: 30,
    spacingFactor: 1.2,
    avoidOverlap: true,
    nodeDimensionsIncludeLabels: true,
    animate: false,
  };

  // 스타일시트 정의
  const stylesheet = [
    // 엔티티 노드 스타일 (미니멀)
    {
      selector: "node[isEntity]",
      style: {
        label: "data(label)" as any,
        "text-valign": "center" as any,
        "text-halign": "center" as any,
        "background-color": entityTypeColors.DEFAULT.bg,
        "border-width": 2,
        "border-color": entityTypeColors.DEFAULT.border,
        width: 80,
        height: 36,
        "font-size": 10,
        "font-weight": "bold",
        color: "#ffffff",
        "text-wrap": "ellipsis" as any,
        "text-max-width": 70,
        shape: "roundrectangle" as any,
      },
    },
    // 그룹 노드 스타일 (더 작고 투명)
    {
      selector: "node[isGroup]",
      style: {
        label: "data(label)" as any,
        "text-valign": "center" as any,
        "text-halign": "center" as any,
        "background-color": "#e0e7ff",
        "border-width": 1.5,
        "border-color": "#818cf8",
        "border-style": "dashed" as any,
        width: 70,
        height: 32,
        "font-size": 9,
        "font-weight": "600",
        color: "#4338ca",
        "text-wrap": "wrap" as any,
        "text-max-width": 60,
        shape: "ellipse" as any,
        opacity: 0.9,
      },
    },
    // 엔티티 타입별 색상
    ...Object.entries(entityTypeColors).map(([type, colors]) => ({
      selector: `node[isEntity][type = "${type}"]`,
      style: {
        "background-color": colors.bg,
        "border-color": colors.border,
      },
    })),
    // 그룹 노드 타입별 색상
    {
      selector: 'node[isGroup][type = "OWNS"]',
      style: {
        "background-color": "#f3e8ff",
        "border-color": "#a78bfa",
        color: "#6b21a8",
      },
    },
    {
      selector: 'node[isGroup][type = "FAMILY_ACCOUNT"]',
      style: {
        "background-color": "#fce7f3",
        "border-color": "#f9a8d4",
        color: "#9f1239",
      },
    },
    // 엣지 기본 스타일 (매우 얇게)
    {
      selector: "edge",
      style: {
        width: 1.5,
        "line-color": relationTypeColors.DEFAULT,
        "target-arrow-color": relationTypeColors.DEFAULT,
        "target-arrow-shape": "triangle" as any,
        "arrow-scale": 0.7,
        "curve-style": "bezier" as any,
        label: "data(label)" as any,
        "font-size": 9,
        "font-weight": "600",
        color: "#64748b",
        "text-background-color": "#ffffff",
        "text-background-opacity": 0.85,
        "text-background-padding": "2px",
        "text-border-width": 0,
      },
    },
    // 그룹 엣지는 더 얇게
    {
      selector: "edge[isGroupEdge]",
      style: {
        width: 1,
        opacity: 0.7,
      },
    },
    // 관계 타입별 색상
    ...Object.entries(relationTypeColors).map(([type, color]) => ({
      selector: `edge[type = "${type}"]`,
      style: {
        "line-color": color,
        "target-arrow-color": color,
      },
    })),
    // 선택된 노드
    {
      selector: "node:selected",
      style: {
        "border-width": 3,
        "border-color": "#ef4444",
      },
    },
    // 호버 효과
    {
      selector: "node:active",
      style: {
        "overlay-color": "#3b82f6",
        "overlay-padding": 6,
        "overlay-opacity": 0.25,
      },
    },
  ];

  if (graphNodes.length === 0) {
    return (
      <div className="h-[600px] flex items-center justify-center bg-gradient-to-br from-slate-50 to-blue-50 rounded-3xl border border-gray-200">
        <div className="text-center">
          <p className="text-gray-500 text-lg">관계 데이터가 없습니다</p>
          <p className="text-gray-400 text-sm mt-2">
            이 엔티티와 연결된 다른 엔티티가 없습니다
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="h-[600px] bg-gradient-to-br from-slate-50 to-blue-50 rounded-3xl border border-gray-200 overflow-hidden relative">
      <CytoscapeComponent
        elements={elements}
        layout={layout}
        stylesheet={stylesheet}
        style={{ width: "100%", height: "100%" }}
        cy={(cy) => {
          cyRef.current = cy;

          // 노드 클릭 이벤트
          cy.on("tap", "node", (evt) => {
            const node = evt.target;
            console.log("Clicked node:", node.data());
          });

          // 초기 뷰 조정 및 zoom level 저장
          setTimeout(() => {
            cy.fit(undefined, 40);
            cy.center();
            initialZoomRef.current = cy.zoom(); // 초기 zoom level 저장
          }, 100);
        }}
      />

      {/* 범례 */}
      <div className="absolute bottom-4 left-4 bg-white/95 backdrop-blur-sm rounded-lg shadow-lg p-3 border border-gray-200">
        <div className="text-xs font-semibold text-gray-700 mb-2">엔티티 타입</div>
        <div className="space-y-1.5">
          {Object.entries(entityTypeColors)
            .filter(([type]) => type !== "DEFAULT")
            .map(([type, colors]) => (
              <div key={type} className="flex items-center gap-2">
                <div
                  className="w-3 h-3 rounded"
                  style={{
                    backgroundColor: colors.bg,
                    border: `1.5px solid ${colors.border}`,
                  }}
                />
                <span className="text-xs text-gray-600">{type}</span>
              </div>
            ))}
        </div>
        <div className="text-xs font-semibold text-gray-700 mt-3 mb-2">관계 타입</div>
        <div className="space-y-1.5">
          {Object.entries(relationTypeColors)
            .filter(([type]) => type !== "DEFAULT")
            .map(([type, color]) => (
              <div key={type} className="flex items-center gap-2">
                <div
                  className="w-6 h-0.5"
                  style={{ backgroundColor: color }}
                />
                <span className="text-xs text-gray-600">{type}</span>
              </div>
            ))}
        </div>
      </div>

      {/* 컨트롤 버튼 */}
      <div className="absolute top-4 right-4 flex flex-col gap-2">
        <button
          onClick={() => cyRef.current?.fit(undefined, 40)}
          className="bg-white/95 backdrop-blur-sm hover:bg-white px-3 py-1.5 rounded-lg shadow-lg border border-gray-200 text-xs font-medium text-gray-700 transition-colors"
        >
          Fit View
        </button>
        <button
          onClick={() => cyRef.current?.center()}
          className="bg-white/95 backdrop-blur-sm hover:bg-white px-3 py-1.5 rounded-lg shadow-lg border border-gray-200 text-xs font-medium text-gray-700 transition-colors"
        >
          Center
        </button>
        <button
          onClick={() => {
            if (cyRef.current) {
              // 새로운 layout 객체 생성 (memoized 버전 대신)
              const freshLayout = {
                name: "breadthfirst",
                directed: true,
                roots: rootNodeId ? [rootNodeId] : undefined,
                padding: 30,
                spacingFactor: 1.2,
                avoidOverlap: true,
                nodeDimensionsIncludeLabels: true,
                animate: false,
              };

              // 새 레이아웃으로 재실행
              cyRef.current.layout(freshLayout as any).run();

              // 초기 zoom level로 복원 (크기 유지)
              setTimeout(() => {
                if (cyRef.current && initialZoomRef.current !== null) {
                  cyRef.current.zoom(initialZoomRef.current);
                  cyRef.current.center();
                }
              }, 100);
            }
          }}
          className="bg-white/95 backdrop-blur-sm hover:bg-white px-3 py-1.5 rounded-lg shadow-lg border border-gray-200 text-xs font-medium text-gray-700 transition-colors"
        >
          Re-layout
        </button>
      </div>
    </div>
  );
}
