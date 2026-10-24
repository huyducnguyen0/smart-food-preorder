import type { Role } from '../../api/auth'

/** Màn hình chính của từng vai trò sau khi đăng nhập (UI Prototype mục 2). */
export function homePathOf(role: Role): string {
  return role === 'KITCHEN' ? '/kitchen' : '/'
}
