"use client";

import { SparklesIcon } from "@heroicons/react/24/outline";

export default function AiMonitoringPage() {
  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">AI 모니터링</h1>
          <p className="text-sm text-gray-500 mt-1">AI 기반 이상탐지 및 분석 결과를 모니터링합니다</p>
        </div>
      </div>

      {/* Under Development Message */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-12">
        <div className="flex flex-col items-center justify-center text-center">
          <div className="w-20 h-20 bg-gradient-to-br from-violet-100 to-fuchsia-100 rounded-full flex items-center justify-center mb-6">
            <SparklesIcon className="h-10 w-10 text-violet-600" />
          </div>
          <h2 className="text-2xl font-bold text-gray-900 mb-3">개발중입니다</h2>
          <p className="text-gray-600 max-w-md">
            AI 기반 이상 패턴 탐지, 예측 분석, 자동 학습 기능이 곧 제공될 예정입니다.
          </p>
          <div className="mt-8 flex items-center gap-2 text-sm text-gray-500">
            <div className="w-2 h-2 bg-violet-400 rounded-full animate-pulse"></div>
            <span>Coming Soon</span>
          </div>
        </div>
      </div>

      {/* Preview Features */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="font-semibold text-gray-900 mb-2">🤖 이상 패턴 학습</h3>
          <p className="text-sm text-gray-600">
            머신러닝을 통해 새로운 이상 패턴을 자동으로 학습합니다
          </p>
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="font-semibold text-gray-900 mb-2">📊 예측 분석</h3>
          <p className="text-sm text-gray-600">
            과거 데이터를 기반으로 위험을 미리 예측합니다
          </p>
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="font-semibold text-gray-900 mb-2">⚡ 실시간 분석</h3>
          <p className="text-sm text-gray-600">
            실시간으로 AI 모델이 거래를 분석하고 평가합니다
          </p>
        </div>
      </div>
    </div>
  );
}
