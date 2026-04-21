// [2026-04-21] max-h-[90vh] + flex 구조로 헤더 고정, 본문 스크롤 처리 (tall prop 제거)
import { ReactNode, useEffect, useRef } from "react";

interface ModalProps {
  open: boolean;
  onClose: () => void;
  children: ReactNode;
  title?: string;
  size?: "sm" | "md" | "lg" | "xl" | "2xl";
  tall?: boolean; // [2026-04-21] deprecated — 항상 스크롤 적용되므로 무시됨
}

export default function Modal({ open, onClose, children, title, size = "md" }: ModalProps) {
  const ref = useRef<HTMLDivElement>(null);

  const sizeClasses = {
    sm: "max-w-sm",
    md: "max-w-md",
    lg: "max-w-2xl",
    xl: "max-w-4xl",
    "2xl": "max-w-6xl",
  };

  // ESC key close
  useEffect(() => {
    if (!open) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [open, onClose]);

  // Outside click close
  useEffect(() => {
    if (!open) return;
    const handleClick = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        onClose();
      }
    };
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div
        ref={ref}
        className={`bg-white rounded-xl shadow-2xl w-full ${sizeClasses[size]} flex flex-col max-h-[90vh] animate-fadeIn`}
        role="dialog"
        aria-modal="true"
      >
        {/* 헤더 — 고정 */}
        <div className="flex items-center justify-between px-8 pt-6 pb-4 shrink-0 border-b border-gray-100">
          {title
            ? <h2 className="text-xl font-bold text-gray-900">{title}</h2>
            : <span />
          }
          <button
            className="text-gray-400 hover:text-gray-600 text-2xl font-bold focus:outline-none leading-none"
            onClick={onClose}
            aria-label="닫기"
            type="button"
          >
            ×
          </button>
        </div>
        {/* 본문 — 스크롤 */}
        <div className="overflow-y-auto flex-1 px-8 py-6">
          {children}
        </div>
      </div>
    </div>
  );
}
