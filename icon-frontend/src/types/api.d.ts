interface ErrorResponse {
  status: number;
  message: string;
  timestamp: string; // 백엔드에서 LocalDateTime을 문자열로 반환할 것이므로 string으로 정의
}

// 필요하다면 다른 공통 응답 인터페이스도 여기에 정의할 수 있습니다.

interface ResponseList<T> {
  total: number;
  data: T[];
}
