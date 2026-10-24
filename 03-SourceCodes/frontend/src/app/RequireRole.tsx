import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import type { Role } from '../api/auth'
import { useAuth } from '../features/auth/useAuth'

/**
 * Chỉ cho vào trang khi đã đăng nhập đúng vai trò. Chưa đăng nhập: chuyển tới đăng nhập rồi quay lại (UC-02 E4);
 * sai vai trò: trang "không có quyền" (UC-02 E5). Đây chỉ để điều hướng cho gọn; quyền thật kiểm ở backend (BR-33).
 */
export function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const auth = useAuth()
  const location = useLocation()

  if (auth.status === 'loading') {
    return (
      <p className="muted center" role="status">
        Đang tải...
      </p>
    )
  }
  if (auth.status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  if (!roles.includes(auth.user.role)) {
    return <Navigate to="/forbidden" replace />
  }
  return children
}
