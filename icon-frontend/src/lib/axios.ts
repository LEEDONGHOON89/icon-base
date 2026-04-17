import axios from "axios";

// [2026-04-17] API URL: 환경변수 > 브라우저 호스트명 기반 > localhost 순으로 결정
// 브라우저 호스트명 사용 시 VM/개발서버 IP가 달라도 동작 (빌드 재배포 불필요)
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ||
  (typeof window !== 'undefined'
    ? `http://${window.location.hostname}:11100`
    : 'http://localhost:11100');

// 한글 주석: 기본 baseURL을 설정한 axios 인스턴스 생성
const api = axios.create({
  baseURL: API_BASE_URL,
});

export default api;
