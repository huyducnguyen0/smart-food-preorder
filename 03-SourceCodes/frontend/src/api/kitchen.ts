import { apiFetch } from './client'

// Kiểu dữ liệu khớp KitchenController (Architecture mục 5.2). Không có giá, thanh toán hay điểm công việc: bếp không
// cần cho việc chế biến (UC-12 Ghi chú).

export type KitchenOrder = {
  id: number
  pickupCode: string
  /** Giờ bắt đầu khung, dạng `2026-10-17T11:30` theo giờ cửa hàng. */
  slotStart: string
  slotEnd: string
  source: 'PRE_ORDER' | 'WALK_IN'
  confirmedAt: string
  /** Đã qua giờ bắt đầu khung mà chưa xong (BR-26); chỉ để hiển thị. */
  late: boolean
  lines: { name: string; quantity: number }[]
}

export type KitchenQueue = {
  serverTime: string
  preparing: KitchenOrder[]
  dueSoon: KitchenOrder[]
  upcoming: KitchenOrder[]
}

/** UC-12.1: các đơn hôm nay chia ba nhóm. */
export function fetchKitchenQueue(signal?: AbortSignal): Promise<KitchenQueue> {
  return apiFetch<KitchenQueue>('/api/kitchen/queue', { signal })
}
