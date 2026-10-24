import { Link, useLocation } from 'react-router'
import { Alert } from '../../components/Alert'
import { Icon } from '../../components/Icon'
import { formatMoney } from '../../lib/format'
import { MAX_QUANTITY, type CartLine } from './cartContext'
import { useCart } from './useCart'

function CartRow({ line }: { line: CartLine }) {
  const { setQuantity, remove } = useCart()
  return (
    <li className="item">
      {line.imageUrl ? (
        <img className="thumb thumb-sm" src={line.imageUrl} alt="" loading="lazy" />
      ) : (
        <div className="thumb thumb-sm thumb-empty" aria-hidden="true">
          <Icon name="menu" />
        </div>
      )}
      <div className="item-body">
        <b>{line.name}</b>
        <span className="muted">{formatMoney(line.unitPrice)} / phần</span>
        <div className="stepper">
          <button
            type="button"
            aria-label={`Giảm số lượng ${line.name}`}
            disabled={line.quantity <= 1}
            onClick={() => setQuantity(line.menuItemId, line.quantity - 1)}
          >
            <Icon name="minus" />
          </button>
          <output aria-label={`Số lượng ${line.name}`}>{line.quantity}</output>
          <button
            type="button"
            aria-label={`Tăng số lượng ${line.name}`}
            disabled={line.quantity >= MAX_QUANTITY}
            onClick={() => setQuantity(line.menuItemId, line.quantity + 1)}
          >
            <Icon name="plus" />
          </button>
        </div>
      </div>
      <div className="line-end">
        <span className="price">{formatMoney(line.unitPrice * line.quantity)}</span>
        <button
          type="button"
          className="link-btn danger-link"
          aria-label={`Bỏ ${line.name} khỏi giỏ`}
          onClick={() => remove(line.menuItemId)}
        >
          Bỏ
        </button>
      </div>
    </li>
  )
}

/**
 * UC-04.1 Quản lý giỏ hàng (bước 3–4): xem từng dòng, đổi số lượng, bỏ món, thấy tổng tiền cập nhật ngay.
 * Giỏ trống thì không chọn được giờ nhận (A2).
 */
export function CartPage() {
  const { lines, total } = useCart()
  // Thông báo từ màn đặt đơn khi món đổi giá, hết hàng hoặc đơn quá lớn (UC-04 E7, E9).
  const notices = (useLocation().state as { notices?: string[] } | null)?.notices ?? []

  if (lines.length === 0) {
    return (
      <div className="empty">
        {notices.length > 0 && <Alert kind="warning">{notices.join(' ')}</Alert>}
        <h1>Giỏ hàng đang trống</h1>
        <p className="muted">Chọn món từ thực đơn để bắt đầu đặt trước.</p>
        <Link className="btn btn-primary" to="/">
          Xem thực đơn
        </Link>
      </div>
    )
  }

  return (
    <div className="content">
      <h1>Giỏ hàng</h1>
      {notices.length > 0 && (
        <Alert kind="warning">
          <p className="notice-title">Giỏ hàng đã được cập nhật, vui lòng xem lại trước khi đặt:</p>
          <ul className="notice-list">
            {notices.map((notice) => (
              <li key={notice}>{notice}</li>
            ))}
          </ul>
        </Alert>
      )}
      <ul className="items">
        {lines.map((line) => (
          <CartRow key={line.menuItemId} line={line} />
        ))}
      </ul>
      <dl className="cart-total">
        <dt>Tổng cộng</dt>
        <dd>{formatMoney(total)}</dd>
      </dl>
      <p className="muted">Thanh toán tại quầy khi nhận món.</p>
      <div className="sticky-action">
        <Link className="btn btn-primary btn-block" to="/checkout">
          Chọn giờ nhận
        </Link>
      </div>
    </div>
  )
}
