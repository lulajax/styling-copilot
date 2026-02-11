import { defineStore } from 'pinia';

interface AuthState {
  accessToken: string;
  refreshToken: string;
  username: string;
}

const ACCESS_KEY = 'fashion_access_token';
const REFRESH_KEY = 'fashion_refresh_token';
const USERNAME_KEY = 'fashion_username';

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: localStorage.getItem(ACCESS_KEY) ?? '',
    refreshToken: localStorage.getItem(REFRESH_KEY) ?? '',
    username: localStorage.getItem(USERNAME_KEY) ?? ''
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.accessToken)
  },
  actions: {
    setTokens(username: string, accessToken: string, refreshToken: string) {
      this.username = username;
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
      localStorage.setItem(USERNAME_KEY, username);
      localStorage.setItem(ACCESS_KEY, accessToken);
      localStorage.setItem(REFRESH_KEY, refreshToken);
    },
    updateAccessToken(accessToken: string) {
      this.accessToken = accessToken;
      localStorage.setItem(ACCESS_KEY, accessToken);
    },
    clear() {
      this.username = '';
      this.accessToken = '';
      this.refreshToken = '';
      localStorage.removeItem(USERNAME_KEY);
      localStorage.removeItem(ACCESS_KEY);
      localStorage.removeItem(REFRESH_KEY);
    }
  }
});
