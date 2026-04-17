"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Provider as JotaiProvider, createStore } from "jotai";
import React from "react";
import { setupApi } from "@/lib/api";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // 탭 전환/포커스 복귀 시 자동 리패치/리렌더 방지
      refetchOnWindowFocus: false,
      refetchOnReconnect: false,
      refetchOnMount: false,
      // 데이터 신선도 기간(필요시 조정)
      staleTime: 5 * 60 * 1000,
      // v4: gcTime (v3: cacheTime). 캐시 유지 시간을 충분히 늘림
      gcTime: 10 * 60 * 1000,
    },
  },
});
const jotaiStore = createStore();

// API 인스턴스 설정 (Jotai Store를 주입)
setupApi(jotaiStore);

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <JotaiProvider store={jotaiStore}>{children}</JotaiProvider>
    </QueryClientProvider>
  );
}
