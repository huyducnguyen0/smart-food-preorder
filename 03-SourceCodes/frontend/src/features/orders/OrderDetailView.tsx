import { Link } from 'react-router'
import type { OrderDetail } from '../../api/orders'
import { Alert } from '../../components/Alert'
import { formatMoney } from '../../lib/format'
import { slotRange, STATUS_LABELS } from '../../lib/orderLabels'

/**
 * Chi tiết một đơn (UC-05 bước 4) và kết quả đặt thành công (UC-04 bước 11): thẻ mã nhận, khung nhận, trạng thái,
 * các dòng và tổng tiền. Khách đọc mã nhận này cho nhân viên khi đến lấy (BR-38).
 */
export function OrderDetailView({
  order,
  placed = false,
}: {
  order: OrderDetail
  placed?: boolean
}) {
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
          Nhận lúc <b>{slotRange(order.slotStart, order.slotEnd)}</b>
        </span>
        <span className="status-pill">{STATUS_LABELS[order.status]}</span>
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
          ? 'Đã thanh toán.'
          : 'Chưa thanh toán: thanh toán tại quầy khi nhận món.'}
      </p>
      <Link className="btn btn-block" to="/">
        Về thực đơn
      </Link>
    </div>
  )
}
