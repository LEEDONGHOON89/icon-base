// [2026-04-24] 탭 바 컴포넌트 - 열린 탭 목록 표시, 스크롤 + 더보기 드롭다운
"use client";

import React, { useRef, useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAtom } from "jotai";
import { tabsAtom, activeTabPathAtom, TabItem } from "@/atoms/tabsAtom";
import {
  XMarkIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  Bars3BottomLeftIcon,
} from "@heroicons/react/24/outline";

const TabBar: React.FC = () => {
  const router = useRouter();
  const [tabs, setTabs] = useAtom(tabsAtom);
  const [activeTabPath, setActiveTabPath] = useAtom(activeTabPathAtom);

  const scrollRef = useRef<HTMLDivElement>(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(false);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // 스크롤 가능 여부 감지
  const checkScroll = useCallback(() => {
    const el = scrollRef.current;
    if (!el) return;
    setCanScrollLeft(el.scrollLeft > 0);
    setCanScrollRight(el.scrollLeft + el.clientWidth < el.scrollWidth - 1);
  }, []);

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    checkScroll();
    el.addEventListener("scroll", checkScroll);
    const ro = new ResizeObserver(checkScroll);
    ro.observe(el);
    return () => {
      el.removeEventListener("scroll", checkScroll);
      ro.disconnect();
    };
  }, [tabs, checkScroll]);

  // 탭 개수 변경 시 활성 탭으로 스크롤
  useEffect(() => {
    if (!scrollRef.current || !activeTabPath) return;
    const activeEl = scrollRef.current.querySelector<HTMLElement>(
      `[data-tab-path="${CSS.escape(activeTabPath)}"]`
    );
    activeEl?.scrollIntoView({ block: "nearest", inline: "nearest" });
    setTimeout(checkScroll, 50);
  }, [activeTabPath, tabs.length, checkScroll]);

  // 드롭다운 외부 클릭 닫기
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(e.target as Node)
      ) {
        setIsDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  const scrollBy = (delta: number) => {
    scrollRef.current?.scrollBy({ left: delta, behavior: "smooth" });
  };

  // 탭 클릭 → 해당 경로로 이동
  const handleTabClick = (path: string) => {
    setActiveTabPath(path);
    router.push(path);
    setIsDropdownOpen(false);
  };

  // 탭 닫기
  const handleTabClose = (e: React.MouseEvent, path: string) => {
    e.stopPropagation();
    const idx = tabs.findIndex((t) => t.path === path);
    const newTabs = tabs.filter((t) => t.path !== path);
    setTabs(newTabs);

    // 닫힌 탭이 활성 탭이면 인접 탭으로 이동
    if (path === activeTabPath && newTabs.length > 0) {
      const nextTab = newTabs[Math.min(idx, newTabs.length - 1)];
      setActiveTabPath(nextTab.path);
      router.push(nextTab.path);
    } else if (newTabs.length === 0) {
      setActiveTabPath("");
      router.push("/dashboard");
    }
  };

  // 탭이 없으면 렌더링 하지 않음
  if (tabs.length === 0) return null;

  return (
    <div className="flex items-center bg-gray-50 border-b border-gray-200 h-9 select-none">
      {/* 왼쪽 스크롤 버튼 */}
      {canScrollLeft && (
        <button
          onClick={() => scrollBy(-160)}
          className="flex-shrink-0 h-full px-1.5 text-gray-400 hover:text-gray-700 hover:bg-gray-100 border-r border-gray-200 transition-colors"
          title="왼쪽으로 스크롤"
        >
          <ChevronLeftIcon className="h-4 w-4" />
        </button>
      )}

      {/* 탭 목록 (가로 스크롤) */}
      <div
        ref={scrollRef}
        className="flex-1 flex items-center overflow-x-auto scrollbar-hide"
        style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}
      >
        {tabs.map((tab) => {
          const isActive = tab.path === activeTabPath;
          return (
            <button
              key={tab.path}
              data-tab-path={tab.path}
              onClick={() => handleTabClick(tab.path)}
              title={tab.parentLabel ? `${tab.parentLabel} > ${tab.label}` : tab.label}
              className={`
                group flex-shrink-0 flex items-center gap-1.5 h-full px-3 text-sm whitespace-nowrap
                border-r border-gray-200 transition-colors duration-150
                ${
                  isActive
                    ? "bg-white text-blue-600 font-medium border-b-2 border-b-blue-500 -mb-px"
                    : "text-gray-600 hover:bg-white hover:text-gray-900"
                }
              `}
            >
              <span className="max-w-[140px] truncate">{tab.label}</span>
              <span
                onClick={(e) => handleTabClose(e, tab.path)}
                className={`
                  flex-shrink-0 rounded p-0.5 transition-colors duration-150
                  ${
                    isActive
                      ? "text-blue-400 hover:text-blue-700 hover:bg-blue-100"
                      : "text-transparent group-hover:text-gray-400 hover:!text-gray-700 hover:bg-gray-200"
                  }
                `}
                title="탭 닫기"
              >
                <XMarkIcon className="h-3.5 w-3.5" />
              </span>
            </button>
          );
        })}
      </div>

      {/* 오른쪽 스크롤 버튼 */}
      {canScrollRight && (
        <button
          onClick={() => scrollBy(160)}
          className="flex-shrink-0 h-full px-1.5 text-gray-400 hover:text-gray-700 hover:bg-gray-100 border-l border-gray-200 transition-colors"
          title="오른쪽으로 스크롤"
        >
          <ChevronRightIcon className="h-4 w-4" />
        </button>
      )}

      {/* 더보기 드롭다운 (탭이 3개 이상일 때 표시) */}
      {tabs.length >= 3 && (
        <div ref={dropdownRef} className="relative flex-shrink-0">
          <button
            onClick={() => setIsDropdownOpen((v) => !v)}
            className={`
              flex items-center gap-1 h-9 px-2 text-xs text-gray-500
              border-l border-gray-200 hover:bg-gray-100 hover:text-gray-700 transition-colors
              ${isDropdownOpen ? "bg-gray-100 text-gray-700" : ""}
            `}
            title="전체 탭 목록"
          >
            <Bars3BottomLeftIcon className="h-4 w-4" />
            <span className="font-medium">{tabs.length}</span>
          </button>

          {/* 드롭다운 패널 */}
          {isDropdownOpen && (
            <div className="absolute right-0 top-full mt-0.5 w-60 bg-white border border-gray-200 rounded-lg shadow-xl z-50 overflow-hidden">
              <div className="px-3 py-2 text-xs font-semibold text-gray-400 uppercase tracking-wider border-b border-gray-100">
                열린 탭 ({tabs.length})
              </div>
              <div className="max-h-72 overflow-y-auto">
                {tabs.map((tab) => {
                  const isActive = tab.path === activeTabPath;
                  return (
                    <button
                      key={tab.path}
                      onClick={() => handleTabClick(tab.path)}
                      className={`
                        w-full flex items-center justify-between px-3 py-2 text-sm
                        hover:bg-blue-50 transition-colors
                        ${isActive ? "bg-blue-50 text-blue-600 font-medium" : "text-gray-700"}
                      `}
                    >
                      <div className="flex flex-col items-start min-w-0">
                        <span className="truncate max-w-[180px]">{tab.label}</span>
                        {tab.parentLabel && (
                          <span className="text-xs text-gray-400 truncate max-w-[180px]">
                            {tab.parentLabel}
                          </span>
                        )}
                      </div>
                      <span
                        onClick={(e) => handleTabClose(e, tab.path)}
                        className="flex-shrink-0 ml-2 p-0.5 rounded text-gray-300 hover:text-gray-600 hover:bg-gray-200 transition-colors"
                        title="탭 닫기"
                      >
                        <XMarkIcon className="h-3.5 w-3.5" />
                      </span>
                    </button>
                  );
                })}
              </div>
              {/* 전체 닫기 */}
              <div className="border-t border-gray-100">
                <button
                  onClick={() => {
                    setTabs([]);
                    setActiveTabPath("");
                    setIsDropdownOpen(false);
                    router.push("/dashboard");
                  }}
                  className="w-full px-3 py-2 text-xs text-red-500 hover:bg-red-50 hover:text-red-700 transition-colors text-left"
                >
                  전체 탭 닫기
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default TabBar;
