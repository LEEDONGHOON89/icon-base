"use client";

import { useState, useRef, useEffect, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  SparklesIcon,
  PaperAirplaneIcon,
  UserCircleIcon,
  LightBulbIcon,
  BookOpenIcon,
  CogIcon,
  QuestionMarkCircleIcon,
  PlusIcon,
  TrashIcon,
  ChatBubbleLeftRightIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  PencilIcon,
} from "@heroicons/react/24/outline";
import {
  sendMessage,
  getSessions,
  getSessionDetail,
  createSession,
  deleteSession,
  updateSessionTitle,
  sendRuleSnapshot,
  Message,
  SessionSummary,
} from "./api";
import { AcademicCapIcon } from "@heroicons/react/24/outline";

const QUICK_QUESTIONS = [
  {
    icon: BookOpenIcon,
    title: "시스템 개요",
    question: "이 시스템의 전체 구조와 주요 기능을 설명해주세요.",
  },
  {
    icon: CogIcon,
    title: "탐지영역 설정",
    question: "탐지영역을 설정하는 방법과 주요 개념을 알려주세요.",
  },
  {
    icon: LightBulbIcon,
    title: "룰 작성 가이드",
    question: "효과적인 탐지 룰을 작성하는 방법과 팁을 알려주세요.",
  },
  {
    icon: QuestionMarkCircleIcon,
    title: "시나리오 설정",
    question: "시나리오를 생성하고 관리하는 방법을 설명해주세요.",
  },
];

interface LocalMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  timestamp: Date;
  isNew?: boolean; // 새로 추가된 메시지인지 (애니메이션용)
}

// 타이핑 애니메이션 컴포넌트
function TypingMessage({
  content,
  onComplete,
  onTyping
}: {
  content: string;
  onComplete: () => void;
  onTyping?: () => void;
}) {
  const [displayedText, setDisplayedText] = useState("");
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    if (currentIndex < content.length) {
      const timeout = setTimeout(() => {
        // 한 번에 여러 글자씩 추가 (빠른 속도)
        const charsToAdd = Math.min(3, content.length - currentIndex);
        setDisplayedText(content.slice(0, currentIndex + charsToAdd));
        setCurrentIndex(currentIndex + charsToAdd);
        onTyping?.(); // 타이핑할 때마다 스크롤
      }, 10); // 10ms마다 3글자씩 = 매우 빠른 타이핑
      return () => clearTimeout(timeout);
    } else {
      onComplete();
    }
  }, [currentIndex, content, onComplete, onTyping]);

  return (
    <div className="prose prose-sm max-w-none">
      <div dangerouslySetInnerHTML={{ __html: formatMarkdown(displayedText) }} />
      {currentIndex < content.length && (
        <span className="inline-block w-0.5 h-4 bg-indigo-500 animate-pulse ml-0.5" />
      )}
    </div>
  );
}

type TabType = "chat" | "learning";

