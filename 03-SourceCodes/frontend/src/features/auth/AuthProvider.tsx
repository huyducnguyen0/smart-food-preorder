import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { fetchMe, login, logout, type CurrentUser } from '../../api/auth'
import { onSessionExpired } from '../../api/client'
import { AuthContext, type AuthContextValue, type AuthState } from './authContext'

/**
 * Giữ trạng thái đăng nhập cho cả ứng dụng. Lúc mở trang hỏi backend "tôi là ai" để biết phiên còn hiệu lực không
 * (UC-02 E4); phiên hết hạn thì coi như chưa đăng nhập.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ status: 'loading', user: null })

  useEffect(() => {
    let cancelled = false
    fetchMe()
      .then((user) => {
        if (!cancelled)
          setState(user ? { status: 'authenticated', user } : { status: 'anonymous', user: null })
      })
      .catch(() => {
        // Không hỏi được backend: coi như chưa đăng nhập; trang đăng nhập sẽ báo lỗi kết nối khi người dùng thử.
        if (!cancelled) setState({ status: 'anonymous', user: null })
      })
    return () => {
      cancelled = true
    }
  }, [])

  // UC-02 E4: một lời gọi API bị từ chối vì hết phiên thì coi như đã đăng xuất; RequireRole sẽ chuyển tới đăng nhập.
  useEffect(
    () =>
      onSessionExpired(() =>
        setState((current) =>
          current.status === 'authenticated' ? { status: 'anonymous', user: null } : current,
        ),
      ),
    [],
  )

  const signIn = useCallback(async (loginId: string, password: string): Promise<CurrentUser> => {
    const user = await login(loginId, password)
    setState({ status: 'authenticated', user })
    return user
  }, [])

  const signOut = useCallback(async () => {
    try {
      await logout()
    } finally {
      // Dù backend trả lỗi (ví dụ phiên đã hết hạn), phía giao diện vẫn thoát khỏi trạng thái đăng nhập.
      setState({ status: 'anonymous', user: null })
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ ...state, signIn, signOut }),
    [state, signIn, signOut],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
