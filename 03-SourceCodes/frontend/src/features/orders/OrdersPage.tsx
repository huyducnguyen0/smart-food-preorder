import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { messageOf } from '../../api/errorMessages'
import { fetchMyOrders, isOpenStatus, type OrderSummary } from '../../api/orders'
import { Alert } from '../../components/Alert'
import { formatMoney } from '../../lib/format'
import { dateOf, slotRange, STATUS_LABELS, STATUS_TONES } from '../../lib/orderLabels'
import { usePolling } from '../../lib/usePolling'
import { ORDER_POLL_MS } from './OrderDetailPage'

type State =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; orders: OrderSummary[] }

/**
 * UC-05 bước 1–2 "Đơn của tôi": đơn chưa kết thúc ở trên, rồi đơn đã kết thúc (thứ tự do backend quyết định). Khách bấm
 * "Tải lại" để thấy trạng thái mới (bước 5).
 */
export function OrdersPage() {
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    fetchMyOrders(controller.signal)
      .then((orders) => setState({ kind: 'ready', orders }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        // E3: báo lỗi và cho thử lại; không hiện danh sách cũ như dữ liệu hiện tại.
        setState({ kind: 'error', message: messageOf(error) })
      })
    return () => controller.abort()
  }, [attempt])

  const reload = useCallback(() => {
    setState({ kind: 'loading' })
    setAttempt((value) => value + 1)
  }, [])

  // PB-14: còn đơn chưa kết thúc thì tự làm mới ngầm mỗi 15 giây; lỗi tạm thời thì giữ danh sách đang hiện.
  const hasOpen = state.kind === 'ready' && state.orders.some((order) => isOpenStatus(order.status))
  usePolling(
    () => {
      fetchMyOrders().then(
        (orders) => setState({ kind: 'ready', orders }),
        () => undefined,
      )
    },
    ORDER_POLL_MS,
    hasOpen,
  )

  return (
    <div className="content">
      <div className="page-head">
        <h1>Đơn của tôi</h1>
        {state.kind === 'ready' && (
          <button type="button" className="link-btn" onClick={reload}>
            Tải lại
          </button>
        )}
      </div>

      {state.kind === 'loading' && (
        <p className="muted center" role="status">
          Đang tải đơn...
        </p>
      )}

      {state.kind === 'error' && (
        <>
          <Alert kind="danger">{state.message}</Alert>
          <button type="button" className="btn btn-primary btn-block" onClick={reload}>
            Thử lại
          </button>
        </>
      )}

      {state.kind === 'ready' && state.orders.length === 0 && (
        <div className="empty">
          <p>Bạn chưa có đơn.</p>
          <Link className="btn btn-primary" to="/">
            Xem thực đơn
          </Link>
        </div>
      )}

      {state.kind === 'ready' && state.orders.length > 0 && (
        <ul className="order-list">
          {state.orders.map((order) => (
            <li key={order.id}>
              <Link className="order-card" to={`/orders/${order.id}`}>
                <span className="order-code">{order.pickupCode}</span>
                <span className="order-meta">
                  {dateOf(order.slotStart)} · {slotRange(order.slotStart, order.slotEnd)}
                </span>
                <span className={`status-pill tone-${STATUS_TONES[order.status]}`}>
                  {STATUS_LABELS[order.status]}
                </span>
                <span className="order-meta">
                  {order.paymentStatus === 'PAID' ? 'Đã thanh toán' : 'Chưa thanh toán'}
                </span>
                <span className="price order-total">{formatMoney(order.totalAmount)}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
