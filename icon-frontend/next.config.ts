import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  eslint: {
    // Warning: This allows production builds to successfully complete even if
    // your project has ESLint errors.
    ignoreDuringBuilds: true,
  },

  // 환경 변수 기본값 설정
  // .env 파일이 없어도 이 값들이 사용됩니다
  env: {
    // 백엔드 API 주소 (개발/프로덕션 자동 전환)
    NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL ||
      (process.env.NODE_ENV === 'production'
        ? 'http://localhost:11100'
        : 'http://localhost:11100'),
  },
};
console.log(nextConfig);
export default nextConfig;
