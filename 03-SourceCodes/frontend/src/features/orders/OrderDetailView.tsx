import { useState } from 'react'
import { Link } from 'react-router'
import type { OrderDetail } from '../../api/orders'
import { Alert } from '../../components/Alert'
import { formatMoney } from '../../lib/format'
import { dateOf, slotRange, STATUS_LABELS, STATUS_TONES, timeOf } from '../../lib/orderLabels'

/**
 * Chi tiết một đơn (UC-05 bước 4) và kết quả đặt thành công (UC-04 bước 11): thẻ mã nhận, khung nhận, trạng thái,
 * các dòng và tổng tiền. Khách đọc mã nhận này cho nhân viên khi đến lấy (BR-38).
 */
export function OrderDetailView({
  order,
  placed = false,
  onCancel,
  cancelling = false,
  cancelError = null,
}: {
  order: OrderDetail
  placed?: boolean
  /** Có thì hiện nút hủy cho đơn còn CONFIRMED (UC-06). */
  onCancel?: () => void
  cancelling?: boolean
  cancelError?: string | null
}) {
  const [confirming, setConfirming] = useState(false)
  return (
    <div className="content">
      {placed && (
        <Alert kind="success">
          Đặt đơn thành công. Hãy đến lấy trong khung giờ đã chọn và đọc mã nhận cho nhân viên.
        </Alert>
      )}
      <section className="ticket" aria-label="Mã nhận">
        <span className="ticket-label">Mã nhận</span>
        <strong className="ticket-code">{order.pickupCode}</strong>
        <span>
          Nhận lúc <b>{slotRange(order.slotStart, order.slotEnd)}</b>, {dateOf(order.slotStart)}
        </span>
        <span className={`status-pill tone-${STATUS_TONES[order.status]}`}>
          {STATUS_LABELS[order.status]}
        </span>
      </section>

      <h2 className="section-title">Món đã đặt</h2>
      <ul className="items">
        {order.lines.map((line) => (
          <li key={line.menuItemId} className="item">
            <div className="item-body">
              <b>
                {line.quantity} × {line.name}
              </b>
              <span className="muted">{formatMoney(line.unitPrice)} / phần</span>
            </div>
            <span className="price">{formatMoney(line.lineTotal)}</span>
          </li>
        ))}
      </ul>
      <dl className="cart-total">
        <dt>Tổng cộng</dt>
        <dd>{formatMoney(order.totalAmount)}</dd>
      </dl>
      <p className="muted">
        {order.paymentStatus === 'PAID'
          ? `Đã thanh toán${order.paymentMethod === 'BANK_TRANSFER' ? ' bằng chuyển khoản' : ' bằng tiền mặt'}.`
          : 'Chưa thanh toán: thanh toán tại quầy khi nhận món.'}{' '}
        Đặt lúc {order.confirmedAt.slice(11, 16)}.
      </p>
      {order.status === 'CANCELLED' && (
        <Alert kind="warning">
          {order.cancelReason
            ? `Cửa hàng đã hủy đơn. Lý do: ${order.cancelReason}`
            : 'Đơn đã được hủy.'}
        </Alert>
      )}
      {order.status === 'NO_SHOW' && (
        <Alert kind="warning">
          Đơn đã quá thời gian nhận nên cửa hàng ghi nhận không đến nhận.
        </Alert>
      )}
      {cancelError && <Alert kind="danger">{cancelError}</Alert>}
      {onCancel && order.status === 'CONFIRMED' && order.paymentStatus === 'PAID' && (
        <p className="muted">
          Đơn đã thanh toán. Muốn hủy, vui lòng liên hệ cửa hàng để được hủy và hoàn tiền.
        </p>
      )}
      {onCancel && order.status === 'CONFIRMED' && order.paymentStatus === 'UNPAID' && (
        <div className="cancel-box">
          {order.cancelDeadline && (
            <p className="muted">
              Bạn có thể tự hủy tới {timeOf(order.cancelDeadline)} nếu cửa hàng chưa bắt đầu làm
              món.
            </p>
          )}
          {confirming ? (
            <>
              <p>Hủy đơn {order.pickupCode}? Chỗ trong khung giờ sẽ được trả lại cho người khác.</p>
              <div className="admin-actions">
                <button
                  type="button"
                  className="btn btn-danger"
                  disabled={cancelling}
                  onClick={() => {
                    setConfirming(false)
                    onCancel()
                  }}
                >
                  Xác nhận hủy
                </button>
                <button type="button" className="btn" onClick={() => setConfirming(false)}>
                  Không hủy
                </button>
              </div>
            </>
          ) : (
            <button
              type="button"
              className="btn btn-block"
              disabled={cancelling}
              onClick={() => setConfirming(true)}
            >
              Hủy đơn
            </button>
          )}
        </div>
      )}
      <Link className="btn btn-block" to="/">
        Về thực đơn
      </Link>
    </div>
  )
}
