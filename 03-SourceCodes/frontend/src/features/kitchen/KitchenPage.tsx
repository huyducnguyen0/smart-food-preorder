import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '../../api/client'
import { messageOf } from '../../api/errorMessages'
import {
  completeOrder,
  fetchKitchenQueue,
  startOrder,
  type KitchenOrder,
  type KitchenQueue,
} from '../../api/kitchen'
import { Alert } from '../../components/Alert'
import { timeOf } from '../../lib/orderLabels'

/** UC-12 bước 3: tự làm mới mỗi 30 giây (UI Prototype mục 2). */
export const REFRESH_MS = 30_000

type State =
  | { kind: 'loading' }
  | { kind: 'error'; message: string; queue: KitchenQueue | null }
  | { kind: 'ready'; queue: KitchenQueue }

type Action = 'start' | 'complete'

function OrderCard({
  order,
  action,
  emphasis,
  busy,
  onAction,
}: {
  order: KitchenOrder
  action: Action
  emphasis: boolean
  busy: boolean
  onAction: (order: KitchenOrder, action: Action) => void
}) {
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
      <button
        type="button"
        className={`btn k-action${emphasis ? ' btn-primary' : ''}`}
        disabled={busy}
        aria-label={`${action === 'start' ? 'Bắt đầu' : 'Hoàn tất'} đơn ${order.pickupCode}`}
        onClick={() => onAction(order, action)}
      >
        {action === 'start' ? 'Bắt đầu' : 'Hoàn tất'}
      </button>
    </li>
  )
}

function Column({
  title,
  orders,
  action,
  emphasis,
  busyId,
  onAction,
}: {
  title: string
  orders: KitchenOrder[]
  action: Action
  emphasis: boolean
  busyId: number | null
  onAction: (order: KitchenOrder, action: Action) => void
}) {
  return (
    <section className="k-column" aria-label={title}>
      <h2>
        {title} <span className="k-count">({orders.length})</span>
      </h2>
      <ul className="k-list">
        {orders.map((order) => (
          <OrderCard
            key={order.id}
            order={order}
            action={action}
            emphasis={emphasis}
            busy={busyId !== null}
            onAction={onAction}
          />
        ))}
      </ul>
    </section>
  )
}

/**
 * UC-12.1 Hàng chờ bếp: đơn hôm nay chia ba nhóm Đang làm, Cần chuẩn bị, Sắp tới; đơn trễ có nhãn đỏ. Tự tải lại mỗi 30
 * giây và có nút "Tải lại". Bếp bấm "Bắt đầu" ở đơn đã xác nhận (kể cả đơn sắp tới, BR-21) và "Hoàn tất" ở đơn đang làm
 * (UC-12.2). Đơn đổi trạng thái ở nơi khác thì báo và làm mới (E1).
 */
export function KitchenPage() {
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [tick, setTick] = useState(0)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

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

  const handleAction = useCallback(
    async (order: KitchenOrder, action: Action) => {
      setBusyId(order.id)
      setActionError(null)
      try {
        await (action === 'start' ? startOrder(order.id) : completeOrder(order.id))
      } catch (error) {
        // E1: trạng thái đơn đã khác với màn hình (hai bếp cùng bấm, màn hình cũ): báo rồi làm mới. Lỗi khác: báo, đơn không đổi.
        setActionError(
          error instanceof ApiError && error.code === 'INVALID_STATUS_TRANSITION'
            ? `Đơn ${order.pickupCode} đã đổi trạng thái, danh sách đã được làm mới.`
            : messageOf(error),
        )
      } finally {
        setBusyId(null)
        reload()
      }
    },
    [reload],
  )

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

      {actionError && <Alert kind="warning">{actionError}</Alert>}

      {state.kind === 'loading' && (
        <p className="muted center" role="status">
          Đang tải hàng chờ...
        </p>
      )}

      {queue && empty && <p className="empty">Chưa có đơn cần làm.</p>}

      {queue && !empty && (
        <div className="k-columns">
          <Column
            title="Đang làm"
            orders={queue.preparing}
            action="complete"
            emphasis
            busyId={busyId}
            onAction={handleAction}
          />
          <Column
            title="Cần chuẩn bị"
            orders={queue.dueSoon}
            action="start"
            emphasis
            busyId={busyId}
            onAction={handleAction}
          />
          <Column
            title="Sắp tới"
            orders={queue.upcoming}
            action="start"
            emphasis={false}
            busyId={busyId}
            onAction={handleAction}
          />
        </div>
      )}
    </div>
  )
}
