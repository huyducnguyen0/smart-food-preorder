import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { MenuItem } from '../../api/menu'
import { useAuth } from '../auth/useAuth'
import { CartContext, MAX_QUANTITY, type CartContextValue, type CartLine } from './cartContext'
import { loadCart, saveCart } from './cartStorage'

function CartStore({ userId, children }: { userId: number | null; children: ReactNode }) {
  const [lines, setLines] = useState<CartLine[]>(() => (userId === null ? [] : loadCart(userId)))

  useEffect(() => {
    if (userId !== null) saveCart(userId, lines)
  }, [userId, lines])

  const add = useCallback((item: MenuItem) => {
    setLines((current) => {
      const existing = current.find((line) => line.menuItemId === item.id)
      if (existing) {
        return current.map((line) =>
          line.menuItemId === item.id
            ? { ...line, quantity: Math.min(line.quantity + 1, MAX_QUANTITY) }
            : line,
        )
      }
      return [
        ...current,
        {
          menuItemId: item.id,
          name: item.name,
          imageUrl: item.imageUrl,
          unitPrice: item.price,
          quantity: 1,
        },
      ]
    })
  }, [])

  const setQuantity = useCallback((menuItemId: number, quantity: number) => {
    const clamped = Math.min(Math.max(Math.trunc(quantity), 1), MAX_QUANTITY)
    setLines((current) =>
      current.map((line) =>
        line.menuItemId === menuItemId ? { ...line, quantity: clamped } : line,
      ),
    )
  }, [])

  const remove = useCallback((menuItemId: number) => {
    setLines((current) => current.filter((line) => line.menuItemId !== menuItemId))
  }, [])

  const clear = useCallback(() => setLines([]), [])

  const value = useMemo<CartContextValue>(
    () => ({
      lines,
      count: lines.reduce((sum, line) => sum + line.quantity, 0),
      total: lines.reduce((sum, line) => sum + line.unitPrice * line.quantity, 0),
      add,
      setQuantity,
      remove,
      clear,
    }),
    [lines, add, setQuantity, remove, clear],
  )
  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

/**
 * Giữ giỏ hàng của người đang đăng nhập (UC-04.1). Đổi người dùng thì dựng lại kho giỏ từ bộ nhớ trình duyệt của
 * người đó; đăng xuất hoặc phiên hết hạn không làm mất giỏ (UC-04 E11). Khách vãng lai không có giỏ.
 */
export function CartProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const userId = user?.id ?? null
  return (
    <CartStore key={userId ?? 'anonymous'} userId={userId}>
      {children}
    </CartStore>
  )
}
