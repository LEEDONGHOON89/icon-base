"use client";

import { useState, useEffect, useRef } from "react";
import { runSimulation, SimulationResponse } from "../api";
import { fetchDataSources, DataSource } from "@/app/data-sources/api";
import { toast } from "react-hot-toast";
import { ChevronDownIcon, CheckIcon, QuestionMarkCircleIcon, XMarkIcon } from "@heroicons/react/24/outline";

type TabType = "datasource-select" | "single-run";

export default function SimulationPage() {
  const [activeTab, setActiveTab] = useState<TabType>("datasource-select");
  const [dataSources, setDataSources] = useState<DataSource[]>([]);
  const [selectedDataSourceId, setSelectedDataSourceId] = useState<string>("");
  const [jsonInput, setJsonInput] = useState<string>("");
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<SimulationResponse | SimulationResponse[] | null>(null);
  const [isHelpModalOpen, setIsHelpModalOpen] = useState(false);

  // Autocomplete 관련 state
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [selectedDisplayText, setSelectedDisplayText] = useState<string>("");
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // DataSource 목록 로딩
  useEffect(() => {
    fetchDataSources()
      .then((sources) => {
        setDataSources(sources);
        if (sources.length > 0) {
          setSelectedDataSourceId(sources[0].dataSourceId);
          const firstSource = sources[0];
          const displayText = `${firstSource.name} (${firstSource.sourceType} - ${firstSource.dataSourceId})`;
          setSelectedDisplayText(displayText);
        }
      })
      .catch((error) => {
        console.error("DataSource 로딩 실패:", error);
        toast.error("DataSource 목록을 불러올 수 없습니다.");
      });
  }, []);

  // 필터링된 DataSource 목록
  const filteredDataSources = dataSources.filter((ds) => {
    if (!searchQuery.trim()) return true; // 검색어 없으면 전체 표시
    const searchLower = searchQuery.toLowerCase();
    return (
      ds.name.toLowerCase().includes(searchLower) ||
      ds.dataSourceId.toLowerCase().includes(searchLower) ||
      ds.sourceType.toLowerCase().includes(searchLower)
    );
  });

  // 외부 클릭 감지
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsDropdownOpen(false);
        setHighlightedIndex(-1);
        setSearchQuery("");
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // DataSource 선택 핸들러
  const handleSelectDataSource = (ds: DataSource) => {
    setSelectedDataSourceId(ds.dataSourceId);
    const displayText = `${ds.name} (${ds.sourceType} - ${ds.dataSourceId})`;
    setSelectedDisplayText(displayText);
    setSearchQuery("");
    setIsDropdownOpen(false);
    setHighlightedIndex(-1);
  };

  // 키보드 네비게이션
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!isDropdownOpen) {
      if (e.key === "ArrowDown" || e.key === "Enter") {
        setIsDropdownOpen(true);
        e.preventDefault();
      }
      return;
    }

    switch (e.key) {
      case "ArrowDown":
        e.preventDefault();
        setHighlightedIndex((prev) =>
          prev < filteredDataSources.length - 1 ? prev + 1 : 0
        );
        break;
      case "ArrowUp":
        e.preventDefault();
        setHighlightedIndex((prev) =>
          prev > 0 ? prev - 1 : filteredDataSources.length - 1
        );
        break;
      case "Enter":
        e.preventDefault();
        if (highlightedIndex >= 0 && filteredDataSources[highlightedIndex]) {
          handleSelectDataSource(filteredDataSources[highlightedIndex]);
        }
        break;
      case "Escape":
        setIsDropdownOpen(false);
        setHighlightedIndex(-1);
        setSearchQuery("");
        inputRef.current?.blur();
        break;
    }
  };

  // JSON 예시 템플릿
  const handleLoadSample = () => {
    if (activeTab === "datasource-select") {
      // DataSource 선택 탭: 기존 샘플
      const sampleJson = {
        CUS_ID: "DEMO_CUS001",
        TRX_DT: "2025-08-20 02:00:00",
        TRX_TYPE: "비대면계좌개설",
        CUSTOMER_AGE: 68,
        TRX_AMT: 0,
        BAL_AMT: 50000000,
        BALANCE_BEFORE: 50000000,
        SENDER: "110-123-456789",
        RECEIVER: "",
        LOGIN_FAIL_CNT: 0,
        TRX_TIME: "02:00",
        ACCOUNT_OPEN_DATE: "2025-08-20",
        NON_FACE_YN: "Y",
        ACCESS_COUNTRY: "KR",
        ACCESS_IP: "192.168.1.100",
        DEVICE_ID: "DEVICE_DEMO001",
        ATM_WD_CNT: 0,
        IS_NIGHT_TIME: "Y",
        NEW_ACCOUNT_DAYS: 0,
        TRANSFER_TYPE: "",
      };
      setJsonInput(JSON.stringify(sampleJson, null, 2));
    } else {
      // 단일실행 탭: dataSourceId 포함 형식 (배열 지원)
      const sampleJson = [
        {
          dataSourceId: "DS_EMPLOYEE",
          data: [
            {
              hr_tx_id: "HR20251221001",
              employee_id: "EMP001",
              emp_name: "김직원",
              dept_name: "영업1팀",
              job_title: "과장",
              status: "ACTIVE",
              perm_level: 3,
              hire_date: "2020-01-15",
              last_update: "2025-12-21 09:00:00"
            },
            {
              hr_tx_id: "HR20251221002",
              employee_id: "EMP002",
              emp_name: "박직원",
              dept_name: "영업2팀",
              job_title: "과장",
              status: "ACTIVE",
              perm_level: 3,
              hire_date: "2019-03-20",
              last_update: "2025-12-21 09:00:00"
            }
          ]
        },
        {
          dataSourceId: "DS_XFER_APPR",
          data: [
            {
              transaction_id: "XFER_TEST_001",
              transaction_datetime: "2025-12-21 10:00:00",
              transaction_date: "2025-12-21",
              amount: 5000000,
              customer_id: "CUST_100",
              customer_name: "김고객",
              sender_account: "100-111-222333",
              receiver_account: "1001-234-567890",
              approver_id: "EMP001",
              approval_datetime: "2025-12-21 10:05:00"
            }
          ]
        }
      ];
      setJsonInput(JSON.stringify(sampleJson, null, 2));
    }
  };

  // 실행 버튼 클릭
  const handleExecute = async () => {
    if (!jsonInput.trim()) {
      toast.error("JSON 데이터를 입력하세요.");
      return;
    }

    try {
      const parsedJson = JSON.parse(jsonInput);
      setIsLoading(true);
      setResult(null);

      // 단일실행 탭: dataSourceId 포함 형식
      if (activeTab === "single-run") {
        if (!Array.isArray(parsedJson)) {
          toast.error("단일실행 탭에서는 배열 형식으로 입력해야 합니다. [{dataSourceId, data}, ...]");
          setIsLoading(false);
          return;
        }

        const results: SimulationResponse[] = [];
        let totalCount = 0;
        let processedCount = 0;

        // 1단계: 전체 건수 계산
        for (const item of parsedJson) {
          if (!item.dataSourceId || !item.data) {
            toast.error("dataSourceId 또는 data가 없는 항목이 있습니다.");
            setIsLoading(false);
            return;
          }
          totalCount += Array.isArray(item.data) ? item.data.length : 1;
        }

        // 2단계: 순차 실행
        for (const item of parsedJson) {
          const dataArray = Array.isArray(item.data) ? item.data : [item.data];

          for (const row of dataArray) {
            processedCount++;
            toast.loading(`${processedCount}/${totalCount} 전송 중... (${item.dataSourceId})`, { id: 'batch-progress' });

            const response = await runSimulation({
              dataSourceId: item.dataSourceId,
              executedBy: "SIMULATION_USER",
              row,
            });

            // 에러 체크
            if (!response.success) {
              toast.error(response.errorMessage || '시뮬레이션 실행 중 오류가 발생했습니다.');
            }

            results.push(response);
          }
        }

        toast.dismiss('batch-progress');
        setResult(results);
        alert(`${totalCount}건 시뮬레이션 실행 완료!`);
      } else {
        // DataSource 선택 탭: 기존 로직
        const dataSourceId = selectedDataSourceId;

        if (!dataSourceId) {
          toast.error("DataSource를 선택하세요.");
          setIsLoading(false);
          return;
        }

        // 배열 자동 감지
        if (Array.isArray(parsedJson)) {
          // 배열: 순차 전송
          const results: SimulationResponse[] = [];
          const total = parsedJson.length;

          for (let i = 0; i < total; i++) {
            const item = parsedJson[i];
            toast.loading(`${i + 1}/${total} 전송 중...`, { id: 'batch-progress' });

            const response = await runSimulation({
              dataSourceId,
              executedBy: "SIMULATION_USER",
              row: item,
            });

            // 에러 체크
            if (!response.success) {
              toast.error(response.errorMessage || '시뮬레이션 실행 중 오류가 발생했습니다.');
            }

            results.push(response);
          }

          toast.dismiss('batch-progress');
          setResult(results);
          alert(`${total}건 시뮬레이션 실행 완료!`);
        } else {
          // 단일 객체: 기존 로직
          const response = await runSimulation({
            dataSourceId,
            executedBy: "SIMULATION_USER",
            row: parsedJson,
          });

          // 에러 체크
          if (!response.success) {
            toast.error(response.errorMessage || '시뮬레이션 실행 중 오류가 발생했습니다.');
          } else {
            alert("시뮬레이션 실행 완료!");
          }

          setResult(response);
        }
      }
    } catch (error: any) {
      console.error("시뮬레이션 실행 실패:", error);
      if (error instanceof SyntaxError) {
        toast.error("JSON 형식이 올바르지 않습니다.");
      } else {
        toast.error(error.response?.data?.message || "시뮬레이션 실행에 실패했습니다.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">실시간 탐지 시뮬레이션</h1>
        <p className="text-sm text-gray-500 mt-1">
          JSON 이벤트 데이터를 전송하여 실시간 탐지를 테스트합니다.
        </p>
      </div>

      {/* 탭 네비게이션 */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex gap-x-8">
          <button
            onClick={() => setActiveTab("datasource-select")}
            className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${activeTab === "datasource-select"
              ? "border-blue-500 text-blue-600"
              : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
              }`}
          >
            DataSource 선택
          </button>
          <button
            onClick={() => setActiveTab("single-run")}
            className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${activeTab === "single-run"
              ? "border-blue-500 text-blue-600"
              : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
              }`}
          >
            단일실행
          </button>
        </nav>
      </div>

      {/* DataSource 선택 탭 */}
      {activeTab === "datasource-select" && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-4">
          <div className="relative" ref={dropdownRef}>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              DataSource 선택 <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <input
                ref={inputRef}
                type="text"
                value={isDropdownOpen ? searchQuery : selectedDisplayText}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setIsDropdownOpen(true);
                  setHighlightedIndex(-1);
                }}
                onFocus={() => {
                  setSearchQuery("");
                  setIsDropdownOpen(true);
                }}
                onKeyDown={handleKeyDown}
                placeholder="DataSource 검색..."
                className="w-full px-4 py-2 pr-10 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <ChevronDownIcon
                className={`absolute right-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400 transition-transform ${isDropdownOpen ? "rotate-180" : ""
                  }`}
              />
            </div>

            {/* Dropdown */}
            {isDropdownOpen && (
              <div className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-lg shadow-lg max-h-60 overflow-auto">
                {filteredDataSources.length === 0 ? (
                  <div className="px-4 py-3 text-sm text-gray-500">
                    검색 결과가 없습니다.
                  </div>
                ) : (
                  filteredDataSources.map((ds, index) => {
                    const isSelected = ds.dataSourceId === selectedDataSourceId;
                    const isHighlighted = index === highlightedIndex;

                    return (
                      <button
                        key={ds.dataSourceId}
                        onClick={() => handleSelectDataSource(ds)}
                        className={`w-full text-left px-4 py-2 flex items-center justify-between hover:bg-gray-100 transition-colors ${isHighlighted ? "bg-blue-50" : ""
                          } ${isSelected ? "bg-blue-50" : ""}`}
                      >
                        <div>
                          <div className="font-medium text-gray-900">
                            {ds.name}
                          </div>
                          <div className="text-sm text-gray-500">
                            {ds.sourceType} - {ds.dataSourceId}
                          </div>
                        </div>
                        {isSelected && (
                          <CheckIcon className="h-5 w-5 text-blue-600" />
                        )}
                      </button>
                    );
                  })
                )}
              </div>
            )}

            <p className="mt-1 text-xs text-gray-500">
              선택한 DataSource의 스키마 매핑이 적용됩니다. (화살표 키로 이동, Enter로 선택)
            </p>
          </div>

          {/* JSON 입력 */}
          <div>
            <div className="flex justify-between items-center mb-2">
              <label className="block text-sm font-medium text-gray-700">
                JSON 데이터 <span className="text-red-500">*</span>
              </label>

            </div>
            <textarea
              value={jsonInput}
              onChange={(e) => setJsonInput(e.target.value)}
              onKeyDown={(e) => {
                if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
                  e.preventDefault();
                  handleExecute();
                }
              }}
              placeholder='{"CUS_ID": "DEMO_CUS001", "TRX_DT": "2025-08-20 02:00:00", ...}'
              className="w-full h-96 px-4 py-3 border border-gray-300 rounded-lg font-mono text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <p className="mt-1 text-xs text-gray-500">
              CSV 컬럼명에 해당하는 JSON 키-값을 입력하세요. (단축키: <kbd className="px-1.5 py-0.5 bg-gray-100 border border-gray-300 rounded text-xs">⌘+Enter</kbd> 또는 <kbd className="px-1.5 py-0.5 bg-gray-100 border border-gray-300 rounded text-xs">Ctrl+Enter</kbd>)
            </p>
          </div>

          {/* 실행 버튼 */}
          <div className="flex justify-end gap-3">
            <button
              onClick={() => {
                setJsonInput("");
                setResult(null);
              }}
              className="px-6 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            >
              초기화
            </button>
            <button
              onClick={handleExecute}
              disabled={isLoading}
              className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
            >
              {isLoading ? (
                <>
                  <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                      fill="none"
                    />
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                    />
                  </svg>
                  실행 중...
                </>
              ) : (
                <>
                  🚀 시뮬레이션 실행
                </>
              )}
            </button>
          </div>
        </div>
      )}

      {/* 단일실행 탭 */}
      {activeTab === "single-run" && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-4">
          {/* JSON 입력 */}
          <div>
            <div className="flex justify-between items-center mb-2">
              <div className="flex items-center gap-2">
                <label className="block text-sm font-medium text-gray-700">
                  JSON 데이터 <span className="text-red-500">*</span>
                </label>
                <button
                  onClick={() => setIsHelpModalOpen(true)}
                  className="text-blue-600 hover:text-blue-700 transition-colors"
                  title="도움말 보기"
                >
                  <QuestionMarkCircleIcon className="h-5 w-5" />
                </button>
              </div>
            </div>
            <textarea
              value={jsonInput}
              onChange={(e) => setJsonInput(e.target.value)}
              onKeyDown={(e) => {
                if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
                  e.preventDefault();
                  handleExecute();
                }
              }}
              placeholder='[{ "dataSourceId": "DS_XXX", "data": [{...}, {...}] }]'
              className="w-full h-96 px-4 py-3 border border-gray-300 rounded-lg font-mono text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <p className="mt-1 text-xs text-gray-500">
              배열 형식으로 입력하세요. (단축키: <kbd className="px-1.5 py-0.5 bg-gray-100 border border-gray-300 rounded text-xs">⌘+Enter</kbd> 또는 <kbd className="px-1.5 py-0.5 bg-gray-100 border border-gray-300 rounded text-xs">Ctrl+Enter</kbd>)
            </p>
          </div>

          {/* 실행 버튼 */}
          <div className="flex justify-end gap-3">
            <button
              onClick={() => {
                setJsonInput("");
                setResult(null);
              }}
              className="px-6 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            >
              초기화
            </button>
            <button
              onClick={handleExecute}
              disabled={isLoading}
              className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
            >
              {isLoading ? (
                <>
                  <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                      fill="none"
                    />
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                    />
                  </svg>
                  실행 중...
                </>
              ) : (
                <>
                  🚀 시뮬레이션 실행
                </>
              )}
            </button>
          </div>
        </div>
      )}

      {/* 실행 결과 */}
      {result && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-4">
          <h2 className="text-lg font-semibold text-gray-900 border-b pb-2">
            실행 결과 {Array.isArray(result) && `(${result.length}건)`}
          </h2>

          {Array.isArray(result) ? (
            // 배열 결과: 합계 표시
            <>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-4">
                <div className="bg-blue-50 rounded-lg p-4">
                  <div className="text-sm text-blue-600 font-medium">전송 건수</div>
                  <div className="text-2xl font-bold text-blue-900 mt-1">
                    {result.length} 건
                  </div>
                </div>

                <div className="bg-green-50 rounded-lg p-4">
                  <div className="text-sm text-green-600 font-medium">Event Streams</div>
                  <div className="text-2xl font-bold text-green-900 mt-1">
                    {result.reduce((sum, r) => sum + r.savedEventStreams, 0)}
                  </div>
                </div>

                <div className="bg-teal-50 rounded-lg p-4">
                  <div className="text-sm text-teal-600 font-medium">Entity Attributes</div>
                  <div className="text-2xl font-bold text-teal-900 mt-1">
                    {result.reduce((sum, r) => sum + (r.updatedEntityAttributes ?? 0), 0)} 건
                  </div>
                </div>

                <div className="bg-purple-50 rounded-lg p-4">
                  <div className="text-sm text-purple-600 font-medium">센서 탐지</div>
                  <div className="text-2xl font-bold text-purple-900 mt-1">
                    {result.reduce((sum, r) => sum + (r.ruleResults ?? 0), 0)} 건
                  </div>
                </div>

                <div className="bg-yellow-50 rounded-lg p-4">
                  <div className="text-sm text-yellow-600 font-medium">룰 탐지</div>
                  <div className="text-2xl font-bold text-yellow-900 mt-1">
                    {result.reduce((sum, r) => sum + (r.savedAggregates ?? 0), 0)} 건
                  </div>
                </div>

                <div className="bg-red-50 rounded-lg p-4">
                  <div className="text-sm text-red-600 font-medium">시나리오 탐지</div>
                  <div className="text-2xl font-bold text-red-900 mt-1">
                    {result.reduce((sum, r) => sum + (r.savedScenarios ?? 0), 0)} 건
                  </div>
                </div>
              </div>

              {/* 개별 실행 결과 */}
              <div className="space-y-2">
                <div className="text-sm font-medium text-gray-700">개별 실행 결과</div>
                <div className="max-h-60 overflow-y-auto space-y-2">
                  {result.map((r, idx) => (
                    <div key={idx} className="bg-gray-50 rounded p-3 text-xs">
                      <div className="font-medium text-gray-700 mb-1">#{idx + 1} - Execution ID: {r.execDsMpId}</div>
                      <div className="text-gray-600 space-x-3">
                        <span>Event: {r.savedEventStreams ?? 0}</span>
                        <span>Entity: {r.updatedEntityAttributes ?? 0}</span>
                        <span>센서: {r.ruleResults ?? 0}</span>
                        <span>룰: {r.savedAggregates ?? 0}</span>
                        <span>시나리오: {r.savedScenarios ?? 0}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* 탐지 결과 요약 */}
              <div className="bg-gray-50 rounded-lg p-4">
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-sm font-medium text-gray-700">📊 탐지 요약</span>
                </div>
                <div className="text-sm text-gray-600 space-y-1">
                  <div>• 총 {result.length}건의 데이터를 순차적으로 전송했습니다.</div>
                  <div>• 전체 {result.reduce((sum, r) => sum + (r.ruleResults ?? 0) + (r.savedAggregates ?? 0) + (r.savedScenarios ?? 0), 0)}개의 탐지 이벤트가 생성되었습니다.</div>
                  <div>• 탐지 이력은 "탐지 모니터링 &gt; 실행 이력" 메뉴에서 확인할 수 있습니다.</div>
                </div>
              </div>
            </>
          ) : (
            // 단일 결과: 기존 로직
            <>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-4">
                <div className="bg-blue-50 rounded-lg p-4">
                  <div className="text-sm text-blue-600 font-medium">Execution ID</div>
                  <div className="text-2xl font-bold text-blue-900 mt-1">
                    {result.execDsMpId}
                  </div>
                </div>

                <div className="bg-green-50 rounded-lg p-4">
                  <div className="text-sm text-green-600 font-medium">Event Streams</div>
                  <div className="text-2xl font-bold text-green-900 mt-1">
                    {result.savedEventStreams ?? 0}
                  </div>
                </div>

                <div className="bg-teal-50 rounded-lg p-4">
                  <div className="text-sm text-teal-600 font-medium">Entity Attributes</div>
                  <div className="text-2xl font-bold text-teal-900 mt-1">
                    {result.updatedEntityAttributes ?? 0} 건
                  </div>
                </div>

                <div className="bg-purple-50 rounded-lg p-4">
                  <div className="text-sm text-purple-600 font-medium">센서 탐지</div>
                  <div className="text-2xl font-bold text-purple-900 mt-1">
                    {result.ruleResults ?? 0} 건
                  </div>
                </div>

                <div className="bg-yellow-50 rounded-lg p-4">
                  <div className="text-sm text-yellow-600 font-medium">룰 탐지</div>
                  <div className="text-2xl font-bold text-yellow-900 mt-1">
                    {result.savedAggregates ?? 0} 건
                  </div>
                </div>

                <div className="bg-red-50 rounded-lg p-4">
                  <div className="text-sm text-red-600 font-medium">시나리오 탐지</div>
                  <div className="text-2xl font-bold text-red-900 mt-1">
                    {result.savedScenarios ?? 0} 건
                  </div>
                </div>
              </div>

              {/* 탐지 결과 요약 */}
              <div className="bg-gray-50 rounded-lg p-4">
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-sm font-medium text-gray-700">📊 탐지 요약</span>
                </div>
                <div className="text-sm text-gray-600 space-y-1">
                  <div>• 총 {(result.ruleResults ?? 0) + (result.savedAggregates ?? 0) + (result.savedScenarios ?? 0)}개의 탐지 이벤트가 생성되었습니다.</div>
                  <div>• 탐지 이력은 "탐지 모니터링 &gt; 실행 이력" 메뉴에서 확인할 수 있습니다.</div>
                  <div>• Execution ID: {result.execDsMpId}로 상세 내역을 조회하세요.</div>
                </div>
              </div>
            </>
          )}
        </div>
      )}

      {/* 안내 정보 */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h3 className="text-sm font-medium text-blue-900 mb-2">💡 사용 안내</h3>
        {activeTab === "datasource-select" ? (
          <ul className="text-sm text-blue-800 space-y-1 list-disc list-inside">
            <li>DataSource를 선택하면 해당 스키마 매핑이 자동으로 적용됩니다.</li>
            <li>JSON 데이터는 CSV 컬럼명을 키로 사용합니다.</li>
            <li><strong>배열 지원</strong>: JSON 배열 <code>[{"{...}"}, {"{...}"}]</code> 입력 시 자동으로 순차 전송됩니다.</li>
            <li><strong>단축키</strong>: JSON 입력창에서 <kbd className="px-1.5 py-0.5 bg-white border border-blue-300 rounded text-xs">⌘+Enter</kbd> 또는 <kbd className="px-1.5 py-0.5 bg-white border border-blue-300 rounded text-xs">Ctrl+Enter</kbd>로 바로 실행할 수 있습니다.</li>
            <li>실행하면 landing_raw_records → mapped_storages → event_stream 순으로 저장됩니다.</li>
            <li>룰/집계/시나리오 탐지가 즉시 수행되며 결과가 반환됩니다.</li>
          </ul>
        ) : (
          <ul className="text-sm text-blue-800 space-y-1 list-disc list-inside">
            <li><strong>형식</strong>: <code>[{"{"} dataSourceId: "DS_XXX", data: [{"{"} ... {"}"}, {"{"} ... {"}"}] {"}"}]</code></li>
            <li>여러 DataSource의 데이터를 한 번에 입력하고 순차적으로 실행할 수 있습니다.</li>
            <li>각 항목은 <code>dataSourceId</code>와 <code>data</code> 필드를 포함해야 합니다.</li>
            <li><code>data</code>는 배열(여러 건) 또는 단일 객체(1건) 모두 가능합니다.</li>
            <li><strong>단축키</strong>: JSON 입력창에서 <kbd className="px-1.5 py-0.5 bg-white border border-blue-300 rounded text-xs">⌘+Enter</kbd> 또는 <kbd className="px-1.5 py-0.5 bg-white border border-blue-300 rounded text-xs">Ctrl+Enter</kbd>로 바로 실행할 수 있습니다.</li>
            <li>실행하면 각 DataSource별로 스키마 매핑이 적용됩니다.</li>
            <li>룰/집계/시나리오 탐지가 즉시 수행되며 결과가 반환됩니다.</li>
          </ul>
        )}
      </div>

      {/* 도움말 모달 */}
      {isHelpModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[80vh] overflow-y-auto">
            {/* 모달 헤더 */}
            <div className="sticky top-0 bg-white border-b px-6 py-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900">💡 단일실행 모드 안내</h2>
              <button
                onClick={() => setIsHelpModalOpen(false)}
                className="text-gray-400 hover:text-gray-600 transition-colors"
              >
                <XMarkIcon className="h-6 w-6" />
              </button>
            </div>

            {/* 모달 본문 */}
            <div className="px-6 py-4 space-y-4">
              <div className="space-y-3">
                <h3 className="text-sm font-semibold text-gray-900">📖 개요</h3>
                <p className="text-sm text-gray-700">
                  여러 DataSource의 데이터를 한 번에 입력하고 순차적으로 실행할 수 있습니다.
                </p>
              </div>

              <div className="space-y-3">
                <h3 className="text-sm font-semibold text-gray-900">📝 JSON 형식</h3>
                <div className="bg-gray-50 rounded-lg p-4">
                  <pre className="text-xs font-mono text-gray-800 overflow-x-auto">
{`[
  {
    "dataSourceId": "DS_EMPLOYEE",
    "data": [
      {
        "hr_tx_id": "HR20251221001",
        "employee_id": "EMP001",
        "emp_name": "김직원",
        ...
      },
      {
        "hr_tx_id": "HR20251221002",
        "employee_id": "EMP002",
        "emp_name": "박직원",
        ...
      }
    ]
  },
  {
    "dataSourceId": "DS_XFER_APPR",
    "data": [
      {
        "transaction_id": "XFER_001",
        "amount": 5000000,
        ...
      }
    ]
  }
]`}
                  </pre>
                </div>
              </div>

              <div className="space-y-3">
                <h3 className="text-sm font-semibold text-gray-900">✅ 규칙</h3>
                <ul className="text-sm text-gray-700 space-y-2 list-disc list-inside">
                  <li>최상위는 반드시 <code className="bg-gray-100 px-1 py-0.5 rounded text-xs">배열</code> 형식이어야 합니다.</li>
                  <li>각 항목은 <code className="bg-gray-100 px-1 py-0.5 rounded text-xs">dataSourceId</code>와 <code className="bg-gray-100 px-1 py-0.5 rounded text-xs">data</code> 필드를 포함해야 합니다.</li>
                  <li><code className="bg-gray-100 px-1 py-0.5 rounded text-xs">data</code>는 배열(여러 건) 또는 단일 객체(1건) 모두 가능합니다.</li>
                  <li>각 DataSource별로 스키마 매핑이 자동으로 적용됩니다.</li>
                  <li>입력한 순서대로 순차적으로 전송됩니다.</li>
                </ul>
              </div>

              <div className="space-y-3">
                <h3 className="text-sm font-semibold text-gray-900">⚡ 단축키</h3>
                <p className="text-sm text-gray-700">
                  JSON 입력창에서 <kbd className="px-2 py-1 bg-gray-100 border border-gray-300 rounded text-xs font-mono">⌘+Enter</kbd> 또는 <kbd className="px-2 py-1 bg-gray-100 border border-gray-300 rounded text-xs font-mono">Ctrl+Enter</kbd>를 눌러 바로 실행할 수 있습니다.
                </p>
              </div>

              <div className="space-y-3">
                <h3 className="text-sm font-semibold text-gray-900">🎯 실행 흐름</h3>
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-gray-700">
                  <ol className="space-y-1 list-decimal list-inside">
                    <li>landing_raw_records 저장</li>
                    <li>mapped_storages 매핑</li>
                    <li>event_stream 생성</li>
                    <li>룰/집계/시나리오 탐지 수행</li>
                    <li>결과 반환</li>
                  </ol>
                </div>
              </div>
            </div>

            {/* 모달 푸터 */}
            <div className="sticky bottom-0 bg-gray-50 px-6 py-4 border-t">
              <button
                onClick={() => setIsHelpModalOpen(false)}
                className="w-full px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
