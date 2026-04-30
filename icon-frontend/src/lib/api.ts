import axios from "axios";
import { authAtom } from "@/atoms/authAtom";
import { toast } from "react-hot-toast";

// [2026-04-17] baseURL 제거: next.config.ts rewrites로 /api/* → localhost:11100 프록시
// 브라우저가 동일 오리진으로 호출하므로 CORS 불필요, 서버 IP 하드코딩 제거
const api = axios.create({
  baseURL: '',
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 15000, // 15s 타임아웃으로 무한 대기 방지
});

// 요청 인터셉터: 액세스 토큰 추가
api.interceptors.request.use(
  (config) => {
    // 이 부분은 setupApi에서 store를 통해 설정됩니다.
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 토큰 갱신 상태 관리
let refreshPromise: Promise<string> | null = null;

export const setupApi = (store: any) => {
  api.interceptors.request.use(
    (config) => {
      const auth = store.get(authAtom);
      console.log("[auth] request interceptor state", auth);
      if (auth && auth.isAuthenticated && auth.accessToken) {
        console.log("[auth] attaching Authorization header");
        config.headers.Authorization = `Bearer ${auth.accessToken}`;
      }
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  api.interceptors.response.use(
    (response) => response,
    async (error) => {
      const originalRequest = error.config;

      // 401 에러이고, 재시도 하지 않은 요청이며, 리프레시 API가 아닌 경우
      if (
        error.response?.status === 401 &&
        !originalRequest._retry &&
        !originalRequest.url?.includes('/auth/refresh') &&
        !originalRequest.url?.includes('/auth/login')
      ) {
        originalRequest._retry = true;

        try {
          // 토큰이 없으면 바로 로그아웃
          const auth = store.get(authAtom);
          if (!auth?.refreshToken) {
            logout(store);
            return Promise.reject(error);
          }

          // 이미 갱신 중이면 해당 Promise를 재사용
          if (!refreshPromise) {
            refreshPromise = refreshToken(store);
          }
          
          const newAccessToken = await refreshPromise;
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          return api(originalRequest);
          
        } catch (refreshError) {
          // 리프레시 실패 시 로그아웃 처리
          logout(store);
          // 원본 에러를 반환하여 무한 루프 방지
          return Promise.reject(error);
        } finally {
          refreshPromise = null;
        }
      }

      return Promise.reject(error);
    }
  );
};

// 토큰 갱신 함수
async function refreshToken(store: any): Promise<string> {
  const auth = store.get(authAtom);
  
  if (!auth?.refreshToken) {
    throw new Error('No refresh token available');
  }

  try {
    const response = await api.post('/api/v1/auth/refresh', {
      refreshToken: auth.refreshToken,
    });
    
    const { accessToken, refreshToken: newRefreshToken } = response.data;
    
    // 새 토큰으로 상태 업데이트
    store.set(authAtom, {
      isAuthenticated: true,
      accessToken,
      refreshToken: newRefreshToken || auth.refreshToken,
    });
    
    return accessToken;
  } catch (error) {
    console.error('Token refresh failed:', error);
    throw error;
  }
}

// 로그아웃 처리 함수
function logout(store: any) {
  store.set(authAtom, {
    isAuthenticated: false,
    accessToken: null,
    refreshToken: null,
  });
  
  if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
    toast.error('로그인이 만료되었습니다. 다시 로그인 해주세요.');
    window.location.href = '/login';
  }
}

export default api;