export default function AISupportPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<TabType>("chat");
  const [inputValue, setInputValue] = useState("");
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);
  const [localMessages, setLocalMessages] = useState<LocalMessage[]>([]);
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [editingTitle, setEditingTitle] = useState<string | null>(null);
  const [newTitle, setNewTitle] = useState("");
  const [typingMessageId, setTypingMessageId] = useState<string | null>(null); // 현재 타이핑 중인 메시지 ID
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  // 세션 목록 조회
  const sessionsQuery = useQuery({
    queryKey: ["ai-chat-sessions"],
    queryFn: getSessions,
  });

  // 세션 상세 조회
  const sessionDetailQuery = useQuery({
    queryKey: ["ai-chat-session", currentSessionId],
    queryFn: () => getSessionDetail(currentSessionId!),
    enabled: !!currentSessionId,
  });

  // 메시지 전송 뮤테이션
  const sendMessageMutation = useMutation({
    mutationFn: sendMessage,
    onSuccess: (data) => {
      const newMessageId = `assistant-${Date.now()}`;
      // AI 응답을 로컬 메시지에 추가 (타이핑 애니메이션 시작)
      setLocalMessages((prev) => [
        ...prev,
        {
          id: newMessageId,
          role: "assistant",
          content: data.response,
          timestamp: new Date(),
          isNew: true, // 새 메시지 표시
        },
      ]);
      setTypingMessageId(newMessageId); // 타이핑 애니메이션 시작
      // 새 세션이면 세션 ID 저장
      if (!currentSessionId) {
        setCurrentSessionId(data.sessionId);
      }
      // 세션 목록 갱신
      queryClient.invalidateQueries({ queryKey: ["ai-chat-sessions"] });
    },
    onError: (error) => {
      // 에러 시 에러 메시지 추가
      setLocalMessages((prev) => [
        ...prev,
        {
          id: `error-${Date.now()}`,
          role: "assistant",
          content: "메시지 전송에 실패했습니다. 다시 시도해주세요.",
          timestamp: new Date(),
        },
      ]);
    },
  });

  // 새 세션 생성 뮤테이션
  const createSessionMutation = useMutation({
    mutationFn: createSession,
    onSuccess: (data) => {
      setCurrentSessionId(data.sessionId);
      setLocalMessages([]);
      queryClient.invalidateQueries({ queryKey: ["ai-chat-sessions"] });
    },
  });

  // 세션 삭제 뮤테이션
  const deleteSessionMutation = useMutation({
    mutationFn: deleteSession,
    onSuccess: () => {
      if (currentSessionId) {
        setCurrentSessionId(null);
        setLocalMessages([]);
      }
      queryClient.invalidateQueries({ queryKey: ["ai-chat-sessions"] });
    },
  });

  // 세션 제목 업데이트 뮤테이션
  const updateTitleMutation = useMutation({
    mutationFn: ({ sessionId, title }: { sessionId: string; title: string }) =>
      updateSessionTitle(sessionId, title),
    onSuccess: () => {
      setEditingTitle(null);
      queryClient.invalidateQueries({ queryKey: ["ai-chat-sessions"] });
    },
  });

  // 룰 스냅샷 전송 뮤테이션
  const ruleSnapshotMutation = useMutation({
    mutationFn: sendRuleSnapshot,
  });

  // 세션 상세 데이터를 로컬 메시지로 변환
  useEffect(() => {
    if (sessionDetailQuery.data) {
      setLocalMessages(
        sessionDetailQuery.data.messages.map((msg) => ({
          id: `${msg.role}-${msg.id}`,
          role: msg.role,
          content: msg.content,
          timestamp: new Date(msg.createdAt),
        }))
      );
    }
  }, [sessionDetailQuery.data]);

  // 스크롤 관리
  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [localMessages, scrollToBottom]);

  const handleSendMessage = async (content: string) => {
    if (!content.trim() || sendMessageMutation.isPending) return;

    // 사용자 메시지를 로컬에 먼저 추가
    const userMessage: LocalMessage = {
      id: `user-${Date.now()}`,
      role: "user",
      content: content.trim(),
      timestamp: new Date(),
    };
    setLocalMessages((prev) => [...prev, userMessage]);
    setInputValue("");

    // API 호출
    sendMessageMutation.mutate({
      sessionId: currentSessionId || undefined,
      message: content.trim(),
    });
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage(inputValue);
    }
  };

  const handleNewChat = () => {
    setCurrentSessionId(null);
    setLocalMessages([]);
  };

  const handleSelectSession = (sessionId: string) => {
    setCurrentSessionId(sessionId);
  };

  const handleDeleteSession = (e: React.MouseEvent, sessionId: string) => {
    e.stopPropagation();
    if (confirm("이 대화를 삭제하시겠습니까?")) {
      deleteSessionMutation.mutate(sessionId);
    }
  };

  const handleStartEditTitle = (e: React.MouseEvent, session: SessionSummary) => {
    e.stopPropagation();
    setEditingTitle(session.sessionId);
    setNewTitle(session.title || "");
  };

  const handleSaveTitle = (sessionId: string) => {
    if (newTitle.trim()) {
      updateTitleMutation.mutate({ sessionId, title: newTitle.trim() });
    } else {
      setEditingTitle(null);
    }
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (days === 0) {
      return date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
    } else if (days === 1) {
      return "어제";
    } else if (days < 7) {
      return `${days}일 전`;
    } else {
      return date.toLocaleDateString("ko-KR", { month: "short", day: "numeric" });
    }
  };

  return (
    <div className="flex flex-col h-[calc(100vh-120px)] bg-gradient-to-br from-slate-50 to-indigo-50">
      {/* 헤더 - 전체 너비 */}
      <div className="bg-white border-b border-gray-200 px-6 py-4">
        <div className="flex items-center justify-between">
          {/* 제목 영역 */}
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-xl flex items-center justify-center">
              <SparklesIcon className="h-6 w-6 text-white" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-gray-800">AI 서포트</h1>
              <p className="text-sm text-gray-500">시스템 설정과 사용법에 대해 질문하세요</p>
            </div>
          </div>
          {/* 탭 - 오른쪽 끝 */}
          <div className="flex bg-gray-100 rounded-lg p-1">
          <button
            onClick={() => setActiveTab("chat")}
            className={`flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-all ${
              activeTab === "chat"
                ? "bg-white text-indigo-600 shadow-sm"
                : "text-gray-600 hover:text-gray-800"
            }`}
          >
            <ChatBubbleLeftRightIcon className="h-4 w-4" />
            Chat
          </button>
          <button
            onClick={() => setActiveTab("learning")}
            className={`flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-all ${
              activeTab === "learning"
                ? "bg-white text-indigo-600 shadow-sm"
                : "text-gray-600 hover:text-gray-800"
            }`}
          >
            <AcademicCapIcon className="h-4 w-4" />
            AI학습
          </button>
          </div>
        </div>
      </div>

      {/* 콘텐츠 영역 */}
      <div className="flex-1 flex overflow-hidden">
        {/* Chat 탭 콘텐츠 */}
        {activeTab === "chat" && (
          <>
            {/* 사이드바 - Chat 탭 콘텐츠 영역 안에 위치 */}
            <div
              className={`${
                isSidebarOpen ? "w-72" : "w-0"
              } transition-all duration-300 bg-white border-r border-gray-200 flex flex-col overflow-hidden`}
            >
              {isSidebarOpen && (
                <>
                  {/* 새 대화 버튼 */}
                  <div className="p-4 border-b border-gray-100">
                    <button
                      onClick={handleNewChat}
                      className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-gradient-to-r from-indigo-500 to-purple-600 text-white rounded-xl hover:shadow-lg transition-all duration-200"
                    >
                      <PlusIcon className="h-5 w-5" />
                      새 대화
                    </button>
                  </div>

                  {/* 세션 목록 */}
                  <div className="flex-1 overflow-y-auto p-2">
                    <p className="px-3 py-2 text-xs font-medium text-gray-500 uppercase">
                      대화 기록
                    </p>
                    {sessionsQuery.isLoading ? (
                      <div className="flex items-center justify-center py-8">
                        <div className="animate-spin w-5 h-5 border-2 border-indigo-500 border-t-transparent rounded-full" />
                      </div>
                    ) : sessionsQuery.data?.length === 0 ? (
                      <p className="text-center text-gray-400 text-sm py-8">
                        대화 기록이 없습니다
                      </p>
                    ) : (
                      <div className="space-y-1">
                        {sessionsQuery.data?.map((session) => (
                          <div
                            key={session.sessionId}
                            onClick={() => handleSelectSession(session.sessionId)}
                            className={`group flex items-center gap-2 px-3 py-2 rounded-lg cursor-pointer transition-colors ${
                              currentSessionId === session.sessionId
                                ? "bg-indigo-50 text-indigo-700"
                                : "hover:bg-gray-100 text-gray-700"
                            }`}
                          >
                            <ChatBubbleLeftRightIcon className="h-4 w-4 flex-shrink-0" />
                            <div className="flex-1 min-w-0">
                              {editingTitle === session.sessionId ? (
                                <input
                                  type="text"
                                  value={newTitle}
                                  onChange={(e) => setNewTitle(e.target.value)}
                                  onBlur={() => handleSaveTitle(session.sessionId)}
                                  onKeyDown={(e) => {
                                    if (e.key === "Enter") handleSaveTitle(session.sessionId);
                                    if (e.key === "Escape") setEditingTitle(null);
                                  }}
                                  onClick={(e) => e.stopPropagation()}
                                  className="w-full px-1 py-0.5 text-sm border border-indigo-300 rounded focus:outline-none focus:ring-1 focus:ring-indigo-500"
                                  autoFocus
                                />
                              ) : (
                                <>
                                  <p className="text-sm font-medium truncate">
                                    {session.title || "새 대화"}
                                  </p>
                                  <p className="text-xs text-gray-400">
                                    {formatDate(session.updatedAt)}
                                  </p>
                                </>
                              )}
                            </div>
                            <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                              <button
                                onClick={(e) => handleStartEditTitle(e, session)}
                                className="p-1 hover:bg-gray-200 rounded"
                                title="제목 수정"
                              >
                                <PencilIcon className="h-3.5 w-3.5 text-gray-500" />
                              </button>
                              <button
                                onClick={(e) => handleDeleteSession(e, session.sessionId)}
                                className="p-1 hover:bg-red-100 rounded"
                                title="삭제"
                              >
                                <TrashIcon className="h-3.5 w-3.5 text-red-500" />
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </>
              )}
            </div>

            {/* 사이드바 토글 버튼 */}
            <button
              onClick={() => setIsSidebarOpen(!isSidebarOpen)}
              className="absolute z-10 bg-white border border-gray-200 rounded-r-lg p-1 hover:bg-gray-50 transition-colors shadow-sm"
              style={{
                left: isSidebarOpen ? "288px" : "0",
                top: "50%",
                transform: "translateY(-50%)"
              }}
            >
              {isSidebarOpen ? (
                <ChevronLeftIcon className="h-4 w-4 text-gray-500" />
              ) : (
                <ChevronRightIcon className="h-4 w-4 text-gray-500" />
              )}
            </button>

            {/* 메인 채팅 영역 */}
            <div className="flex-1 flex flex-col overflow-hidden">
              {/* 메시지 영역 */}
              <div className="flex-1 overflow-y-auto p-6 space-y-6">
                {localMessages.length === 0 ? (
                  <div className="h-full flex flex-col items-center justify-center">
                    <div className="w-20 h-20 bg-gradient-to-br from-indigo-100 to-purple-100 rounded-3xl flex items-center justify-center mb-6">
                      <SparklesIcon className="h-10 w-10 text-indigo-500" />
                    </div>
                    <h2 className="text-xl font-semibold text-gray-800 mb-2">
                      무엇을 도와드릴까요?
                    </h2>
                    <p className="text-gray-500 text-center mb-8 max-w-md">
                      시스템 설정, 룰 작성, 시나리오 관리 등 궁금한 점을 자유롭게 질문해주세요.
                    </p>

                    {/* 빠른 질문 버튼 */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 w-full max-w-lg">
                      {QUICK_QUESTIONS.map((item, index) => (
                        <button
                          key={index}
                          onClick={() => handleSendMessage(item.question)}
                          className="flex items-center gap-3 p-4 bg-white hover:bg-indigo-50 rounded-xl text-left transition-colors group border border-gray-100 hover:border-indigo-200"
                        >
                          <div className="w-10 h-10 bg-gray-50 group-hover:bg-white rounded-lg flex items-center justify-center shadow-sm group-hover:shadow">
                            <item.icon className="h-5 w-5 text-indigo-500" />
                          </div>
                          <span className="text-sm font-medium text-gray-700 group-hover:text-indigo-700">
                            {item.title}
                          </span>
                        </button>
                      ))}
                    </div>
                  </div>
                ) : (
                  <>
                    {localMessages.map((message) => (
                      <div
                        key={message.id}
                        className={`flex gap-4 ${
                          message.role === "user" ? "flex-row-reverse" : ""
                        }`}
                      >
                        {/* 아바타 */}
                        <div
                          className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${
                            message.role === "user"
                              ? "bg-gradient-to-br from-blue-500 to-blue-600"
                              : "bg-gradient-to-br from-indigo-500 to-purple-600"
                          }`}
                        >
                          {message.role === "user" ? (
                            <UserCircleIcon className="h-6 w-6 text-white" />
                          ) : (
                            <SparklesIcon className="h-6 w-6 text-white" />
                          )}
                        </div>

                        {/* 메시지 내용 */}
                        <div className={`max-w-[75%] ${message.role === "user" ? "text-right" : ""}`}>
                          <div
                            className={`inline-block px-5 py-3 rounded-2xl ${
                              message.role === "user"
                                ? "bg-blue-500 text-white"
                                : "bg-white text-gray-800 shadow-sm border border-gray-100"
                            }`}
                          >
                            {message.role === "assistant" ? (
                              // 새 메시지면 타이핑 애니메이션, 아니면 일반 렌더링
                              typingMessageId === message.id ? (
                                <TypingMessage
                                  content={message.content}
                                  onComplete={() => {
                                    setTypingMessageId(null);
                                    // isNew 플래그 제거
                                    setLocalMessages((prev) =>
                                      prev.map((m) =>
                                        m.id === message.id ? { ...m, isNew: false } : m
                                      )
                                    );
                                  }}
                                  onTyping={scrollToBottom}
                                />
                              ) : (
                                <div className="prose prose-sm max-w-none">
                                  <div dangerouslySetInnerHTML={{ __html: formatMarkdown(message.content) }} />
                                </div>
                              )
                            ) : (
                              <p className="whitespace-pre-wrap text-left">{message.content}</p>
                            )}
                          </div>
                          <p className="text-xs text-gray-400 mt-1">
                            {message.timestamp.toLocaleTimeString("ko-KR", {
                              hour: "2-digit",
                              minute: "2-digit",
                            })}
                          </p>
                        </div>
                      </div>
                    ))}

                    {/* 로딩 인디케이터 */}
                    {sendMessageMutation.isPending && (
                      <div className="flex gap-4">
                        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center">
                          <SparklesIcon className="h-6 w-6 text-white" />
                        </div>
                        <div className="bg-white rounded-2xl px-5 py-3 shadow-sm border border-gray-100">
                          <div className="flex gap-1">
                            <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: "0ms" }} />
                            <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: "150ms" }} />
                            <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: "300ms" }} />
                          </div>
                        </div>
                      </div>
                    )}
                    <div ref={messagesEndRef} />
                  </>
                )}
              </div>

              {/* 입력 영역 */}
              <div className="bg-white border-t border-gray-200 p-4">
                <div className="max-w-4xl mx-auto">
                  <div className="flex gap-3">
                    <textarea
                      ref={inputRef}
                      value={inputValue}
                      onChange={(e) => setInputValue(e.target.value)}
                      onKeyDown={handleKeyDown}
                      placeholder="질문을 입력하세요... (Shift+Enter로 줄바꿈)"
                      rows={1}
                      className="flex-1 resize-none px-4 py-3 bg-gray-100 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all"
                      style={{ maxHeight: "120px" }}
                    />
                    <button
                      onClick={() => handleSendMessage(inputValue)}
                      disabled={!inputValue.trim() || sendMessageMutation.isPending}
                      className="px-5 py-3 bg-gradient-to-r from-indigo-500 to-purple-600 text-white rounded-xl hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 flex items-center gap-2"
                    >
                      <PaperAirplaneIcon className="h-5 w-5" />
                      <span className="hidden sm:inline">전송</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </>
        )}

        {/* AI학습 탭 콘텐츠 */}
        {activeTab === "learning" && (
          <div className="flex-1 overflow-y-auto p-6">
            <div className="max-w-2xl mx-auto">
              <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
                <div className="text-center mb-8">
                  <div className="w-16 h-16 bg-gradient-to-br from-indigo-100 to-purple-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
                    <AcademicCapIcon className="h-8 w-8 text-indigo-500" />
                  </div>
                  <h2 className="text-xl font-bold text-gray-800 mb-2">AI 학습 관리</h2>
                  <p className="text-gray-500">
                    AI가 시스템의 룰과 설정을 학습할 수 있도록 데이터를 전송합니다.
                  </p>
                </div>

                {/* 룰 학습 카드 */}
                <div className="border border-gray-200 rounded-xl p-6 hover:border-indigo-300 transition-colors">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold text-gray-800 mb-2">
                        룰 학습
                      </h3>
                      <p className="text-sm text-gray-500 mb-4">
                        현재 활성화된 모든 탐지 룰을 AI에게 학습시킵니다.
                        룰의 이름, 설명, 조건 정보가 전송됩니다.
                      </p>

                      {/* 결과 메시지 */}
                      {ruleSnapshotMutation.isSuccess && (
                        <div className={`p-3 rounded-lg mb-4 ${
                          ruleSnapshotMutation.data?.success
                            ? "bg-green-50 text-green-700"
                            : "bg-red-50 text-red-700"
                        }`}>
                          <p className="text-sm font-medium">
                            {ruleSnapshotMutation.data?.message}
                          </p>
                          {ruleSnapshotMutation.data?.success && ruleSnapshotMutation.data?.ruleCount > 0 && (
                            <p className="text-sm mt-1">
                              전송된 룰: {ruleSnapshotMutation.data.ruleCount}개
                            </p>
                          )}
                        </div>
                      )}

                      {ruleSnapshotMutation.isError && (
                        <div className="p-3 rounded-lg mb-4 bg-red-50 text-red-700">
                          <p className="text-sm font-medium">
                            룰 학습 전송에 실패했습니다. 다시 시도해주세요.
                          </p>
                        </div>
                      )}
                    </div>

                    <button
                      onClick={() => ruleSnapshotMutation.mutate()}
                      disabled={ruleSnapshotMutation.isPending}
                      className="px-5 py-2.5 bg-gradient-to-r from-indigo-500 to-purple-600 text-white rounded-lg hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 flex items-center gap-2 text-sm font-medium"
                    >
                      {ruleSnapshotMutation.isPending ? (
                        <>
                          <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                          전송 중...
                        </>
                      ) : (
                        <>
                          <BookOpenIcon className="h-4 w-4" />
                          룰 학습
                        </>
                      )}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// 마크다운 간단 변환 함수
function formatMarkdown(text: string): string {
  if (!text) return "";
  let html = text;

  // 코드 블록
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre class="bg-gray-800 text-gray-100 p-3 rounded-lg overflow-x-auto my-2 text-sm"><code>$2</code></pre>');

  // 인라인 코드
  html = html.replace(/`([^`]+)`/g, '<code class="bg-gray-100 px-1 rounded text-sm text-indigo-600">$1</code>');

  // 헤더
  html = html.replace(/^### (.+)$/gm, '<h3 class="text-base font-semibold mt-4 mb-2 text-gray-800">$1</h3>');
  html = html.replace(/^## (.+)$/gm, '<h2 class="text-lg font-bold mt-4 mb-3 text-gray-900">$1</h2>');

  // 굵은 글씨
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong class="font-semibold">$1</strong>');

  // 리스트
  html = html.replace(/^- (.+)$/gm, '<li class="ml-4 list-disc">$1</li>');
  html = html.replace(/^(\d+)\. (.+)$/gm, '<li class="ml-4"><span class="font-medium text-indigo-600">$1.</span> $2</li>');

  // 줄바꿈
  html = html.replace(/\n\n/g, '</p><p class="my-2">');
  html = html.replace(/\n/g, '<br>');

  return `<div class="space-y-1">${html}</div>`;
}
