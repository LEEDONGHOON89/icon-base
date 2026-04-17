import api from "@/lib/api";

// 사용자 정보 인터페이스
export interface UserInfo {
  userId: string;
  loginId: string;
  name: string;
  role: string;
}

// 현재 로그인한 사용자 정보 조회
export const fetchCurrentUser = async (): Promise<UserInfo> => {
  if (typeof window !== "undefined") {
    const rawAuth = window.localStorage.getItem("auth");
    console.log("[auth] localStorage auth", rawAuth);
  }
  console.log("[auth] axios headers before /auth/me", api.defaults.headers.common.Authorization);
  const response = await api.get('/api/v1/auth/me');
  return response.data;
};

// 로그인 요청 인터페이스
export interface LoginRequest {
  loginId: string;
  password: string;
}

// 로그인 응답 인터페이스
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  message?: string;
  user?: UserInfo;
}

// 로그인
export const login = async (data: LoginRequest): Promise<LoginResponse> => {
  const response = await api.post('/api/v1/auth/login', data);
  return response.data;
};
