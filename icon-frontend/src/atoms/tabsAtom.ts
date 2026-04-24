// [2026-04-24] 탭 기능 구현 - 열린 탭 목록 및 활성 탭 상태 관리
import { atom } from "jotai";

export interface TabItem {
  path: string;     // 탭의 경로 (URL path)
  label: string;    // 탭에 표시할 이름
  parentLabel?: string; // 상위 메뉴명 (툴팁용)
}

// 열린 탭 목록 (세션 중 유지, 새로고침 시 초기화)
export const tabsAtom = atom<TabItem[]>([]);

// 현재 활성 탭 경로
export const activeTabPathAtom = atom<string>("");
