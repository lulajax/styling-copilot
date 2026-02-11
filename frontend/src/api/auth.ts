import { http } from './http';
import type { ApiResult } from '@/types/api';

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface RefreshResponse {
  accessToken: string;
  expiresIn: number;
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  const { data } = await http.post<ApiResult<LoginResponse>>('/api/auth/login', { username, password });
  return data.data;
}

export async function refresh(refreshToken: string): Promise<RefreshResponse> {
  const { data } = await http.post<ApiResult<RefreshResponse>>('/api/auth/refresh', { refreshToken });
  return data.data;
}
