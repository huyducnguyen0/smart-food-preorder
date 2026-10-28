import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { messageOf } from '../../api/errorMessages'
import { ApiError } from '../../api/client'
import {
  fetchSlots,
  placeOrder,
  type ItemChange,
  type SlotList,
  type SlotView,
  type UnavailableReason,
} from '../../api/orders'
import { Alert } from '../../components/Alert'
import { formatMoney } from '../../lib/format'
import { slotRange, timeOf } from '../../lib/orderLabels'
import { useCart } from '../cart/useCart'

type SlotsState =
  { kind: 'loading' } | { kind: 'error'; message: string } | { kind: 'ready'; list: SlotList }

const UNAVAILABLE_MESSAGES: Record<UnavailableReason, string> = {
  NO_SLOT_LEFT_TODAY: 'Hôm nay không còn khung nhận. Giỏ hàng của bạn vẫn được giữ.',
  ALL_SLOTS_FULL: 'Các khung hôm nay đã hết chỗ. Bạn có thể bớt món rồi thử lại.',
  ORDER_TOO_LARGE: 'Đơn quá lớn cho một khung giờ. Vui lòng bớt món hoặc liên hệ cửa hàng.',
}

/** Nhóm các khung theo giờ để dễ quét bằng mắt (UI Prototype mục 2): 11 giờ, 12 giờ... */
function groupByHour(slots: SlotView[]): [string, SlotView[]][] {
  const groups = new Map<string, SlotView[]>()
  for (const slot of slots) {
    const hour = timeOf(slot.start).slice(0, 2)
    groups.set(hour, [...(groups.get(hour) ?? []), slot])
  }
  return [...groups.entries()]
}

function itemChangesOf(error: ApiError): ItemChange[] {
  const items = error.extra.items
  return Array.isArray(items) ? (items as ItemChange[]) : []
}

/**
 * UC-04.2 bước 5–11: chọn khung giờ, xem lại và xác nhận đặt. Mã yêu cầu sinh một lần khi vào màn và giữ nguyên cho mọi
 * lần bấm "Xác nhận đặt", nên bấm nhiều lần hoặc gửi lại sau khi mất mạng không tạo thêm đơn (A1, BR-17). Nơi quay về
 * khi lỗi theo bảng "Lỗi khi gửi đơn" của UI Prototype mục 2.
 */
