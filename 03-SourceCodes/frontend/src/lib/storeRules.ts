import type { Role } from '../api/auth'
import type { CounterOrder } from '../api/counter'

/**
 * BR-24: quản lý hủy được đơn xác nhận hoặc đang làm; nhân viên quầy chỉ hủy được đơn walk-in chưa bắt đầu làm.
 * Backend vẫn kiểm tra lại; đây chỉ để không hiện nút vô dụng.
 */
export function canStoreCancel(order: CounterOrder, role: Role | undefined): boolean {
  if (role === 'MANAGER') return order.status === 'CONFIRMED' || order.status === 'PREPARING'
  return order.source === 'WALK_IN' && order.status === 'CONFIRMED'
}
