import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
import { getAccessToken, setAccessToken, clearAuth } from "../store/authStore";

const baseURL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1";

// withCredentials is required here, not just on the /auth/refresh call below
// — the frontend (:5173) and backend (:8080) are different origins, and
// without this the browser won't store the Set-Cookie the backend sends
// back from login/register (the httpOnly refresh token cookie).
export const apiClient = axios.create({ baseURL, withCredentials: true });

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

// Refresh-token flow: on 401, try /auth/refresh once, replay the original
// request, and give up (logging out) if the refresh itself fails.
let refreshInFlight: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  if (!refreshInFlight) {
    refreshInFlight = axios
      .post(`${baseURL}/auth/refresh`, {}, { withCredentials: true })
      .then((res) => {
        const token = res.data?.data?.accessToken ?? null;
        if (token) setAccessToken(token);
        return token;
      })
      .catch(() => null)
      .finally(() => {
        refreshInFlight = null;
      });
  }
  return refreshInFlight;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as
      | (InternalAxiosRequestConfig & { _retry?: boolean })
      | undefined;

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true;
      const newToken = await refreshAccessToken();
      if (newToken) {
        originalRequest.headers.set("Authorization", `Bearer ${newToken}`);
        return apiClient(originalRequest);
      }
      clearAuth();
    }

    return Promise.reject(error);
  },
);
