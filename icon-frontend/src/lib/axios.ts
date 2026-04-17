import axios from "axios";

// API 기본 URL 설정 (우선순위: 환경변수 > 기본값)
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:11100';

// 한글 주석: 기본 baseURL을 설정한 axios 인스턴스 생성
const api = axios.create({
  baseURL: API_BASE_URL,
});

export default api;
