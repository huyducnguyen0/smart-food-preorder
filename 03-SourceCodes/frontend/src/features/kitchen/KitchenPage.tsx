import { useCallback, useEffect, useState } from 'react'
import { messageOf } from '../../api/errorMessages'
import { fetchKitchenQueue, type KitchenOrder, type KitchenQueue } from '../../api/kitchen'
import { Alert } from '../../components/Alert'
import { timeOf } from '../../lib/orderLabels'

/** UC-12 bước 3: tự làm mới mỗi 30 giây (UI Prototype mục 2). */
export const REFRESH_MS = 30_000

type State =
  | { kind: 'loading' }
  | { kind: 'error'; message: string; queue: KitchenQueue | null }
  | { kind: 'ready'; queue: KitchenQueue }

function OrderCard({ order }: { order: KitchenOrder }) {
  return (
    <li className={`k-card${order.late ? ' late' : ''}`}>
      <div className="k-card-head">
        <strong className="k-code">{order.pickupCode}</strong>
        <span className="k-time">{timeOf(order.slotStart)}</span>
        {order.late && <span className="k-late">TRỄ</span>}
        <span className="k-source">{order.source === 'PRE_ORDER' ? 'Pre-order' : 'Walk-in'}</span>
      </div>
      <ul className="k-lines">
        {order.lines.map((line) => (
          <li key={line.name}>
            <b>{line.quantity}</b> × {line.name}
          </li>
        ))}
      </ul>
      <span className="k-confirmed">Xác nhận {timeOf(order.confirmedAt)}</span>
    </li>
  )
}

function Column({ title, orders }: { title: string; orders: KitchenOrder[] }) {
  return (
    <section className="k-column" aria-label={title}>
      <h2>
        {title} <span className="k-count">({orders.length})</span>
      </h2>
      <ul className="k-list">
        {orders.map((order) => (
          <OrderCard key={order.id} order={order} />
        ))}
      </ul>
    </section>
  )
}

/**
 * UC-12.1 Hàng chờ bếp: đơn hôm nay chia ba nhóm Đang làm, Cần chuẩn bị, Sắp tới; đơn trễ có nhãn đỏ. Tự tải lại mỗi 30
 * giây và có nút "Tải lại". Thao tác Bắt đầu và Hoàn tất làm ở T-22.
 */
export function KitchenPage() {
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [tick, setTick] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    fetchKitchenQueue(controller.signal)
      .then((queue) => setState({ kind: 'ready', queue }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        // E3: báo lỗi; danh sách cũ (nếu có) vẫn hiện nhưng kèm cảnh báo là có thể đã cũ.
        setState((current) => ({
          kind: 'error',
          message: messageOf(error),
          queue:
            current.kind === 'ready'
              ? current.queue
              : current.kind === 'error'
                ? current.queue
                : null,
        }))
      })
    return () => controller.abort()
  }, [tick])

  useEffect(() => {
    const timer = window.setInterval(() => setTick((value) => value + 1), REFRESH_MS)
    return () => window.clearInterval(timer)
  }, [])

  const reload = useCallback(() => setTick((value) => value + 1), [])

  const queue = state.kind === 'ready' ? state.queue : state.kind === 'error' ? state.queue : null
  const empty =
    queue !== null && queue.preparing.length + queue.dueSoon.length + queue.upcoming.length === 0

  return (
    <div className="k-page">
      <div className="k-bar">
        <span className="muted">
          {queue
            ? `Cập nhật lúc ${timeOf(queue.serverTime)} · tự làm mới mỗi 30 giây`
            : 'Đang tải...'}
        </span>
        <button type="button" className="btn" onClick={reload}>
          Tải lại
        </button>
      </div>

      {state.kind === 'error' && (
        <Alert kind="danger">
          {state.message}
          {state.queue && ' Danh sách bên dưới có thể đã cũ.'}
        </Alert>
      )}

      {state.kind === 'loading' && (
        <p className="muted center" role="status">
          Đang tải hàng chờ...
        </p>
      )}

      {queue && empty && <p className="empty">Chưa có đơn cần làm.</p>}

      {queue && !empty && (
        <div className="k-columns">
          <Column title="Đang làm" orders={queue.preparing} />
          <Column title="Cần chuẩn bị" orders={queue.dueSoon} />
          <Column title="Sắp tới" orders={queue.upcoming} />
        </div>
      )}
    </div>
  )
}
