import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { apiClient } from "../services/apiClient";
import { getAccessToken, setAccessToken, clearAuth } from "./authStore";
import type { ApiResponse, User } from "../types/api";

interface AuthContextValue {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (organizationName: string, name: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

interface LoginResponseData {
  accessToken: string;
  user: User;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // On first load there's no access token in memory (a page refresh clears
  // it), but the httpOnly refresh cookie may still be valid — try a silent
  // refresh before deciding the user is logged out.
  useEffect(() => {
    let cancelled = false;

    async function restoreSession() {
      try {
        const refreshRes = await apiClient.post<ApiResponse<{ accessToken: string }>>("/auth/refresh");
        const token = refreshRes.data.data?.accessToken;
        if (!token) throw new Error("no token");
        setAccessToken(token);

        const meRes = await apiClient.get<ApiResponse<User>>("/users/me");
        if (!cancelled && meRes.data.data) {
          setUser(meRes.data.data);
        }
      } catch {
        clearAuth();
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    restoreSession();
    return () => {
      cancelled = true;
    };
  }, []);

  async function login(email: string, password: string) {
    const res = await apiClient.post<ApiResponse<LoginResponseData>>("/auth/login", { email, password });
    const data = res.data.data;
    if (!data) throw new Error("Login failed");
    setAccessToken(data.accessToken);
    setUser(data.user);
  }

  async function register(organizationName: string, name: string, email: string, password: string) {
    const res = await apiClient.post<ApiResponse<LoginResponseData>>("/auth/register", {
      organizationName,
      name,
      email,
      password,
    });
    const data = res.data.data;
    if (!data) throw new Error("Registration failed");
    setAccessToken(data.accessToken);
    setUser(data.user);
  }

  async function logout() {
    try {
      await apiClient.post("/auth/logout");
    } finally {
      clearAuth();
      setUser(null);
    }
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

export function isAuthenticated(): boolean {
  return getAccessToken() !== null;
}
