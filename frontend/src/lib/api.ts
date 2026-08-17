import axios from "axios";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export const apiClient = axios.create({
  baseURL: `${API_URL}/api/v1`,
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    const token = localStorage.getItem("accessToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

let refreshPromise: Promise<string | null> | null = null;

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      typeof window !== "undefined"
    ) {
      originalRequest._retry = true;
      const refreshToken = localStorage.getItem("refreshToken");
      if (!refreshToken) {
        clearAuth();
        return Promise.reject(error);
      }

      if (!refreshPromise) {
        refreshPromise = refreshAccessToken(refreshToken).finally(() => {
          refreshPromise = null;
        });
      }

      const newAccessToken = await refreshPromise;
      if (newAccessToken) {
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return apiClient(originalRequest);
      }
    }
    return Promise.reject(error);
  },
);

export type ApiResponse<T> = {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
};

export type User = {
  id: string;
  organizationId: string;
  departmentId: string | null;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  active: boolean;
  roles: string[];
  lastLoginAt: string | null;
  createdAt: string;
};

export type AuthData = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
};

export function storeAuth(data: AuthData) {
  localStorage.setItem("accessToken", data.accessToken);
  localStorage.setItem("refreshToken", data.refreshToken);
  localStorage.setItem("user", JSON.stringify(data.user));
}

export function clearAuth() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
}

export function getStoredUser(): User | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem("user");
  return raw ? (JSON.parse(raw) as User) : null;
}

async function refreshAccessToken(refreshToken: string): Promise<string | null> {
  try {
    const response = await axios.post<ApiResponse<AuthData>>(
      `${API_URL}/api/v1/auth/refresh`,
      { refreshToken },
    );
    storeAuth(response.data.data);
    return response.data.data.accessToken;
  } catch {
    clearAuth();
    return null;
  }
}

export async function getHealth() {
  const response = await apiClient.get<
    ApiResponse<{ status: string; service: string; version: string }>
  >("/health");
  return response.data;
}

export async function login(email: string, password: string) {
  const response = await apiClient.post<ApiResponse<AuthData>>("/auth/login", {
    email,
    password,
  });
  storeAuth(response.data.data);
  return response.data.data;
}

export async function register(input: {
  organizationName: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}) {
  const response = await apiClient.post<ApiResponse<AuthData>>("/auth/register", input);
  storeAuth(response.data.data);
  return response.data.data;
}

export async function logout() {
  const refreshToken = localStorage.getItem("refreshToken");
  if (refreshToken) {
    try {
      await apiClient.post("/auth/logout", { refreshToken });
    } catch {
      // Ignore logout errors
    }
  }
  clearAuth();
}

export async function getCurrentUser() {
  const response = await apiClient.get<ApiResponse<User>>("/users/me");
  return response.data.data;
}

export async function getDepartments() {
  const response = await apiClient.get<
    ApiResponse<
      Array<{
        id: string;
        organizationId: string;
        name: string;
        code: string | null;
        managerId: string | null;
        active: boolean;
        createdAt: string;
      }>
    >
  >("/departments");
  return response.data.data;
}
