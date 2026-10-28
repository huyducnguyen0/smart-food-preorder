import { useCallback, useEffect, useMemo, useState } from 'react'
import { ApiError } from '../../api/client'
import {
  fetchCounterOrders,
  handOverOrder,
  recordPayment,
  type CounterOrder,
  type PaymentMethod,
} from '../../api/counter'
import { messageOf } from '../../api/errorMessages'
import { Alert } from '../../components/Alert'
import { formatMoney } from '../../lib/format'
import { slotRange, STATUS_LABELS, STATUS_TONES } from '../../lib/orderLabels'
import { WalkInPanel } from './WalkInPanel'

/** Tự làm mới mỗi 30 giây, như hàng chờ bếp. */
export const REFRESH_MS = 30_000

const METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'tiền mặt',
  BANK_TRANSFER: 'chuyển khoản',
}

type State =
  | { kind: 'loading' }
  | { kind: 'error'; message: string; orders: CounterOrder[] | null }
  | { kind: 'ready'; orders: CounterOrder[] }

/** Khung bàn giao của đơn sẵn sàng và đã thanh toán: quầy gõ mã khách đọc rồi xác nhận (BR-29). */
function HandoverForm({
  order,
  busy,
  onHandOver,
}: {
  order: CounterOrder
  busy: boolean
  onHandOver: (order: CounterOrder, code: string) => void
}) {
  const [code, setCode] = useState('')
  return (
    <form
      className="c-handover"
      onSubmit={(event) => {
        event.preventDefault()
        onHandOver(order, code)
      }}
    >
      <input
        type="text"
        className="c-search"
        placeholder="Mã khách đọc"
        aria-label={`Mã nhận khách đọc cho đơn ${order.pickupCode}`}
        autoComplete="off"
        maxLength={6}
        value={code}
        onChange={(event) => setCode(event.target.value)}
      />
      <button
        type="submit"
        className="btn btn-primary"
        disabled={busy || code.trim() === ''}
        aria-label={`Bàn giao đơn ${order.pickupCode}`}
      >
        Bàn giao
      </button>
    </form>
  )
}

function OrderCard({
  order,
  busy,
  onPay,
  onHandOver,
}: {
  order: CounterOrder
  busy: boolean
  onPay: (order: CounterOrder, method: PaymentMethod) => void
  onHandOver: (order: CounterOrder, code: string) => void
}) {
  return (
    <li className="k-card">
      <div className="k-card-head">
        <strong className="k-code">{order.pickupCode}</strong>
        <span className="k-time">{slotRange(order.slotStart, order.slotEnd)}</span>
        <span className={`status-pill tone-${STATUS_TONES[order.status]}`}>
          {STATUS_LABELS[order.status]}
        </span>
      </div>
      <p className="muted c-customer">
        {order.customerName ?? 'Khách mua tại quầy'} ·{' '}
        {order.source === 'PRE_ORDER' ? 'Pre-order' : 'Walk-in'}
      </p>
      <ul className="k-lines">
        {order.lines.map((line) => (
          <li key={line.name}>
            <b>{line.quantity}</b> × {line.name}
          </li>
        ))}
      </ul>
      <p className="c-total">
        <span>Cần thu</span>
        <b>{formatMoney(order.totalAmount)}</b>
      </p>
      {order.paymentStatus === 'PAID' ? (
        <>
          <p className="c-paid">
            Đã thanh toán{order.paymentMethod ? ` bằng ${METHOD_LABELS[order.paymentMethod]}` : ''}
          </p>
          {order.status === 'READY' && (
            <HandoverForm order={order} busy={busy} onHandOver={onHandOver} />
          )}
        </>
      ) : (
        <div className="c-actions">
          <button
            type="button"
            className="btn btn-primary"
            disabled={busy}
            aria-label={`Thu tiền mặt đơn ${order.pickupCode}`}
            onClick={() => onPay(order, 'CASH')}
          >
            Tiền mặt
          </button>
          <button
            type="button"
            className="btn"
            disabled={busy}
            aria-label={`Thu chuyển khoản đơn ${order.pickupCode}`}
            onClick={() => onPay(order, 'BANK_TRANSFER')}
          >
            Chuyển khoản
          </button>
        </div>
      )}
      {order.paymentStatus === 'UNPAID' && order.status === 'READY' && (
        <p className="muted c-hint">Đơn sẵn sàng. Thu tiền trước khi bàn giao.</p>
      )}
    </li>
  )
}

