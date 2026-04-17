import { useQuery } from '@tanstack/react-query';
import { useAtom, useAtomValue } from 'jotai';
import { authAtom, userAtom } from '@/atoms/authAtom';
import { fetchCurrentUser } from '@/app/auth/api';
import { useEffect } from 'react';

/**
 * 사용자 정보를 관리하는 훅
 */
export const useUser = () => {
  const auth = useAtomValue(authAtom);
  const [user, setUser] = useAtom(userAtom);

  // 로그인되어 있을 때만 사용자 정보를 가져옴
  const { data: fetchedUser, isLoading, error } = useQuery({
    queryKey: ['user', 'me'],
    queryFn: fetchCurrentUser,
    enabled: auth.isAuthenticated && !user,
    staleTime: 5 * 60 * 1000, // 5분간 fresh
    retry: false,
  });

  // 사용자 정보가 가져와지면 atom에 저장
  useEffect(() => {
    if (fetchedUser && !user) {
      setUser(fetchedUser);
    }
  }, [fetchedUser, user, setUser]);

  return {
    user,
    isLoading,
    error,
    refetch: () => {
      // 사용자 정보 새로고침 시 atom 초기화 후 다시 가져오기
      setUser(null);
    },
  };
};