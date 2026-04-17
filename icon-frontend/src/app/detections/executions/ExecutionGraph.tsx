"use client";

import { useMemo } from "react";
import ReactFlow, {
  Background,
  Edge,
  Handle,
  Node,
  NodeProps,
  Position,
} from "reactflow";
import "reactflow/dist/style.css";
import type {
  DetectedAggregate,
  DetectedRule,
  DetectedScenario,
  ExecutionDetail,
} from "../api";

const NODE_STYLE: Record<GraphNodeType, string> = {
  exec: "bg-indigo-600 text-white",
  group: "bg-gray-900 text-white",
  rule: "bg-emerald-100 text-emerald-800 border border-emerald-300",
  aggregate: "bg-sky-100 text-sky-800 border border-sky-300",
  scenario: "bg-rose-100 text-rose-800 border border-rose-300",
};

const NODE_SIZE = {
  exec: { width: 200, height: 84 },
  group: { width: 200, height: 72 },
  rule: { width: 220, height: 96 },
  aggregate: { width: 220, height: 96 },
  scenario: { width: 220, height: 96 },
} satisfies Record<GraphNodeType, { width: number; height: number }>;

const EDGE_STYLE = {
  stroke: "#9CA3AF",
  strokeWidth: 2,
  strokeDasharray: "6 6",
  opacity: 0.8,
};
const EDGE_CLASS = "animated-dash-edge";

type GraphNodeType = "exec" | "group" | "rule" | "aggregate" | "scenario";

interface GraphNodeData {
  label: string;
  subLabel?: string;
  type: GraphNodeType;
  meta?: {
    detectedAt?: string;
  };
  sourceHandles?: { id: string; top: string }[];
  targetHandleId?: string;
}

const GraphNodeComponent = ({ data }: NodeProps<GraphNodeData>) => {
  const renderHandles = () => {
    switch (data.type) {
      case "exec":
        return (
          <Handle
            id="exec-source"
            type="source"
            position={Position.Right}
            style={{ background: "#6366F1" }}
          />
        );
      case "group":
        return (
          <>
            <Handle
              id={data.targetHandleId ?? "group-target"}
              type="target"
              position={Position.Left}
              style={{ background: "#94A3B8" }}
            />
            {(data.sourceHandles ?? [
              { id: "group-source-1", top: "30%" },
              { id: "group-source-2", top: "50%" },
              { id: "group-source-3", top: "70%" },
            ]).map((handle) => (
              <Handle
                key={handle.id}
                id={handle.id}
                type="source"
                position={Position.Right}
                style={{
                  top: handle.top,
                  transform: "translateY(-50%)",
                  background: "#38BDF8",
                }}
              />
            ))}
          </>
        );
      default:
        return (
          <Handle
            id={data.targetHandleId ?? "target"}
            type="target"
            position={Position.Left}
            style={{
              top: "50%",
              transform: "translateY(-50%)",
              background: "#94A3B8",
            }}
          />
        );
    }
  };

  return (
    <div
      className={`shadow-lg rounded-2xl px-4 py-3 text-sm ${NODE_STYLE[data.type]} select-none`}
      style={{ minWidth: NODE_SIZE[data.type].width - 12 }}
    >
      {renderHandles()}
      <div className="font-semibold truncate">{data.label}</div>
      {data.subLabel && (
        <div className="text-xs opacity-80 mt-1 truncate">{data.subLabel}</div>
      )}
      {data.meta?.detectedAt && (
        <div className="text-[11px] mt-2 opacity-80">{data.meta.detectedAt}</div>
      )}
      {data.meta?.detectedAt && (
        <div className="text-[11px] mt-2 opacity-80">{data.meta.detectedAt}</div>
      )}
    </div>
  );
};

const nodeTypes = { graph: GraphNodeComponent };

interface GroupBucket {
  key: string;
  rules: DetectedRule[];
  aggregates: DetectedAggregate[];
  scenarios: DetectedScenario[];
}

function normalizeKey(value?: string | null) {
  return value && value.trim().length > 0 ? value : "(그룹키 없음)";
}

