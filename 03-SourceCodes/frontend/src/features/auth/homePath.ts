import type { Role } from '../../api/auth'

/** Màn hình chính của từng vai trò sau khi đăng nhập (UI Prototype mục 2). */
export function homePathOf(role: Role): string {
  if (role === 'KITCHEN') return '/kitchen'
  // Quầy vào thẳng màn thu tiền; quản lý làm được việc của quầy nhưng vẫn vào thực đơn (UI Prototype mục 2).
  if (role === 'COUNTER') return '/counter'
  return '/'
}
