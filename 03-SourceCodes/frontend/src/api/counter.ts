import { apiFetch } from './client'
import type { OrderStatus } from './orders'

// Kiểu dữ liệu khớp CounterController (Architecture mục 5.2).

export type PaymentMethod = 'CASH' | 'BANK_TRANSFER'

export type CounterOrder = {
  id: number
  pickupCode: string
  /** Tên khách; null với đơn walk-in. */
  customerName: string | null
  /** Giờ bắt đầu khung, dạng `2026-10-17T11:30` theo giờ cửa hàng. */
  slotStart: string
  slotEnd: string
  source: 'PRE_ORDER' | 'WALK_IN'
  status: OrderStatus
  paymentStatus: 'UNPAID' | 'PAID'
  paymentMethod: PaymentMethod | null
  totalAmount: number
  lines: { name: string; quantity: number }[]
}

/** Đơn hôm nay còn đang xử lý, để quầy thu tiền và giao món. */
export async function fetchCounterOrders(signal?: AbortSignal): Promise<CounterOrder[]> {
  const response = await apiFetch<{ orders: CounterOrder[] }>('/api/counter/orders', { signal })
  return response.orders
}

/** UC-08: ghi nhận đơn đã thanh toán kèm phương thức. */
export function recordPayment(id: number, method: PaymentMethod) {
  return apiFetch<{ id: number; paymentStatus: 'PAID'; paymentMethod: PaymentMethod }>(
    `/api/counter/orders/${id}/payment`,
    { method: 'POST', body: { method } },
  )
}
