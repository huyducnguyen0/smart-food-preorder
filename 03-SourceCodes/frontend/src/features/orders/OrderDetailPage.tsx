import { useLocation } from 'react-router'
import type { OrderDetail } from '../../api/orders'
import { PlaceholderPage } from '../../components/PlaceholderPage'
import { OrderDetailView } from './OrderDetailView'

/**
 * Trang `/orders/:id`. Sau khi đặt thành công, đơn vừa tạo được truyền theo điều hướng để hiện ngay kết quả (UC-04
 * bước 11). Tải đơn theo mã (mở lại từ "Đơn của tôi") làm ở T-20.
 */
export function OrderDetailPage() {
  const state = useLocation().state as { order?: OrderDetail; placed?: boolean } | null
  if (!state?.order) return <PlaceholderPage title="Chi tiết đơn" task="T-20" />
  return <OrderDetailView order={state.order} placed={state.placed === true} />
}