function buildBuckets(detail: ExecutionDetail): GroupBucket[] {
  const bucketMap = new Map<string, GroupBucket>();

  const ensure = (key: string) => {
    if (!bucketMap.has(key)) {
      bucketMap.set(key, { key, rules: [], aggregates: [], scenarios: [] });
    }
    return bucketMap.get(key)!;
  };

  detail.groupKeys.forEach((key) => ensure(normalizeKey(key)));
  detail.rules.forEach((rule) => {
    ensure(normalizeKey(rule.groupKey as string | undefined)).rules.push(rule);
  });
  detail.aggregates.forEach((aggregate) => {
    ensure(normalizeKey(aggregate.groupKey)).aggregates.push(aggregate);
  });
  detail.scenarios.forEach((scenario) => {
    ensure(normalizeKey(scenario.groupKey)).scenarios.push(scenario);
  });

  if (bucketMap.size === 0) {
    ensure("(탐지 데이터 없음)");
  }

  return Array.from(bucketMap.values());
}

function formatDate(value?: string | null) {
  if (!value) return undefined;
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value ?? undefined;
  }
}

export function ExecutionGraph({ detail, fullscreen = false }: { detail: ExecutionDetail; fullscreen?: boolean }) {
  const { nodes, edges, canvasWidth, canvasHeight } = useMemo(() => {
    const buckets = buildBuckets(detail);
    const rowCount = Math.max(buckets.length, 1);
    const rowSpacing = 160;
    const startY = 120;
    const verticalOffset = 120;
    const canvasHeight = Math.max(
      startY * 2 + verticalOffset,
      startY + (rowCount - 1) * rowSpacing + 200 + verticalOffset,
    );

    const execSize = NODE_SIZE.exec;
    const firstCenterY = startY + verticalOffset;
    const execCenterY = Math.max(startY, firstCenterY - verticalOffset);
    const execPosition = {
      x: 40,
      y: execCenterY - execSize.height / 2,
    };

    const groupGapX = 60;
    const columnGap = 130;
    const groupX = execPosition.x + execSize.width + groupGapX;
    const analysisX = groupX + NODE_SIZE.group.width + columnGap;
    const canvasWidth = analysisX + NODE_SIZE.rule.width + 160;

    const flowNodes: Node<GraphNodeData>[] = [];
    const flowEdges: Edge[] = [];

    const eventLabel = detail.summary.landingRecordId != null
      ? `Event ${detail.summary.landingRecordId}`
      : "Event";
    const eventSubLabel = [
      detail.summary.execDsMpId != null ? `Exec ${detail.summary.execDsMpId}` : null,
      detail.summary.dataSourceId,
    ]
      .filter(Boolean)
      .join(" · ");

    flowNodes.push({
      id: "exec",
      type: "graph",
      position: execPosition,
      data: {
        type: "exec",
        label: eventLabel,
        subLabel: eventSubLabel,
      },
    });

    buckets.forEach((bucket, index) => {
      const rawCenterY = startY + index * rowSpacing;
      const centerY = rawCenterY + verticalOffset;
      const groupId = `group-${index}`;
      const groupSize = NODE_SIZE.group;
      const groupPosition = {
        x: groupX,
        y: centerY - groupSize.height / 2,
      };

      const columnDefs = [
        {
          key: "rule" as const,
          items: bucket.rules,
          builder: (rule: DetectedRule, idx: number) => ({
            id: `rule-${index}-${idx}`,
            label: rule.ruleName ?? rule.ruleId ?? "룰 탐지",
            subLabel: rule.ruleId ? `#${rule.ruleId}` : undefined,
            meta: { detectedAt: formatDate(rule.detectedAt) },
          }),
        },
        {
          key: "agg" as const,
          items: bucket.aggregates,
          builder: (agg: DetectedAggregate, idx: number) => ({
            id: `aggregate-${index}-${idx}`,
            label: agg.ruleName ?? agg.ruleId,
            subLabel: agg.ruleId,
            meta: { detectedAt: formatDate(agg.detectedAt) },
          }),
        },
        {
          key: "scn" as const,
          items: bucket.scenarios,
          builder: (sc: DetectedScenario, idx: number) => ({
            id: `scenario-${index}-${idx}`,
            label: sc.scenarioName ?? sc.scenarioId,
            subLabel: sc.scenarioId,
            meta: { detectedAt: formatDate(sc.detectedAt) },
          }),
        },
      ];

      const groupHandleEntries: { id: string; top: number; category: "rule" | "agg" | "scn"; index: number }[] = [];

      const innerSpacing = 28;
      const heightSequence: number[] = [];
      columnDefs.forEach((col) => {
        const sizeKey = col.key === "rule" ? "rule" : col.key === "agg" ? "aggregate" : "scenario";
        const height = NODE_SIZE[sizeKey].height;
        for (let i = 0; i < col.items.length; i += 1) {
          heightSequence.push(height);
        }
      });

      const totalHeight = heightSequence.reduce((sum, h) => sum + h, 0);
      const totalSpacing = Math.max(0, heightSequence.length - 1) * innerSpacing;
      let currentTop = centerY - (totalHeight + totalSpacing) / 2;
      let remaining = heightSequence.length;

      columnDefs.forEach((column) => {
        const sizeKey = column.key === "rule" ? "rule" : column.key === "agg" ? "aggregate" : "scenario";
        const size = NODE_SIZE[sizeKey];

        column.items.forEach((item, idx) => {
          const built = (column.builder as any)(item, idx);
          const nodeId = built.id;
          const nodeTop = currentTop;
          const nodeCenter = nodeTop + size.height / 2;
          const handleId = `${groupId}-${column.key}-${idx}`;
          const relativeTop = ((nodeCenter - groupPosition.y) / groupSize.height) * 100;
          const clampedTop = Math.max(10, Math.min(90, relativeTop));

          flowNodes.push({
            id: nodeId,
            type: "graph",
            position: { x: analysisX, y: nodeTop },
            data: {
              type: column.key === "rule" ? "rule" : column.key === "agg" ? "aggregate" : "scenario",
              label: built.label,
              subLabel: built.subLabel,
              meta: built.meta,
              targetHandleId: `${nodeId}-target`,
            },
          });
          flowEdges.push({
            id: `edge-${groupId}-${nodeId}`,
            source: groupId,
            target: nodeId,
            type: "default",
            sourceHandle: handleId,
            targetHandle: `${nodeId}-target`,
            style: EDGE_STYLE,
            className: EDGE_CLASS,
            animated: true,
          });
          groupHandleEntries.push({ id: handleId, top: clampedTop, category: column.key, index: idx });

          currentTop = nodeTop + size.height;
          remaining -= 1;
          if (remaining > 0) {
            currentTop += innerSpacing;
          }
        });
      });

      const categoryPriority: Record<"rule" | "agg" | "scn", number> = { rule: 0, agg: 1, scn: 2 };
      const sortedHandleEntries = groupHandleEntries.sort((a, b) => {
        const diff = categoryPriority[a.category] - categoryPriority[b.category];
        if (diff !== 0) return diff;
        if (a.top === b.top) return a.index - b.index;
        return a.top - b.top;
      });

      const groupHandles = sortedHandleEntries.length
        ? sortedHandleEntries.map((entry) => ({ id: entry.id, top: `${entry.top}%` }))
        : [{ id: `${groupId}-placeholder`, top: "50%" }];

      flowNodes.push({
        id: groupId,
        type: "graph",
        position: groupPosition,
        data: {
          type: "group",
          label: bucket.key,
          sourceHandles: groupHandles,
          targetHandleId: `${groupId}-target`,
        },
      });

      flowEdges.push({
        id: `edge-exec-${groupId}`,
        source: "exec",
        target: groupId,
        type: "default",
        targetHandle: `${groupId}-target`,
        style: EDGE_STYLE,
        className: EDGE_CLASS,
        animated: true,
      });
    });

    return {
      nodes: flowNodes,
      edges: flowEdges,
      canvasWidth,
      canvasHeight,
    };
  }, [detail]);

  const containerOuterClass = fullscreen ? "w-full h-full" : "w-full overflow-x-auto";
  const containerInnerClass = fullscreen
    ? "w-full h-full rounded-3xl border border-gray-200"
    : "mx-auto rounded-3xl border border-gray-200";
  const containerStyle = fullscreen
    ? { width: "100%", height: "100%" }
    : { width: canvasWidth, height: canvasHeight, minHeight: 320 };

  return (
    <div className={containerOuterClass}>
      <div className={containerInnerClass} style={containerStyle}>
        <ReactFlow
          key={detail.summary.execDsMpId}
          defaultNodes={nodes}
          defaultEdges={edges}
          nodeTypes={nodeTypes}
          fitView={fullscreen}
          nodesDraggable
          nodesConnectable={false}
          panOnDrag
          panOnScroll
          zoomOnScroll={false}
          zoomOnPinch={false}
          zoomOnDoubleClick={false}
          elementsSelectable={false}
        >
          <Background gap={32} color="#E5E7EB" size={1} />
        </ReactFlow>
        <style jsx global>{`
          @keyframes edge-dash-move {
            to {
              stroke-dashoffset: -24;
            }
          }
          .react-flow__edge-path.${EDGE_CLASS} {
            animation: edge-dash-move 4s linear infinite;
          }
        `}</style>
      </div>
    </div>
  );
}
