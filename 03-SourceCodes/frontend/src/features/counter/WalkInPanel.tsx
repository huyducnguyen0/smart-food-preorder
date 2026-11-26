import { useEffect, useMemo, useState } from 'react'
import { ApiError } from '../../api/client'
import {
  confirmWalkIn,
  estimateWalkIn,
  type PaymentMethod,
  type WalkInSlot,
} from '../../api/counter'
import { messageOf } from '../../api/errorMessages'
import { fetchMenu, type MenuItem } from '../../api/menu'
import type { OrderDetail } from '../../api/orders'
import { Alert } from '../../components/Alert'
import { formatMoney } from '../../lib/format'
import { slotRange } from '../../lib/orderLabels'

/**
 * UC-07 Tạo đơn walk-in: nhân viên chọn món cho khách mua trực tiếp, xem khung dự kiến sớm nhất (BR-18) và chỉ xác
 * nhận khi khách đồng ý; lúc xác nhận ghi nhận thanh toán (BR-20). Khung vừa hết chỗ thì hiện khung mới để báo lại
 * khách (BR-19). Mã yêu cầu giữ nguyên cho mọi lần bấm xác nhận của cùng một đơn (NFR-02).
 */
export function WalkInPanel({ onCreated }: { onCreated: () => void }) {
  const [items, setItems] = useState<MenuItem[]>([])
  const [quantities, setQuantities] = useState<Record<number, number>>({})
  const [slot, setSlot] = useState<WalkInSlot | null>(null)
  const [total, setTotal] = useState<number | null>(null)
  const [requestId, setRequestId] = useState(() => crypto.randomUUID())
  const [notice, setNotice] = useState<{ kind: 'warning' | 'danger'; text: string } | null>(null)
  const [created, setCreated] = useState<OrderDetail | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    fetchMenu(controller.signal)
      .then((categories) => setItems(categories.flatMap((category) => category.items)))
      .catch((error: unknown) => {
        if (!controller.signal.aborted) setNotice({ kind: 'danger', text: messageOf(error) })
      })
    return () => controller.abort()
  }, [])

  const lines = useMemo(
    () =>
      items
        .filter((item) => (quantities[item.id] ?? 0) > 0)
        .map((item) => ({
          menuItemId: item.id,
          quantity: quantities[item.id],
          unitPrice: item.price,
        })),
    [items, quantities],
  )

  function change(item: MenuItem, delta: number) {
    setQuantities((current) => ({
      ...current,
      [item.id]: Math.max(0, Math.min(99, (current[item.id] ?? 0) + delta)),
    }))
    // Đổi món thì khung đã báo không còn đúng: phải xem lại trước khi xác nhận.
    setSlot(null)
    setTotal(null)
    setCreated(null)
  }

  async function handleEstimate() {
    setBusy(true)
    setNotice(null)
    try {
      const result = await estimateWalkIn(lines)
      setSlot(result.slot)
      setTotal(result.totalAmount)
    } catch (error) {
      setSlot(null)
      setNotice({ kind: 'danger', text: messageOf(error) })
    } finally {
      setBusy(false)
    }
  }

  async function handleConfirm(method: PaymentMethod) {
    if (!slot) return
    setBusy(true)
    setNotice(null)
    try {
      const order = await confirmWalkIn({
        requestId,
        slotStart: slot.start,
        lines,
        paymentMethod: method,
      })
      setCreated(order)
      setQuantities({})
      setSlot(null)
      setTotal(null)
      setRequestId(crypto.randomUUID())
      onCreated()
    } catch (error) {
      if (error instanceof ApiError && error.code === 'SLOT_FULL') {
        const next = error.extra.nextSlot as WalkInSlot | undefined
        setSlot(next ?? null)
        setNotice({
          kind: 'warning',
          text: next
            ? `Khung đã báo vừa hết chỗ. Khung mới: ${slotRange(next.start, next.end)}. Hỏi lại khách trước khi xác nhận.`
            : 'Khung đã báo vừa hết chỗ và hôm nay không còn khung nào đủ chỗ.',
        })
      } else {
        setNotice({ kind: 'danger', text: messageOf(error) })
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="k-card walk-in" aria-label="Tạo đơn tại quầy">
      <h2>Tạo đơn tại quầy</h2>
      {notice && <Alert kind={notice.kind}>{notice.text}</Alert>}
      {created && (
        <Alert kind="success">
          Đã tạo đơn, mã nhận <b>{created.pickupCode}</b>, nhận lúc{' '}
          {slotRange(created.slotStart, created.slotEnd)}. Đọc mã cho khách.
        </Alert>
      )}
      <ul className="walk-in-items">
        {items
          .filter((item) => item.available)
          .map((item) => (
            <li key={item.id}>
              <span>
                {item.name} <span className="muted">{formatMoney(item.price)}</span>
              </span>
              <span className="stepper">
                <button
                  type="button"
                  aria-label={`Bớt ${item.name}`}
                  disabled={busy || (quantities[item.id] ?? 0) === 0}
                  onClick={() => change(item, -1)}
                >
                  −
                </button>
                <output aria-label={`Số lượng ${item.name}`}>{quantities[item.id] ?? 0}</output>
                <button
                  type="button"
                  aria-label={`Thêm ${item.name}`}
                  disabled={busy}
                  onClick={() => change(item, 1)}
                >
                  +
                </button>
              </span>
            </li>
          ))}
      </ul>
      <button
        type="button"
        className="btn btn-block"
        disabled={busy || lines.length === 0}
        onClick={handleEstimate}
      >
        Xem khung dự kiến
      </button>
      {slot && (
        <div className="walk-in-confirm">
          <p>
            Dự kiến nhận lúc <b>{slotRange(slot.start, slot.end)}</b>
            {total !== null && (
              <>
                {' '}
                · cần thu <b>{formatMoney(total)}</b>
              </>
            )}
            . Khách đồng ý thì thu tiền và xác nhận:
          </p>
          <div className="c-actions">
            <button
              type="button"
              className="btn btn-primary"
              disabled={busy}
              onClick={() => void handleConfirm('CASH')}
            >
              Đã thu tiền mặt
            </button>
            <button
              type="button"
              className="btn"
              disabled={busy}
              onClick={() => void handleConfirm('BANK_TRANSFER')}
            >
              Đã thu chuyển khoản
            </button>
          </div>
        </div>
      )}
    </section>
  )
}
