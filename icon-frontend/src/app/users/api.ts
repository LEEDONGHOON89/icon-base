import api from "@/lib/api";

// User 타입 (백엔드 UserResponse 구조와 일치)
export interface User {
  userId: string;
  loginId: string;
  userName: string;
  email: string;
  description: string;
}

// 생성/수정 요청 타입
export interface UserCreateRequest {
  loginId: string;
  password: string;
  userName: string;
  email?: string;
  description?: string;
}
export interface UserUpdateRequest {
  userName: string;
  email?: string;
  description?: string;
  password?: string;
}

// 비밀번호 변경 요청 타입
export interface PasswordChangeRequest {
  password: string;
}

// 사용자 목록 조회
export async function fetchUsers(): Promise<ResponseList<User>> {
  const res = await api.get<ResponseList<User>>("/api/v1/users");
  return res.data;
}

// 단일 사용자 조회
export async function fetchUser(userId: string): Promise<User> {
  const res = await api.get(`/api/v1/users/${userId}`);
  return res.data;
}

// 사용자 생성
export async function createUser(data: UserCreateRequest): Promise<User> {
  const res = await api.post("/api/v1/users", data);
  return res.data;
}

// 사용자 수정
export async function updateUser(
  userId: string,
  data: UserUpdateRequest
): Promise<User> {
  const res = await api.put(`/api/v1/users/${userId}`, data);
  return res.data;
}

// 비밀번호 변경
export async function changePassword(
  userId: string,
  data: PasswordChangeRequest
): Promise<void> {
  await api.put(`/api/v1/users/${userId}/password`, data);
}
