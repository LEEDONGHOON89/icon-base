import { atomWithStorage } from "jotai/utils";

interface AuthState {
  isAuthenticated: boolean;
  accessToken: string | null;
  refreshToken: string | null;
}

interface UserInfo {
  userId?: string;
  loginId?: string;
  name?: string;
  role?: string;
}

// 로컬 스토리지에 상태를 저장하는 atom
export const authAtom = atomWithStorage<AuthState>("auth", {
  isAuthenticated: false,
  accessToken: null,
  refreshToken: null,
});

// 사용자 정보를 저장하는 atom
export const userAtom = atomWithStorage<UserInfo | null>("user", null);
