"use client";

// [2026-04-20] 데이터소스 파서 연결 섹션 컴포넌트
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  fetchActiveParsers,
  fetchLinkedParsers,
  linkParser,
  unlinkParser,
  PARSER_TYPE_LABELS,
  PARSER_TYPE_COLORS,
} from "@/app/parsers/api";
import type { LinkedParserResponse } from "@/app/parsers/api";
import { useState } from "react";
import { toast } from "react-hot-toast";
import {
  PlusIcon,
  TrashIcon,
  ScissorsIcon,
  InformationCircleIcon,
} from "@heroicons/react/24/outline";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";

interface Props {
  dataSourceId: string;
}

export default function DataSourceParserSection({ dataSourceId }: Props) {
  const queryClient = useQueryClient();
  const [selectedParserId, setSelectedParserId] = useState("");
  const [removingLink, setRemovingLink] = useState<LinkedParserResponse | null>(null);

  // 이 데이터소스에 연결된 파서 목록
  const { data: linked = [], isLoading } = useQuery({
    queryKey: ["datasource-parsers", dataSourceId],
    queryFn: () => fetchLinkedParsers(dataSourceId),
  });

  // 전체 활성 파서 목록 (드롭다운용)
  const { data: allParsers = [] } = useQuery({
    queryKey: ["activeParsers"],
    queryFn: fetchActiveParsers,
  });

  // 이미 연결된 파서 ID 목록
  const linkedIds = new Set(linked.map((l) => l.parser.parserId));

  // 파서 연결
  const linkMutation = useMutation({
    mutationFn: () =>
      linkParser(dataSourceId, {
        parserId: selectedParserId,
        parserOrder: linked.length,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["datasource-parsers", dataSourceId] });
      toast.success("파서가 연결되었습니다.");
      setSelectedParserId("");
    },
    onError: (err: any) =>
      toast.error(err?.response?.data?.message || "파서 연결에 실패했습니다."),
  });

  // 파서 연결 해제
  const unlinkMutation = useMutation({
    mutationFn: (parserId: string) => unlinkParser(dataSourceId, parserId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["datasource-parsers", dataSourceId] });
      toast.success("파서 연결이 해제되었습니다.");
      setRemovingLink(null);
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || "연결 해제에 실패했습니다.");
      setRemovingLink(null);
    },
  });

  // 연결 가능한 파서 (이미 연결된 파서 제외)
  const available = allParsers.filter((p) => !linkedIds.has(p.parserId));

  return (
    <div className="space-y-5">
      {/* 안내 */}
      <div className="flex gap-2 p-4 bg-blue-50 border border-blue-200 rounded-lg text-sm text-blue-700">
        <InformationCircleIcon className="h-5 w-5 shrink-0 mt-0.5" />
        <div>
          <p className="font-semibold mb-1">파서 동작 방식</p>
          <p className="text-blue-600 text-xs">
            파서는 수집된 데이터의 <strong>파싱 대상 필드</strong>(예: line)를 지정된 규칙으로 분해하여
            COLUMN1, COLUMN2 ... 형태의 새 필드를 생성합니다.
            원본 필드(line)는 매핑 결과에서 제거됩니다.
            생성된 COLUMN 필드들은 <strong>원본 필드</strong> 탭에서 표준 필드와 매핑할 수 있습니다.
          </p>
          <p className="text-blue-600 text-xs mt-1">
            파서가 여러 개 연결된 경우 <strong>적용 순서(parserOrder)</strong>대로 순차 실행됩니다.
          </p>
        </div>
      </div>

      {/* 파서 추가 */}
      <div className="flex items-center gap-3">
        <select
          value={selectedParserId}
          onChange={(e) => setSelectedParserId(e.target.value)}
          className="flex-1 px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">파서 선택...</option>
          {available.map((p) => (
            <option key={p.parserId} value={p.parserId}>
              [{PARSER_TYPE_LABELS[p.parserType]}] {p.parserName}
              {p.sourceField ? ` — 대상: ${p.sourceField}` : ""}
            </option>
          ))}
        </select>
        <button
          disabled={!selectedParserId || linkMutation.isPending}
          onClick={() => linkMutation.mutate()}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <PlusIcon className="h-4 w-4" />
          연결
        </button>
      </div>

      {/* 연결된 파서 목록 */}
      {isLoading ? (
        <div className="text-sm text-gray-400 py-6 text-center">로딩 중...</div>
      ) : linked.length === 0 ? (
        <div className="text-center py-10 text-gray-400">
          <ScissorsIcon className="h-10 w-10 mx-auto mb-3 opacity-40" />
          <p className="text-sm">연결된 파서가 없습니다.</p>
          <p className="text-xs mt-1">위에서 파서를 선택하여 연결하세요.</p>
        </div>
      ) : (
        <div className="space-y-2">
          {linked
            .sort((a, b) => a.parserOrder - b.parserOrder)
            .map((link, idx) => (
              <div
                key={link.dataSourceParserId}
                className="flex items-center gap-4 p-4 bg-white border border-gray-200 rounded-xl hover:border-blue-200 transition-colors"
              >
                {/* 순서 뱃지 */}
                <span className="w-7 h-7 flex-none flex items-center justify-center bg-blue-100 text-blue-700 rounded-full text-xs font-bold">
                  {idx + 1}
                </span>

                {/* 파서 정보 */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-gray-900 text-sm">{link.parser.parserName}</span>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${PARSER_TYPE_COLORS[link.parser.parserType]}`}>
                      {PARSER_TYPE_LABELS[link.parser.parserType]}
                    </span>
                    {!link.isActive && (
                      <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-500">비활성</span>
                    )}
                  </div>
                  <div className="flex items-center gap-4 mt-1 text-xs text-gray-500">
                    {link.parser.sourceField && (
                      <span>
                        파싱 대상:{" "}
                        <code className="bg-gray-100 px-1 rounded font-mono">{link.parser.sourceField}</code>
                      </span>
                    )}
                    <span>규칙 {link.parser.ruleCount}개</span>
                    {link.parser.description && (
                      <span className="truncate max-w-xs">{link.parser.description}</span>
                    )}
                  </div>
                </div>

                {/* 삭제 */}
                <button
                  onClick={() => setRemovingLink(link)}
                  className="p-2 text-red-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                  title="연결 해제"
                >
                  <TrashIcon className="h-4 w-4" />
                </button>
              </div>
            ))}
        </div>
      )}

      {/* 연결 해제 확인 */}
      <ConfirmDialog
        isOpen={!!removingLink}
        onClose={() => setRemovingLink(null)}
        onConfirm={() => removingLink && unlinkMutation.mutate(removingLink.parser.parserId)}
        title="파서 연결 해제"
        message={`"${removingLink?.parser.parserName}" 파서의 연결을 해제하시겠습니까?`}
        confirmText="해제"
        confirmColor="red"
      />
    </div>
  );
}
