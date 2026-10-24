import { apiFetch } from './client'

// Kiểu dữ liệu khớp SlotController và OrderController của backend (Architecture mục 5). Không có điểm công việc hay
// quota: khách chỉ được biết khung đặt được hay không (BR-36).

export type SlotView = {
  /** Giờ bắt đầu khung, dạng `2026-10-17T11:30` theo giờ cửa hàng. */
  start: string
  end: string
  bookable: boolean
}

export type UnavailableReason = 'NO_SLOT_LEFT_TODAY' | 'ALL_SLOTS_FULL' | 'ORDER_TOO_LARGE'

export type SlotList = {
  date: string
  slots: SlotView[]
  unavailableReason: UnavailableReason | null
}

export type CartLineRequest = { menuItemId: number; quantity: number }

/** UC-04 bước 6: khung hôm nay và tình trạng đặt được theo giỏ. */
export function fetchSlots(lines: CartLineRequest[], signal?: AbortSignal): Promise<SlotList> {
  const query = lines.map((line) => `${line.menuItemId}:${line.quantity}`).join(',')
  return apiFetch<SlotList>(`/api/slots?lines=${encodeURIComponent(query)}`, { signal })
}

export type OrderStatus =
  'CONFIRMED' | 'PREPARING' | 'READY' | 'PICKED_UP' | 'CANCELLED' | 'NO_SHOW'

export type OrderLine = {
  menuItemId: number
  name: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export type OrderDetail = {
  id: number
  pickupCode: string
  slotStart: string
  slotEnd: string
  status: OrderStatus
  paymentStatus: 'UNPAID' | 'PAID'
  paymentMethod: 'CASH' | 'BANK_TRANSFER' | null
  totalAmount: number
  confirmedAt: string
  lines: OrderLine[]
}

export type PlaceOrderRequest = {
  /** Mã yêu cầu của lần đặt; giữ nguyên khi gửi lại để backend không tạo đơn thứ hai (BR-17). */
  requestId: string
  slotStart: string
  lines: { menuItemId: number; quantity: number; unitPrice: number }[]
}

/** UC-04 bước 7–11. Gửi lại cùng `requestId` trả lại đúng đơn đã tạo (A1). */
export function placeOrder(request: PlaceOrderRequest): Promise<OrderDetail> {
  return apiFetch<OrderDetail>('/api/orders', { method: 'POST', body: request })
}

/** Một món đã thay đổi so với giỏ, backend báo kèm lỗi ITEMS_CHANGED (UC-04 E7). */
export type ItemChange = {
  menuItemId: number
  reason: 'PRICE_CHANGED' | 'UNAVAILABLE'
  currentPrice?: number
}
