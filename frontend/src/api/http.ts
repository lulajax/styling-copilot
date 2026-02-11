import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { getLocale } from '@/i18n';

const ACCESS_KEY = 'fashion_access_token';
const REFRESH_KEY = 'fashion_refresh_token';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20000
});

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(ACCESS_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  config.headers['Accept-Language'] = getLocale();
  return config;
});

let refreshing = false;
let pendingQueue: Array<(token: string | null) => void> = [];

const processQueue = (token: string | null) => {
  pendingQueue.forEach((callback) => callback(token));
  pendingQueue = [];
};

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const { response, config } = error;
    if (!response || response.status !== 401 || !config) {
      return Promise.reject(error);
    }

    const original = config as InternalAxiosRequestConfig & { _retry?: boolean };
    const refreshToken = localStorage.getItem(REFRESH_KEY);
    if (original._retry || !refreshToken || original.url?.includes('/api/auth/refresh')) {
      localStorage.removeItem(ACCESS_KEY);
      localStorage.removeItem(REFRESH_KEY);
      return Promise.reject(error);
    }

    original._retry = true;

    if (refreshing) {
      return new Promise((resolve, reject) => {
        pendingQueue.push((token) => {
          if (!token) {
            reject(error);
            return;
          }
          original.headers.Authorization = `Bearer ${token}`;
          resolve(http(original));
        });
      });
    }

    refreshing = true;
    try {
      const refreshResponse = await axios.post(`${API_BASE_URL}/api/auth/refresh`, { refreshToken });
      const newAccessToken = refreshResponse.data?.data?.accessToken as string | undefined;
      if (!newAccessToken) {
        throw new Error('refresh response missing token');
      }
      localStorage.setItem(ACCESS_KEY, newAccessToken);
      processQueue(newAccessToken);
      original.headers.Authorization = `Bearer ${newAccessToken}`;
      return http(original);
    } catch (refreshError) {
      processQueue(null);
      localStorage.removeItem(ACCESS_KEY);
      localStorage.removeItem(REFRESH_KEY);
      return Promise.reject(refreshError);
    } finally {
      refreshing = false;
    }
  }
);

export function getAccessToken(): string {
  return localStorage.getItem(ACCESS_KEY) ?? '';
}
