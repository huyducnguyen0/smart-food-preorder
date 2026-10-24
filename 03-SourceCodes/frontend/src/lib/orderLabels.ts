import type { OrderStatus } from '../api/orders'

/** Tên trạng thái đơn hiển thị cho khách (SRS B.5). */
export const STATUS_LABELS: Record<OrderStatus, string> = {
  CONFIRMED: 'Đã xác nhận',
  PREPARING: 'Đang chuẩn bị',
  READY: 'Sẵn sàng nhận',
  PICKED_UP: 'Đã nhận',
  CANCELLED: 'Đã hủy',
  NO_SHOW: 'Không đến nhận',
}

/** Giờ của một thời điểm dạng `2026-10-17T11:30`: "11:30". */
export function timeOf(dateTime: string): string {
  return dateTime.slice(11, 16)
}

/** Khoảng giờ của khung nhận: "11:30 – 11:45". */
export function slotRange(start: string, end: string): string {
  return `${timeOf(start)} – ${timeOf(end)}`
}
