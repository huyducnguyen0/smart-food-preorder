import { apiFetch } from './client'
import type { OrderDetail, OrderStatus } from './orders'

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
  /** Đơn READY đã quá giờ kết thúc khung + thời gian chờ: được đánh dấu không đến nhận (BR-25). */
  overdue: boolean
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

/** UC-09: bàn giao đơn sẵn sàng, đúng mã nhận và đã thanh toán (BR-29). */
export function handOverOrder(id: number, pickupCode: string) {
  return apiFetch<{ id: number; status: 'PICKED_UP' }>(`/api/counter/orders/${id}/handover`, {
    method: 'POST',
    body: { pickupCode },
  })
}

/** Khung dự kiến của đơn walk-in. */
export type WalkInSlot = { start: string; end: string }

export type WalkInLine = { menuItemId: number; quantity: number; unitPrice: number }

/** UC-07 bước 2–3: khung dự kiến sớm nhất (BR-18) và số tiền cần thu; chưa giữ quota. */
export function estimateWalkIn(lines: WalkInLine[]) {
  return apiFetch<{ slot: WalkInSlot; totalAmount: number }>('/api/counter/walk-in/estimate', {
    method: 'POST',
    body: { lines },
  })
}

/** UC-07 bước 5–7: xác nhận đơn walk-in khách đã đồng ý, kèm phương thức đã thu (BR-20). */
export function confirmWalkIn(request: {
  requestId: string
  slotStart: string
  lines: WalkInLine[]
  paymentMethod: PaymentMethod
}) {
  return apiFetch<OrderDetail>('/api/counter/walk-in', { method: 'POST', body: request })
}

/** UC-11: cửa hàng hủy đơn kèm lý do (BR-24). Quầy chỉ hủy được đơn walk-in chưa bắt đầu làm. */
export function cancelStoreOrder(id: number, reason: string) {
  return apiFetch<{ id: number; status: OrderStatus }>(`/api/counter/orders/${id}/cancel`, {
    method: 'POST',
    body: { reason },
  })
}

/** UC-10: đánh dấu khách không đến nhận đơn READY đã quá hạn (BR-25). Không hoàn quota. */
export function markNoShow(id: number) {
  return apiFetch<{ id: number; status: OrderStatus }>(`/api/counter/orders/${id}/no-show`, {
    method: 'POST',
  })
}
