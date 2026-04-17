'use client';

import { useUser } from '@/hooks/useUser';
import { useAtomValue } from 'jotai';
import { authAtom } from '@/atoms/authAtom';
import { useEffect } from 'react';

interface UserProviderProps {
  children: React.ReactNode;
}

/**
 * 사용자 정보를 자동으로 로드하는 Provider 컴포넌트
 */
export default function UserProvider({ children }: UserProviderProps) {
  const auth = useAtomValue(authAtom);
  const { user, isLoading } = useUser();

  // 로그인되어 있지만 사용자 정보가 없을 때의 로딩 상태 표시
  if (auth.isAuthenticated && !user && isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">사용자 정보를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}