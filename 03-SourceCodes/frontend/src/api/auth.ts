import { apiFetch, ApiError } from './client'

// Kiểu dữ liệu khớp CurrentUser của backend (GET /api/auth/me, POST /api/auth/login).
export type Role = 'CUSTOMER' | 'COUNTER' | 'KITCHEN' | 'MANAGER'

export type CurrentUser = {
  id: number
  displayName: string
  role: Role
}

/** Người dùng đang đăng nhập, hoặc null nếu chưa đăng nhập hoặc phiên đã hết hạn (UC-02 E4). */
export async function fetchMe(): Promise<CurrentUser | null> {
  try {
    return await apiFetch<CurrentUser>('/api/auth/me')
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null
    throw error
  }
}

export function login(loginId: string, password: string): Promise<CurrentUser> {
  return apiFetch<CurrentUser>('/api/auth/login', { method: 'POST', body: { loginId, password } })
}

export function logout(): Promise<void> {
  return apiFetch<void>('/api/auth/logout', { method: 'POST' })
}
