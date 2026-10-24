import { useContext } from 'react'
import { CartContext, type CartContextValue } from './cartContext'

export function useCart(): CartContextValue {
  const value = useContext(CartContext)
  if (!value) throw new Error('useCart phải được dùng bên trong CartProvider')
  return value
}
