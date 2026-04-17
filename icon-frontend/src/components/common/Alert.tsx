import React from "react";

interface AlertProps {
  message: string; // 표시할 메시지
  type?: "error" | "success" | "info" | "warning"; // Alert 타입 (기본값: error)
  className?: string; // 추가 커스텀 클래스
}

// 타입별 색상 매핑
const typeStyles = {
  error: {
    bg: "bg-red-100",
    border: "border border-red-600",
    text: "text-red-800",
    icon: "text-red-600",
    iconSvg: (
      <svg
        className="w-6 h-6 text-red-600 flex-shrink-0"
        fill="currentColor"
        viewBox="0 0 20 20"
        aria-hidden="true"
      >
        <path
          fillRule="evenodd"
          d="M18 10A8 8 0 11 2 10a8 8 0 0116 0zm-8-4a1 1 0 00-1 1v3a1 1 0 002 0V7a1 1 0 00-1-1zm0 8a1.5 1.5 0 100-3 1.5 1.5 0 000 3z"
          clipRule="evenodd"
        />
      </svg>
    ),
  },
  success: {
    bg: "bg-green-100",
    border: "border border-green-600",
    text: "text-green-800",
    icon: "text-green-600",
    iconSvg: (
      <svg
        className="w-6 h-6 text-green-600 flex-shrink-0"
        fill="currentColor"
        viewBox="0 0 20 20"
        aria-hidden="true"
      >
        <path
          fillRule="evenodd"
          d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-8.707a1 1 0 00-1.414-1.414L9 11.586 7.707 10.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
          clipRule="evenodd"
        />
      </svg>
    ),
  },
  info: {
    bg: "bg-blue-100",
    border: "border border-blue-600",
    text: "text-blue-800",
    icon: "text-blue-600",
    iconSvg: (
      <svg
        className="w-6 h-6 text-blue-600 flex-shrink-0"
        fill="currentColor"
        viewBox="0 0 20 20"
        aria-hidden="true"
      >
        <path
          fillRule="evenodd"
          d="M18 10A8 8 0 11 2 10a8 8 0 0116 0zm-8-4a1 1 0 100 2 1 1 0 000-2zm2 4a1 1 0 00-2 0v4a1 1 0 002 0v-4z"
          clipRule="evenodd"
        />
      </svg>
    ),
  },
  warning: {
    bg: "bg-yellow-100",
    border: "border border-yellow-600",
    text: "text-yellow-800",
    icon: "text-yellow-600",
    iconSvg: (
      <svg
        className="w-6 h-6 text-yellow-600 flex-shrink-0"
        fill="currentColor"
        viewBox="0 0 20 20"
        aria-hidden="true"
      >
        <path
          fillRule="evenodd"
          d="M8.257 3.099c.765-1.36 2.72-1.36 3.485 0l6.518 11.591c.75 1.334-.213 2.987-1.742 2.987H3.48c-1.53 0-2.492-1.653-1.742-2.987L8.257 3.1zM11 14a1 1 0 11-2 0 1 1 0 012 0zm-1-2a1 1 0 01-1-1V9a1 1 0 112 0v2a1 1 0 01-1 1z"
          clipRule="evenodd"
        />
      </svg>
    ),
  },
};

const Alert: React.FC<AlertProps> = ({
  message,
  type = "error",
  className,
}) => {
  const style = typeStyles[type] || typeStyles.error;
  return (
    <div
      className={`flex items-center gap-3 ${style.bg} ${style.border} ${
        style.text
      } px-3 py-2 rounded-lg shadow-md font-medium ${className || ""}`}
      role="alert"
    >
      {/* 타입별 아이콘 */}
      {style.iconSvg}
      <span className="flex-1">{message}</span>
    </div>
  );
};

export default Alert;
