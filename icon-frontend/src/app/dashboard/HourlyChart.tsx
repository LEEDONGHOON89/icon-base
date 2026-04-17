"use client";

import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import { HourlyStats } from "./api";

interface HourlyChartProps {
  data: HourlyStats[];
  loading?: boolean;
}

export default function HourlyChart({ data, loading }: HourlyChartProps) {
  if (loading) {
    return (
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <div className="animate-pulse">
          <div className="h-6 bg-gray-200 rounded w-1/4 mb-4"></div>
          <div className="h-64 bg-gray-100 rounded"></div>
        </div>
      </div>
    );
  }

  // 시간 라벨 포맷 (00시, 01시, ...)
  const chartData = data.map((item) => ({
    ...item,
    hourLabel: `${item.hour.toString().padStart(2, "0")}시`,
  }));

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
      <h3 className="text-lg font-semibold text-gray-800 mb-4">
        시간대별 거래/탐지 현황
      </h3>
      <div className="h-72">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={chartData}
            margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
          >
            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
            <XAxis
              dataKey="hourLabel"
              tick={{ fontSize: 11, fill: "#6b7280" }}
              tickLine={false}
              axisLine={{ stroke: "#e5e7eb" }}
            />
            <YAxis
              tick={{ fontSize: 11, fill: "#6b7280" }}
              tickLine={false}
              axisLine={{ stroke: "#e5e7eb" }}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: "white",
                border: "1px solid #e5e7eb",
                borderRadius: "8px",
                boxShadow: "0 4px 6px -1px rgb(0 0 0 / 0.1)",
              }}
              labelStyle={{ fontWeight: "bold", marginBottom: "4px" }}
              formatter={(value: number, name: string) => [
                value.toLocaleString() + "건",
                name === "transactionCount" ? "거래" : "탐지",
              ]}
            />
            <Legend
              formatter={(value) =>
                value === "transactionCount" ? "거래 건수" : "탐지 건수"
              }
              wrapperStyle={{ paddingTop: "10px" }}
            />
            <Bar
              dataKey="transactionCount"
              fill="#3b82f6"
              radius={[4, 4, 0, 0]}
              name="transactionCount"
            />
            <Bar
              dataKey="detectionCount"
              fill="#ef4444"
              radius={[4, 4, 0, 0]}
              name="detectionCount"
            />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