export function CheckoutPage() {
  const { lines, total, reprice, clear } = useCart()
  const navigate = useNavigate()
  const [requestId] = useState(() => crypto.randomUUID())
  const [slots, setSlots] = useState<SlotsState>({ kind: 'loading' })
  const [selected, setSelected] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<{ message: string; showOrders?: boolean } | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [alternatives, setAlternatives] = useState<SlotView[]>([])
  // Đặt xong thì giỏ bị xóa; cờ này để việc giỏ trống lúc đó không bị hiểu là A2 và đẩy về giỏ.
  const [placed, setPlaced] = useState(false)

  const cartKey = useMemo(
    () => lines.map((line) => `${line.menuItemId}:${line.quantity}`).join(','),
    [lines],
  )

  useEffect(() => {
    if (cartKey === '') return
    const controller = new AbortController()
    const request = cartKey.split(',').map((part) => {
      const [id, quantity] = part.split(':').map(Number)
      return { menuItemId: id, quantity }
    })
    fetchSlots(request, controller.signal)
      .then((list) => {
        setSlots({ kind: 'ready', list })
        // Khung đã chọn mà vừa hết chỗ hoặc không còn hợp lệ thì bỏ chọn.
        setSelected((current) =>
          list.slots.some((slot) => slot.start === current && slot.bookable) ? current : null,
        )
      })
      .catch((e: unknown) => {
        if (controller.signal.aborted) return
        setSlots({ kind: 'error', message: messageOf(e) })
      })
    return () => controller.abort()
  }, [cartKey, reloadKey])

  const reloadSlots = useCallback(() => {
    setSlots({ kind: 'loading' })
    setReloadKey((value) => value + 1)
  }, [])

  // A2: giỏ trống thì không có gì để đặt.
  if (lines.length === 0) return placed ? null : <Navigate to="/cart" replace />

  async function confirm() {
    if (!selected || submitting) return
    setSubmitting(true)
    setError(null)
    setAlternatives([])
    try {
      const order = await placeOrder({
        requestId,
        slotStart: selected,
        lines: lines.map((line) => ({
          menuItemId: line.menuItemId,
          quantity: line.quantity,
          unitPrice: line.unitPrice,
        })),
      })
      setPlaced(true)
      navigate(`/orders/${order.id}`, { replace: true, state: { order, placed: true } })
      clear()
    } catch (e) {
      handleError(e)
      setSubmitting(false)
    }
  }

  function handleError(e: unknown) {
    if (!(e instanceof ApiError)) {
      setError({ message: messageOf(e) })
      return
    }
    switch (e.code) {
      case 'UNAUTHENTICATED':
        // E11: phiên hết hạn; giỏ nằm trên trình duyệt nên vẫn còn sau khi đăng nhập lại.
        navigate('/login', { state: { from: '/checkout' } })
        return
      case 'ITEMS_CHANGED': {
        // E7: cập nhật giỏ theo món đã đổi rồi về giỏ để khách xem lại (không bao giờ tính giá khách chưa thấy).
        const changes = itemChangesOf(e)
        const notices = changes.flatMap((change) => {
          const line = lines.find((item) => item.menuItemId === change.menuItemId)
          if (!line) return []
          return change.reason === 'PRICE_CHANGED' && change.currentPrice !== undefined
            ? [`${line.name}: giá mới ${formatMoney(change.currentPrice)}.`]
            : [`${line.name}: không còn bán, đã bỏ khỏi giỏ.`]
        })
        reprice(changes)
        navigate('/cart', { state: { notices } })
        return
      }
      case 'ORDER_TOO_LARGE':
        // E9: đổi khung cũng vô ích, về giỏ để bớt món.
        navigate('/cart', { state: { notices: [messageOf(e)] } })
        return
      case 'SLOT_NOT_BOOKABLE':
      case 'SLOT_FULL':
        // E6, E8: ở lại, tải lại danh sách khung để chọn khung khác; khung hết chỗ thì kèm tối đa 3 gợi ý (UC-18).
        setSelected(null)
        setError({ message: messageOf(e) })
        setAlternatives(
          Array.isArray(e.extra.alternatives) ? (e.extra.alternatives as SlotView[]) : [],
        )
        reloadSlots()
        return
      case 'OPEN_ORDER_LIMIT_REACHED':
        // E5: giữ giỏ, chỉ báo và dẫn tới "Đơn của tôi".
        setError({ message: messageOf(e), showOrders: true })
        return
      default:
        // E10 và lỗi khác: giữ giỏ, khách bấm lại an toàn nhờ mã yêu cầu không đổi.
        setError({ message: messageOf(e) })
    }
  }

  const chosen =
    slots.kind === 'ready' ? slots.list.slots.find((s) => s.start === selected) : undefined

  return (
    <div className="content">
      <h1>Chọn giờ nhận</h1>

      {slots.kind === 'loading' && (
        <p className="muted center" role="status">
          Đang tải các khung giờ...
        </p>
      )}

      {slots.kind === 'error' && (
        <>
          <Alert kind="danger">{slots.message}</Alert>
          <button type="button" className="btn btn-primary btn-block" onClick={reloadSlots}>
            Thử lại
          </button>
        </>
      )}

      {slots.kind === 'ready' && slots.list.unavailableReason && (
        <>
          <Alert kind="warning">{UNAVAILABLE_MESSAGES[slots.list.unavailableReason]}</Alert>
          <Link className="btn btn-block" to="/cart">
            Về giỏ hàng
          </Link>
        </>
      )}

      {error && (
        <Alert kind="danger">
          {error.message}
          {error.showOrders && (
            <>
              {' '}
              <Link to="/orders">Xem đơn của tôi</Link>
            </>
          )}
        </Alert>
      )}

      {alternatives.length > 0 && (
        <section className="alternatives" aria-label="Khung gợi ý">
          <p className="notice-title">Khung gợi ý còn chỗ cho giỏ của bạn:</p>
          <div className="slot-grid">
            {alternatives.map((slot) => (
              <button
                key={slot.start}
                type="button"
                className={`slot${slot.start === selected ? ' selected' : ''}`}
                aria-pressed={slot.start === selected}
                aria-label={`Gợi ý ${timeOf(slot.start)}`}
                onClick={() => setSelected(slot.start)}
              >
                {timeOf(slot.start)}
              </button>
            ))}
          </div>
        </section>
      )}

      {slots.kind === 'ready' && slots.list.slots.length > 0 && (
        <>
          <p className="muted">Hôm nay, mỗi khung 15 phút. Khung xám đã hết chỗ.</p>
          {groupByHour(slots.list.slots).map(([hour, hourSlots]) => (
            <section key={hour} aria-label={`${hour} giờ`}>
              <h2 className="slot-hour">{hour} giờ</h2>
              <div className="slot-grid">
                {hourSlots.map((slot) => (
                  <button
                    key={slot.start}
                    type="button"
                    className={`slot${slot.start === selected ? ' selected' : ''}${slot.bookable ? '' : ' full'}`}
                    disabled={!slot.bookable}
                    aria-pressed={slot.start === selected}
                    aria-label={`${timeOf(slot.start)}${slot.bookable ? '' : ', không đủ chỗ'}`}
                    onClick={() => setSelected(slot.start)}
                  >
                    {timeOf(slot.start)}
                  </button>
                ))}
              </div>
            </section>
          ))}
        </>
      )}

      <h2 className="section-title">Tóm tắt đơn</h2>
      <ul className="summary">
        {lines.map((line) => (
          <li key={line.menuItemId}>
            <span>
              {line.quantity} × {line.name}
            </span>
            <span>{formatMoney(line.unitPrice * line.quantity)}</span>
          </li>
        ))}
      </ul>
      <dl className="cart-total">
        <dt>Tổng cộng</dt>
        <dd>{formatMoney(total)}</dd>
      </dl>
      <p className="muted">
        {chosen ? `Nhận lúc ${slotRange(chosen.start, chosen.end)}. ` : ''}Thanh toán tại quầy khi
        nhận món.
      </p>

      <div className="sticky-action">
        <button
          type="button"
          className="btn btn-primary btn-block"
          disabled={!selected || submitting}
          onClick={confirm}
        >
          {submitting ? 'Đang đặt...' : 'Xác nhận đặt'}
        </button>
      </div>
    </div>
  )
}
