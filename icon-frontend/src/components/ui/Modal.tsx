"use client";

import { XMarkIcon } from "@heroicons/react/24/outline";
import { ReactNode } from "react";

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  size?: "sm" | "md" | "lg" | "xl";
}

const sizeClasses = {
  sm: "max-w-sm",
  md: "max-w-md",
  lg: "max-w-lg",
  xl: "max-w-xl",
};

export function Modal({
  isOpen,
  onClose,
  title,
  children,
  size = "md"
}: ModalProps) {
  if (!isOpen) return null;

  // [2026-04-21] max-h-[90vh] + flex 구조로 헤더 고정, 본문 스크롤 처리
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className={`bg-white rounded-lg w-full ${sizeClasses[size]} mx-4 flex flex-col max-h-[90vh]`}>
        {/* 헤더 — 고정 */}
        <div className="flex justify-between items-center px-6 pt-6 pb-4 shrink-0 border-b border-gray-100">
          <h2 className="text-xl font-semibold text-gray-900">{title}</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-500 focus:outline-none"
          >
            <XMarkIcon className="h-6 w-6" />
          </button>
        </div>
        {/* 본문 — 스크롤 */}
        <div className="overflow-y-auto flex-1 px-6 py-5">
          {children}
        </div>
      </div>
    </div>
  );
}
