import { createContext } from 'react'
import type { MenuItem } from '../../api/menu'
import type { ItemChange } from '../../api/orders'

/** Số lượng tối đa của một món trong giỏ; khớp giới hạn của API đặt đơn (Architecture mục 5). */
export const MAX_QUANTITY = 999

/** Một dòng giỏ: món và giá khách đã thấy lúc thêm (UC-04 bước 2). Backend luôn kiểm tra lại giá khi gửi (BR-35). */
export type CartLine = {
  menuItemId: number
  name: string
  imageUrl: string | null
  /** Giá khách đã thấy, đồng. */
  unitPrice: number
  quantity: number
}

export type CartContextValue = {
  lines: CartLine[]
  /** Tổng số phần (cộng số lượng các dòng). */
  count: number
  /** Tổng tiền = Σ giá × số lượng. */
  total: number
  /** Thêm 1 phần; món đã có thì tăng số lượng (UC-04 bước 2). */
  add: (item: MenuItem) => void
  /** Đổi số lượng, kẹp trong khoảng 1 đến {@link MAX_QUANTITY} (UC-04 bước 3). */
  setQuantity: (menuItemId: number, quantity: number) => void
  /** Bỏ món khỏi giỏ (UC-04 bước 3). */
  remove: (menuItemId: number) => void
  /** Cập nhật giỏ theo các món backend báo đã đổi: đổi giá thì lấy giá mới, tạm hết hoặc ngừng bán thì bỏ (UC-04 E7). */
  reprice: (changes: ItemChange[]) => void
  /** Xóa cả giỏ, dùng sau khi đặt thành công (UC-04 bước 11). */
  clear: () => void
}

export const CartContext = createContext<CartContextValue | null>(null)
