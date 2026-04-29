"use client";

import React from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAtom } from "jotai";
import { authAtom, userAtom } from "@/atoms/authAtom";
import api from "@/lib/api";
import {
  ArrowRightOnRectangleIcon,
  UsersIcon,
  ShieldCheckIcon,
  DocumentTextIcon,
  PuzzlePieceIcon,
  Bars3Icon,
  CircleStackIcon,
  MapIcon,
  CogIcon,
  BellAlertIcon,
  ShareIcon,
  ClockIcon,
  UserCircleIcon,
  TableCellsIcon,
  HomeIcon,
  BeakerIcon,
  ArrowsRightLeftIcon,
  CubeIcon,
  MagnifyingGlassCircleIcon,
  SparklesIcon,
  ServerStackIcon,
  ScissorsIcon,
} from "@heroicons/react/24/outline";
import { useState } from "react";
import { ChevronDownIcon, ChevronRightIcon } from "@heroicons/react/24/outline";
// [2026-04-24] 탭 기능 구현 - 탭 상태 atom 및 TabBar 컴포넌트 임포트
import { tabsAtom, activeTabPathAtom } from "@/atoms/tabsAtom";
import TabBar from "@/components/TabBar";

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const pathname = usePathname();
  const router = useRouter();
  const [auth, setAuth] = useAtom(authAtom);
  const [, setUser] = useAtom(userAtom);
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  // [2026-04-24] 탭 상태 관리
  const [, setTabs] = useAtom(tabsAtom);
  const [, setActiveTabPath] = useAtom(activeTabPathAtom);

  // 메뉴 검색 autocomplete 상태
  const [searchQuery, setSearchQuery] = useState("");
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const searchInputRef = React.useRef<HTMLInputElement>(null);
  const searchDropdownRef = React.useRef<HTMLDivElement>(null);

  // 현재 경로에 따라 초기 펼침 상태 결정
  const getInitialExpandedMenus = () => {
    const expanded: string[] = [];

    // 시스템 설정 관리
    if (
      pathname.startsWith("/fields") ||
      pathname.startsWith("/data-sources") ||
      pathname.startsWith("/agents") ||
      pathname.startsWith("/parsers")
    ) {
      expanded.push("system");
    }

    // 계정관리
    if (pathname.startsWith("/users")) {
      expanded.push("user");
    }

    // 탐지 정책관리 (detection-engine)
    if (
      pathname.startsWith("/sensors") ||
      pathname.startsWith("/domain-settings") ||
      pathname.startsWith("/rules") ||
      pathname.startsWith("/scenarios") ||
      pathname.startsWith("/relation-rules") ||
      pathname.startsWith("/engine") ||
      pathname.startsWith("/ai-support")
    ) {
      expanded.push("detection-engine");
    }

    // 탐지 모니터링
    if (
      pathname.startsWith("/detections") ||
      pathname.startsWith("/transactions")
    ) {
      expanded.push("detections");
    }

    // 감사
    if (pathname.startsWith("/audit")) {
      expanded.push("watch-over");
    }

    return expanded;
  };

  const [expandedMenus, setExpandedMenus] = useState<string[]>(getInitialExpandedMenus());

  // 경로 변경 시 펼침 상태 업데이트
  React.useEffect(() => {
    setExpandedMenus(getInitialExpandedMenus());
  }, [pathname]);

  const handleLogout = async () => {
    try {
      if (auth.refreshToken) {
        await api.post("/api/v1/auth/logout", {
          refreshToken: auth.refreshToken,
        });
      }
    } catch (error) {
      console.error("로그아웃 API 호출 실패:", error);
    } finally {
      setAuth({
        isAuthenticated: false,
        accessToken: null,
        refreshToken: null,
      });
      setUser(null); // 사용자 정보도 초기화
      router.push("/login");
    }
  };

  const menuItems = [
    // 대시보드
    {
      name: "대시보드",
      href: "/dashboard",
      icon: HomeIcon,
      color: "from-blue-500 to-blue-600"
    },
    // 시나리오 탐지 (1단계 메뉴)
    {
      name: "시나리오 탐지",
      href: "/detections/scenarios",
      icon: MapIcon,
      color: "from-emerald-500 to-emerald-600"
    },
    // 탐지 모니터링
    {
      id: "detections",
      name: "탐지모니터링",
      icon: BellAlertIcon,
      color: "from-teal-500 to-teal-600",
      children: [
        {
          name: "시나리오 탐지",
          href: "/detections/scenarios",
          icon: MapIcon,
        },
        {
          name: "룰 탐지",
          href: "/detections/rules",
          icon: DocumentTextIcon,
        },
        {
          name: "탐지 조치",
          href: "/detections/actions",
          icon: ShieldCheckIcon,
        },
        {
          name: "엔티티 행적",
          href: "/detections/entity-history",
          icon: ClockIcon,
        },
        {
          name: "시뮬레이션",
          href: "/detections/simulation",
          icon: BeakerIcon,
        },
        {
          name: "실행 이력",
          href: "/detections/executions",
          icon: ClockIcon,
        },
        {
          name: "트랜잭션 추적",
          href: "/transactions",
          icon: MagnifyingGlassCircleIcon,
        },
        {
          name: "AI 모니터링",
          href: "/detections/ai-monitoring",
          icon: SparklesIcon,
        },
      ],
    },
    // 탐지 엔진 설정
    {
      id: "detection-engine",
      name: "탐지정책관리",
      icon: PuzzlePieceIcon,
      color: "from-indigo-500 to-indigo-600",
      children: [
        {
          name: "센서 관리",
          href: "/sensors",
          icon: BellAlertIcon,
        },
        {
          name: "룰 관리",
          href: "/rules",
          icon: DocumentTextIcon,
        },
        {
          name: "시나리오 관리",
          href: "/scenarios",
          icon: PuzzlePieceIcon,
        },
        {
          name: "도메인 설정",
          href: "/domain-settings",
          icon: CubeIcon,
        },
        {
          name: "엔티티 관계 설정",
          href: "/relation-rules",
          icon: ArrowsRightLeftIcon,
        },
        {
          name: "AI 서포트",
          href: "/ai-support",
          icon: SparklesIcon,
        },
      ],
    },
    {
      id: "user",
      name: "계정관리",
      icon: CogIcon,
      color: "from-slate-500 to-slate-600",
      children: [
        {
          name: "사용자 관리",
          href: "/users",
          icon: UsersIcon,
        },
      ]
    },
    {
      id: "watch-over",
      name: "감사",
      icon: ClockIcon,
      color: "from-purple-500 to-purple-600",
      children: [
        {
          name: "변경이력",
          href: "/audit/change-history",
          icon: ClockIcon,
        },
      ]
    },

    // 시스템 관리
    {
      id: "system",
      name: "시스템 설정 관리",
      icon: CogIcon,
      color: "from-slate-500 to-slate-600",
      children: [
        // 데이터 연동
        {
          name: "데이터소스",
          href: "/data-sources",
          icon: CircleStackIcon,
          color: "from-cyan-500 to-cyan-600"
        },
        {
          name: "표준 필드",
          href: "/fields/standard",
          icon: TableCellsIcon,
        },
        {
          name: "엔티티 필드",
          href: "/fields/entity",
          icon: TableCellsIcon,
        },

        {
          name: "에이전트 관리",
          href: "/agents",
          icon: ServerStackIcon,
        },
        // [2026-04-20] 파서 관리 메뉴 추가
        {
          name: "파서 관리",
          href: "/parsers",
          icon: ScissorsIcon,
        },
      ],
    },

  ];

  const toggleMenu = (menuId: string) => {
    setExpandedMenus((prev) =>
      prev.includes(menuId)
        ? prev.filter((id) => id !== menuId)
        : [...prev, menuId]
    );
  };

  // 메뉴 아이템 평면화 (검색용)
  const flattenedMenuItems = React.useMemo(() => {
    const items: Array<{ name: string; href: string; parentName?: string }> = [];

    menuItems.forEach((item) => {
      if (item.href) {
        // 직접 href가 있는 메뉴 (대시보드 등)
        items.push({ name: item.name, href: item.href });
      }

      if ('children' in item && item.children) {
        // 자식 메뉴가 있는 경우
        (item.children as any[]).forEach((child) => {
          items.push({
            name: child.name,
            href: child.href,
            parentName: item.name,
          });
        });
      }
    });

    return items;
  }, []);

  // [2026-04-29] 탭으로 유지할 경로 화이트리스트
  //   여기에 추가된 경로(및 하위 경로)만 탭 바에 표시됨
  //   나머지 메뉴는 탭 생성 없이 일반 페이지 이동으로 동작
  const TAB_ENABLED_PATHS = [
    "/detections/scenarios",  // 시나리오 탐지
    "/detections/rules",      // 룰 탐지
    "/detections/actions",    // 탐지 조치
    "/detections/entity-history", // 엔티티 행적
  ];

  // 현재 경로가 탭 대상인지 확인 (화이트리스트 경로 또는 그 하위 경로)
  const isTabEnabledPath = (path: string) =>
    TAB_ENABLED_PATHS.some(
      (tp) => path === tp || path.startsWith(tp + "/")
    );

  // [2026-04-24] pathname 변경 시 탭 자동 추가 (중복 방지)
  // [2026-04-24] 서브 페이지(상세/수정/신규 등) 탭 제외 처리
  // [2026-04-29] TAB_ENABLED_PATHS 화이트리스트: 해당 경로만 탭 생성, 나머지는 일반 이동
  React.useEffect(() => {
    if (!pathname || pathname === "/login") return;

    // 현재 경로에 해당하는 메뉴 항목 조회 (정확 일치 우선)
    const matched = flattenedMenuItems.find(
      (item) =>
        pathname === item.href ||
        (item.href !== "/" && pathname.startsWith(item.href + "/"))
    );

    // 탭 비대상 경로: 탭 생성하지 않고 active 탭도 해제
    if (!isTabEnabledPath(pathname)) {
      setActiveTabPath("");
      return;
    }

    const isSubPage = matched != null && pathname !== matched.href;

    if (isSubPage) {
      // 상세/수정 등 하위 경로: 새 탭 추가하지 않고 부모 메뉴 탭을 active 로 설정
      const parentPath = matched.href;
      setActiveTabPath(parentPath);
      // 부모 탭이 아직 없으면 생성 (직접 URL 진입 시 대비)
      setTabs((prev) => {
        if (prev.some((t) => t.path === parentPath)) return prev;
        return [...prev, {
          path: parentPath,
          label: matched.name,
          parentLabel: matched.parentName,
        }];
      });
    } else {
      // 탭 대상 메뉴 직접 경로: 탭 추가 (중복 방지)
      const label = matched?.name ?? pathname;
      const parentLabel = matched?.parentName;

      setActiveTabPath(pathname);
      setTabs((prev) => {
        if (prev.some((t) => t.path === pathname)) return prev;
        return [...prev, { path: pathname, label, parentLabel }];
      });
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pathname]);

  // 검색 필터링
  const filteredMenuItems = React.useMemo(() => {
    if (!searchQuery.trim()) return [];

    const query = searchQuery.toLowerCase().trim();
    return flattenedMenuItems.filter(
      (item) =>
        item.name.toLowerCase().includes(query) ||
        item.parentName?.toLowerCase().includes(query)
    );
  }, [searchQuery, flattenedMenuItems]);

  // 검색 입력 핸들러
  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(e.target.value);
    setIsSearchOpen(true);
    setHighlightedIndex(-1);
  };

  // 검색 포커스 핸들러
  const handleSearchFocus = () => {
    if (searchQuery.trim()) {
      setIsSearchOpen(true);
    }
  };

  // 메뉴 선택 핸들러
  const handleMenuSelect = (href: string) => {
    router.push(href);
    setSearchQuery("");
    setIsSearchOpen(false);
    setHighlightedIndex(-1);
    searchInputRef.current?.blur();
  };

  // 키보드 네비게이션
  const handleSearchKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!isSearchOpen || filteredMenuItems.length === 0) return;

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setHighlightedIndex((prev) =>
          prev < filteredMenuItems.length - 1 ? prev + 1 : prev
        );
        break;
      case 'ArrowUp':
        e.preventDefault();
        setHighlightedIndex((prev) => (prev > 0 ? prev - 1 : -1));
        break;
      case 'Enter':
        e.preventDefault();
        if (highlightedIndex >= 0 && highlightedIndex < filteredMenuItems.length) {
          handleMenuSelect(filteredMenuItems[highlightedIndex].href);
        }
        break;
      case 'Escape':
        setIsSearchOpen(false);
        setHighlightedIndex(-1);
        searchInputRef.current?.blur();
        break;
    }
  };

  // 외부 클릭 감지
  React.useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        searchDropdownRef.current &&
        !searchDropdownRef.current.contains(event.target as Node) &&
        searchInputRef.current &&
        !searchInputRef.current.contains(event.target as Node)
      ) {
        setIsSearchOpen(false);
        setHighlightedIndex(-1);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="flex min-h-screen bg-gray-50">
      {/* Mobile menu button */}
      {!isSidebarOpen && (
        <div className="lg:hidden fixed top-4 left-4 z-50">
          <button
            onClick={() => setIsSidebarOpen(true)}
            className="p-2 rounded-lg bg-white shadow-md hover:shadow-lg transition-shadow"
          >
            <Bars3Icon className="h-6 w-6 text-gray-600" />
          </button>
        </div>
      )}

      {/* Sidebar backdrop - transparent, only for closing */}
      {isSidebarOpen && (
        <div
          className="lg:hidden fixed inset-0 z-30"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`
        fixed inset-y-0 left-0 z-40 w-72 bg-white shadow-xl transition-transform duration-300 ease-in-out h-screen overflow-y-auto
        ${isSidebarOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"
          }
      `}
      >
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="p-6 border-b border-gray-200">
            <div className="flex items-center">
              <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-indigo-600 rounded-xl flex items-center justify-center">
                <ShieldCheckIcon className="h-6 w-6 text-white" />
              </div>
              <div className="ml-3">
                <h1 className="text-xl font-bold text-gray-900">
                  내부통제시스템
                </h1>
                <p className="text-sm text-gray-600">관리 시스템</p>
              </div>
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex-1 p-6 space-y-2">
            {menuItems.map((item) => {
              const Icon = item.icon;

              // Parent menu with children
              if ('children' in item && item.children && item.id) {
                const isExpanded = expandedMenus.includes(item.id);
                const hasActiveChild = (item.children as any[]).some(
                  (child) => pathname === child.href || pathname.startsWith(child.href + '/')
                );

                return (
                  <div key={item.id}>
                    <button
                      onClick={() => toggleMenu(item.id)}
                      className={`
                        w-full group flex items-center px-4 py-3 rounded-xl transition-all duration-200 relative overflow-hidden
                        ${hasActiveChild
                          ? "bg-gradient-to-r " +
                          item.color +
                          " text-white shadow-lg"
                          : "text-gray-700 hover:bg-gray-100 hover:text-gray-900"
                        }
                      `}
                    >
                      <Icon
                        className={`
                        h-6 w-6 transition-transform duration-200
                        ${hasActiveChild
                            ? "text-white"
                            : "text-gray-500 group-hover:text-gray-700"
                          }
                      `}
                      />
                      <span className="ml-3 font-medium flex-1 text-left">
                        {item.name}
                      </span>
                      {isExpanded ? (
                        <ChevronDownIcon className="h-4 w-4" />
                      ) : (
                        <ChevronRightIcon className="h-4 w-4" />
                      )}
                    </button>

                    {/* Children items */}
                    {isExpanded && (
                      <div className="mt-2 ml-6 space-y-1">
                        {(item.children as any[]).map((child) => {
                          const ChildIcon = child.icon;
                          const isActive = pathname === child.href || pathname.startsWith(child.href + '/');

                          return (
                            <Link
                              key={child.href}
                              href={child.href}
                              className={`
                                group flex items-center px-4 py-2 rounded-lg transition-all duration-200 relative overflow-hidden
                                ${isActive
                                  ? "bg-gray-100 text-gray-900"
                                  : "text-gray-600 hover:bg-gray-50 hover:text-gray-900"
                                }
                              `}
                              onClick={() => setIsSidebarOpen(false)}
                            >
                              <ChildIcon
                                className={`
                                h-5 w-5 transition-transform duration-200
                                ${isActive
                                    ? "text-gray-700"
                                    : "text-gray-400 group-hover:text-gray-600"
                                  }
                              `}
                              />
                              <span className="ml-3 text-sm font-medium">
                                {child.name}
                              </span>
                            </Link>
                          );
                        })}
                      </div>
                    )}
                  </div>
                );
              }

              // Regular menu item (items without children must have href)
              if (!item.href) return null;

              const isActive = pathname === item.href || pathname.startsWith(item.href + '/');

              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`
                    group flex items-center px-4 py-3 rounded-xl transition-all duration-200 relative overflow-hidden
                    ${isActive
                      ? "bg-gradient-to-r " +
                      item.color +
                      " text-white shadow-lg transform scale-105"
                      : "text-gray-700 hover:bg-gray-100 hover:text-gray-900"
                    }
                  `}
                  onClick={() => setIsSidebarOpen(false)}
                >
                  <Icon
                    className={`
                    h-6 w-6 transition-transform duration-200
                    ${isActive
                        ? "text-white"
                        : "text-gray-500 group-hover:text-gray-700"
                      }
                    ${isActive ? "scale-110" : "group-hover:scale-110"}
                  `}
                  />
                  <span className="ml-3 font-medium">{item.name}</span>
                  {isActive && (
                    <div className="absolute inset-0 bg-white/10 rounded-xl"></div>
                  )}
                </Link>
              );
            })}
          </nav>

          {/* User section */}
          <div className="p-6 border-t border-gray-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <div className="w-8 h-8 bg-gradient-to-br from-gray-400 to-gray-600 rounded-full flex items-center justify-center">
                  <span className="text-white font-medium text-sm">U</span>
                </div>
                <div className="ml-3">
                  <p className="text-sm font-medium text-gray-900">사용자</p>
                  <p className="text-xs text-gray-600">관리자</p>
                </div>
              </div>
              <button
                onClick={handleLogout}
                className="p-2 rounded-lg bg-red-50 hover:bg-red-100 text-red-600 transition-colors duration-200 hover:scale-105 transform"
                title="로그아웃"
              >
                <ArrowRightOnRectangleIcon className="h-5 w-5" />
              </button>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0 lg:ml-72">
        {/* Header */}
        {/* [2026-04-24] 탭 바 포함 — 헤더 높이가 h-16 + TabBar(h-9) 로 확장됨 */}
        <header className="bg-white shadow-sm border-b border-gray-200 lg:pl-0 pl-16 sticky top-0 z-20">
          <div className="flex items-center justify-between h-16 px-6 border-b border-gray-100">
            {/* 검색 입력 */}
            <div className="flex-1 max-w-md relative">
              <div className="relative">
                <input
                  ref={searchInputRef}
                  type="text"
                  value={searchQuery}
                  onChange={handleSearchChange}
                  onFocus={handleSearchFocus}
                  onKeyDown={handleSearchKeyDown}
                  placeholder="메뉴 검색... (예: 대시보드, 룰 관리)"
                  className="w-full pl-10 pr-4 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                <MagnifyingGlassCircleIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
              </div>

              {/* 자동완성 드롭다운 */}
              {isSearchOpen && filteredMenuItems.length > 0 && (
                <div
                  ref={searchDropdownRef}
                  className="absolute top-full left-0 right-0 mt-2 bg-white border border-gray-200 rounded-lg shadow-lg z-50 max-h-96 overflow-y-auto"
                >
                  {filteredMenuItems.map((item, index) => (
                    <button
                      key={item.href}
                      onClick={() => handleMenuSelect(item.href)}
                      className={`
                        w-full text-left px-4 py-3 hover:bg-blue-50 transition-colors
                        ${index === highlightedIndex ? 'bg-blue-50' : ''}
                        ${index === 0 ? 'rounded-t-lg' : ''}
                        ${index === filteredMenuItems.length - 1 ? 'rounded-b-lg' : 'border-b border-gray-100'}
                      `}
                    >
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="text-sm font-medium text-gray-900">
                            {item.name}
                          </div>
                          {item.parentName && (
                            <div className="text-xs text-gray-500 mt-0.5">
                              {item.parentName} &gt; {item.name}
                            </div>
                          )}
                        </div>
                        <ChevronRightIcon className="h-4 w-4 text-gray-400" />
                      </div>
                    </button>
                  ))}
                </div>
              )}

              {/* 검색 결과 없음 */}
              {isSearchOpen && searchQuery.trim() && filteredMenuItems.length === 0 && (
                <div
                  ref={searchDropdownRef}
                  className="absolute top-full left-0 right-0 mt-2 bg-white border border-gray-200 rounded-lg shadow-lg z-50 p-4 text-center text-sm text-gray-500"
                >
                  검색 결과가 없습니다.
                </div>
              )}
            </div>

            {/* 시스템 상태 */}
            <div className="flex items-center space-x-4 ml-6">
              <div className="hidden sm:flex items-center space-x-2 text-sm text-gray-600">
                <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                <span>시스템 정상</span>
              </div>
            </div>
          </div>
          {/* [2026-04-24] 탭 바 */}
          <TabBar />
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-auto p-4 bg-gradient-to-br ">
          {children}
        </main>
      </div>
    </div>
  );
};

export default Layout;
