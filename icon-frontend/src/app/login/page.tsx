"use client";

import { authAtom, userAtom } from "@/atoms/authAtom";
import Input from "@/components/common/Input";
import LoadingButton from "@/components/common/LoadingButton";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import { login } from "@/app/auth/api";
import { useAtom } from "jotai";
import { useRouter } from "next/navigation";
import React, { useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";

export default function LoginPage() {
  const [loginId, setLoginId] = useState("admin");
  const [password, setPassword] = useState("qwer1234!");
  const router = useRouter();
  const [auth, setAuth] = useAtom(authAtom);
  const [, setUser] = useAtom(userAtom);

  const { handleError, clearError } = useErrorHandling();

  useEffect(() => {
    if (typeof window !== "undefined" && auth.isAuthenticated) {
      router.push("/");
    }
  }, [auth.isAuthenticated, router]);

  // 로그인 API 호출 함수
  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      // 로그인 성공 시 인증 정보만 설정
      setAuth({
        isAuthenticated: true,
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
      });
      
      // 사용자 정보는 UserProvider에서 자동으로 가져옴
      router.push("/");
    },
    onError: (error) => {
      // 에러 처리
      handleError(error);
    },
  });

  // 폼 제출 핸들러
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    clearError();
    loginMutation.mutate({ loginId, password });
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-100">
      <div className="w-full max-w-md p-8 space-y-6 bg-white rounded-lg shadow-md">
        <h2 className="text-2xl font-bold text-center text-gray-900">로그인</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="로그인 ID"
            id="loginId"
            name="loginId"
            type="text"
            placeholder="로그인 ID를 입력하세요"
            value={loginId}
            onChange={(e) => setLoginId(e.target.value)}
            required
          />
          <Input
            label="비밀번호"
            id="password"
            name="password"
            type="password"
            placeholder="비밀번호를 입력하세요"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <LoadingButton
            type="submit"
            loading={loginMutation.isPending}
            className="w-full py-2 px-4 bg-blue-600 text-white font-semibold rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            로그인
          </LoadingButton>
        </form>
      </div>
    </div>
  );
}
