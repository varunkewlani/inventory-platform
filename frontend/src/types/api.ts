export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  meta: unknown;
  error: { code: string; message: string } | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export type Role = "ADMIN" | "MANAGER" | "STAFF";

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  status: "ACTIVE" | "DISABLED";
  organizationId: number;
  createdAt: string;
}
