"use client";

import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useState, Suspense } from "react";
import api from "@/lib/api";

type EventStream = { groupKey: string; eventDt: string; eventData: Record<string, any> };
type Detail = {
  groupKey: string;
  ruleId: string;
  detectedAt: string;
  windowStart: string;
  windowEnd: string;
  predicateRuleId?: string | null;
  predicateRuleName?: string | null;
  matchedEvents: EventStream[];
  matchedCount: number;
};

async function fetchDetail(params: { groupKey: string; ruleId?: string; aggregateId?: string; anchor: string }): Promise<Detail> {
  const res = await api.get<Detail>("/api/v1/analytics/aggregates/detail", { params });
  return res.data;
}

// 이벤트 데이터를 카테고리별로 분류하는 함수
function categorizeEventData(eventData: Record<string, any>) {
  const categories: Record<string, Record<string, any>> = {
    거래정보: {},
    고객정보: {},
    계좌정보: {},
    기타정보: {},
  };

  Object.entries(eventData).forEach(([key, value]) => {
    if (key.includes("거래") || key.includes("금액") || key.includes("Transaction") || key.includes("Amount")) {
      categories.거래정보[key] = value;
    } else if (key.includes("고객") || key.includes("Customer") || key.includes("User") || key.includes("이름")) {
      categories.고객정보[key] = value;
    } else if (key.includes("계좌") || key.includes("Account") || key.includes("카드") || key.includes("Card")) {
      categories.계좌정보[key] = value;
    } else {
      categories.기타정보[key] = value;
    }
  });

  // 빈 카테고리 제거
  return Object.entries(categories)
    .filter(([_, data]) => Object.keys(data).length > 0)
    .reduce((acc, [cat, data]) => ({ ...acc, [cat]: data }), {});
}

// 이벤트 상세 컴포넌트
function EventDetail({ event, index }: { event: EventStream; index: number }) {
  const [activeTab, setActiveTab] = useState<string>("");
  const categories = categorizeEventData(event.eventData);
  const categoryNames = Object.keys(categories);

  // 첫 번째 탭을 기본값으로 설정
  if (!activeTab && categoryNames.length > 0) {
    setActiveTab(categoryNames[0]);
  }

  return (
    <div className="border rounded-xl overflow-hidden">
      {/* 이벤트 시각 헤더 */}
      <div className="bg-gray-50 px-4 py-3 border-b">
        <div className="text-sm font-medium text-gray-700">
          이벤트 {index + 1} - {new Date(event.eventDt).toLocaleString()}
        </div>
      </div>

      {/* 탭 헤더 */}
      <div className="flex border-b bg-white">
        {categoryNames.map((cat) => (
          <button
            key={cat}
            onClick={() => setActiveTab(cat)}
            className={`px-4 py-3 text-sm font-medium transition-colors border-b-2 ${
              activeTab === cat
                ? "border-blue-600 text-blue-600 bg-blue-50"
                : "border-transparent text-gray-600 hover:text-gray-800 hover:bg-gray-50"
            }`}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* 탭 내용 */}
      <div className="p-4 bg-white">
        {categoryNames.map((cat) => {
          if (cat !== activeTab) return null;
          const data = (categories as Record<string, Record<string, any>>)[cat];

          return (
            <div key={cat} className="space-y-2">
              {Object.entries(data).map(([key, value]) => (
                <div key={key} className="flex items-start py-2 border-b last:border-b-0">
                  <div className="w-1/3 text-sm font-medium text-gray-600">{key}</div>
                  <div className="w-2/3 text-sm text-gray-900">
                    {typeof value === "object" ? (
                      <pre className="text-xs bg-gray-50 p-2 rounded overflow-x-auto">
                        {JSON.stringify(value, null, 2)}
                      </pre>
                    ) : (
                      String(value)
                    )}
                  </div>
                </div>
              ))}
            </div>
          );
        })}
      </div>
    </div>
  );
}

function AggregateDetailPageContent() {
  const sp = useSearchParams();
  const groupKey = sp.get("groupKey") || "";
  const aggregateId = sp.get("aggregateId") || "";
  const anchor = sp.get("anchor") || "";

  const { data, isLoading, error } = useQuery({
    queryKey: ["agg-detail", groupKey, aggregateId, anchor],
    queryFn: () => fetchDetail({ groupKey, aggregateId, anchor }),
    enabled: !!groupKey && !!aggregateId && !!anchor,
  });

  if (!groupKey || !aggregateId || !anchor) return <div className="p-6">필수 파라미터가 없습니다.</div>;
  if (isLoading) return <div className="p-6">불러오는 중…</div>;
  if (error || !data) return <div className="p-6">상세를 불러오지 못했습니다.</div>;

  return (
    <div className="max-w-5xl mx-auto py-8 space-y-6">
      <div className="bg-white rounded-2xl shadow p-6 border border-gray-100">
        <h1 className="text-xl font-semibold text-gray-800">집계 상세</h1>
        <p className="text-gray-600 mt-1">집계에 매칭된 이벤트 목록을 표시합니다.</p>
        <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
          <div><span className="text-gray-500">그룹키</span> <span className="font-mono ml-2">{data.groupKey}</span></div>
          <div><span className="text-gray-500">집계</span> <span className="font-mono ml-2">{data.ruleId}</span></div>
          <div><span className="text-gray-500">앵커</span> <span className="ml-2">{new Date(data.detectedAt).toLocaleString()}</span></div>
          <div><span className="text-gray-500">창</span> <span className="ml-2">{new Date(data.windowStart).toLocaleString()} ~ {new Date(data.windowEnd).toLocaleString()}</span></div>
          <div className="md:col-span-2">
            <span className="text-gray-500">대상 룰</span>
            <span className="ml-2">
              {data.predicateRuleId ? (
                <>
                  {data.predicateRuleName ? `${data.predicateRuleName} (${data.predicateRuleId})` : data.predicateRuleId}
                </>
              ) : (
                "(없음)"
              )}
            </span>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow p-6 border border-gray-100">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-800">매칭 이벤트</h2>
          <div className="text-sm text-gray-600">총 {data.matchedCount}건</div>
        </div>
        {data.matchedEvents.length === 0 ? (
          <div className="text-gray-500 py-8 text-center">매칭된 이벤트가 없습니다.</div>
        ) : (
          <div className="space-y-4">
            {data.matchedEvents.map((ev, i) => (
              <EventDetail key={`${ev.groupKey}-${ev.eventDt}-${i}`} event={ev} index={i} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default function AggregateDetailPage() {
  return (
    <Suspense fallback={<div className="p-6">불러오는 중…</div>}>
      <AggregateDetailPageContent />
    </Suspense>
  );
}
