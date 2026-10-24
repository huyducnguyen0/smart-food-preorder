import { MAX_QUANTITY, type CartLine } from './cartContext'

// Giỏ nằm trên trình duyệt của khách (quyết định 2026-10-10, UC-04 Ghi chú): đổi thiết bị hoặc xóa dữ liệu trình duyệt
// thì mất giỏ. Mỗi tài khoản một giỏ riêng để hai khách dùng chung một máy không thấy giỏ của nhau.
const keyOf = (userId: number) => `nhom15.cart.v1.${userId}`

function isLine(value: unknown): value is CartLine {
  if (typeof value !== 'object' || value === null) return false
  const line = value as Record<string, unknown>
  return (
    Number.isInteger(line.menuItemId) &&
    typeof line.name === 'string' &&
    (line.imageUrl === null || typeof line.imageUrl === 'string') &&
    Number.isInteger(line.unitPrice) &&
    (line.unitPrice as number) >= 0 &&
    Number.isInteger(line.quantity) &&
    (line.quantity as number) >= 1 &&
    (line.quantity as number) <= MAX_QUANTITY
  )
}

/** Đọc giỏ đã lưu. Dữ liệu hỏng hoặc không đọc được bộ nhớ trình duyệt thì coi như giỏ trống, không báo lỗi. */
export function loadCart(userId: number): CartLine[] {
  try {
    const raw = window.localStorage.getItem(keyOf(userId))
    if (!raw) return []
    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    const seen = new Set<number>()
    return parsed.filter(isLine).filter((line) => {
      if (seen.has(line.menuItemId)) return false
      seen.add(line.menuItemId)
      return true
    })
  } catch {
    return []
  }
}

/** Lưu giỏ. Không lưu được (chế độ riêng tư, bộ nhớ đầy) thì giỏ vẫn dùng được trong phiên hiện tại. */
export function saveCart(userId: number, lines: CartLine[]): void {
  try {
    if (lines.length === 0) window.localStorage.removeItem(keyOf(userId))
    else window.localStorage.setItem(keyOf(userId), JSON.stringify(lines))
  } catch {
    // Bỏ qua có chủ đích: xem chú thích ở trên.
  }
}