/**
 * Màn hình quầy (UC-08): đơn hôm nay còn đang xử lý, tìm theo mã nhận, ghi nhận thanh toán tiền mặt hoặc chuyển khoản
 * (BR-27); đơn sẵn sàng và đã thanh toán thì nhập mã khách đọc để bàn giao (UC-09, BR-29). Tự làm mới mỗi 30 giây.
 */
export function CounterPage() {
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [tick, setTick] = useState(0)
  const [search, setSearch] = useState('')
  const [busyId, setBusyId] = useState<number | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    fetchCounterOrders(controller.signal)
      .then((orders) => setState({ kind: 'ready', orders }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        setState((current) => ({
          kind: 'error',
          message: messageOf(error),
          orders: current.kind === 'loading' ? null : current.orders,
        }))
      })
    return () => controller.abort()
  }, [tick])

  useEffect(() => {
    const timer = window.setInterval(() => setTick((value) => value + 1), REFRESH_MS)
    return () => window.clearInterval(timer)
  }, [])

  const reload = useCallback(() => setTick((value) => value + 1), [])

  const handlePay = useCallback(
    async (order: CounterOrder, method: PaymentMethod) => {
      setBusyId(order.id)
      setNotice(null)
      try {
        await recordPayment(order.id, method)
      } catch (error) {
        // Đã có người ghi nhận trước hoặc đơn đã đổi: báo rồi làm mới để quầy thấy trạng thái thật.
        setNotice(
          error instanceof ApiError &&
            (error.code === 'ALREADY_PAID' || error.code === 'ORDER_NOT_PAYABLE')
            ? `Đơn ${order.pickupCode}: ${messageOf(error)}`
            : messageOf(error),
        )
      } finally {
        setBusyId(null)
        reload()
      }
    },
    [reload],
  )

  const handleHandOver = useCallback(
    async (order: CounterOrder, code: string) => {
      setBusyId(order.id)
      setNotice(null)
      try {
        await handOverOrder(order.id, code)
      } catch (error) {
        // Sai mã, chưa thanh toán hoặc đơn đã đổi trạng thái: báo lý do để quầy xử lý, đơn không đổi.
        setNotice(`Đơn ${order.pickupCode}: ${messageOf(error)}`)
      } finally {
        setBusyId(null)
        reload()
      }
    },
    [reload],
  )

  const orders = state.kind === 'loading' ? null : state.orders
  const visible = useMemo(() => {
    const needle = search.trim().toUpperCase()
    return (orders ?? []).filter((order) => needle === '' || order.pickupCode.includes(needle))
  }, [orders, search])

  return (
    <div className="k-page">
      <div className="k-bar">
        <input
          className="c-search"
          type="search"
          placeholder="Tìm theo mã nhận"
          aria-label="Tìm theo mã nhận"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <button type="button" className="btn" onClick={reload}>
          Tải lại
        </button>
      </div>

      <WalkInPanel onCreated={reload} />

      {state.kind === 'error' && (
        <Alert kind="danger">
          {state.message}
          {state.orders && ' Danh sách bên dưới có thể đã cũ.'}
        </Alert>
      )}
      {notice && <Alert kind="warning">{notice}</Alert>}

      {state.kind === 'loading' && (
        <p className="muted center" role="status">
          Đang tải đơn...
        </p>
      )}

      {orders && orders.length === 0 && <p className="empty">Hôm nay chưa có đơn cần xử lý.</p>}
      {orders && orders.length > 0 && visible.length === 0 && (
        <p className="empty">Không có đơn nào khớp mã "{search.trim()}".</p>
      )}

      {visible.length > 0 && (
        <ul className="c-grid">
          {visible.map((order) => (
            <OrderCard
              key={order.id}
              order={order}
              busy={busyId !== null}
              onPay={handlePay}
              onHandOver={handleHandOver}
            />
          ))}
        </ul>
      )}
    </div>
  )
}
