import React from "react";

interface LoadingButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean; // 로딩 상태 여부
  children: React.ReactNode; // 버튼 텍스트/내용
  spinnerClassName?: string; // 스피너 커스텀 클래스 (선택)
  size?: "xs" | "small" | "medium" | "large"; // 버튼 크기
  variant?: "primary" | "secondary" | "danger"; // 버튼 스타일 변형
}

// 공통 로딩 버튼 컴포넌트
const LoadingButton: React.FC<LoadingButtonProps> = ({
  loading = false,
  children,
  disabled,
  className = "",
  spinnerClassName = "",
  size = "medium",
  variant = "primary",
  ...props
}) => {
  // 크기별 스타일
  const sizeStyles = {
    xs: "px-2 py-1 text-xs",
    small: "px-3 py-1.5 text-sm",
    medium: "px-4 py-2 text-base",
    large: "px-6 py-3 text-lg",
  };

  // 변형별 스타일
  const variantStyles = {
    primary: "bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-400",
    secondary: "bg-gray-600 text-white hover:bg-gray-700 focus:ring-gray-400",
    danger: "bg-red-600 text-white hover:bg-red-700 focus:ring-red-500",
  };

  const baseStyles = "inline-flex items-center justify-center font-medium rounded-md shadow transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50";

  return (
    <button
      type="button"
      className={`${baseStyles} ${sizeStyles[size]} ${variantStyles[variant]} ${className}`}
      disabled={loading || disabled}
      {...props}
    >
      {loading && (
        <svg
          className={`animate-spin h-5 w-5 mr-2 text-white ${spinnerClassName}`}
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
          />
        </svg>
      )}
      {loading ? "로딩 중..." : children}
    </button>
  );
};

export default LoadingButton;
