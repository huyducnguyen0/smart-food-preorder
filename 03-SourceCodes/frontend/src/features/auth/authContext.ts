import { createContext } from 'react'
import type { CurrentUser } from '../../api/auth'

export type AuthState =
  | { status: 'loading'; user: null }
  | { status: 'anonymous'; user: null }
  | { status: 'authenticated'; user: CurrentUser }

export type AuthContextValue = AuthState & {
  /** Đăng nhập; ném ApiError nếu sai thông tin hoặc tài khoản bị khóa (UC-02 E1, E2). */
  signIn: (loginId: string, password: string) => Promise<CurrentUser>
  /** Đăng xuất (UC-02 bước 7–8). */
  signOut: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
