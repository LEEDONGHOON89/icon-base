import type { NextConfig } from "next";

// [2026-04-17] API 프록시 방식으로 변경
// 브라우저는 동일 오리진(/api/*)으로 호출 → Next.js 서버가 백엔드(localhost:11100)로 전달
// 하드코딩된 서버 IP 제거, CORS 문제 해결
const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:11100';

const nextConfig: NextConfig = {
  output: 'standalone',
  eslint: {
    ignoreDuringBuilds: true,
  },
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${BACKEND_URL}/api/:path*`,
      },
      {
        source: '/rpc/:path*',
        destination: `${BACKEND_URL}/rpc/:path*`,
      },
    ];
  },
};
export default nextConfig;
