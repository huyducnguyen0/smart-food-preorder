import { Link } from 'react-router'
import { formatMoney } from '../../lib/format'
import { useCart } from './useCart'

/** Thanh giỏ dính đáy trang thực đơn khi giỏ có món: số phần, tổng tiền, nút xem giỏ (UI Prototype mục 2). */
export function CartBar() {
  const { count, total } = useCart()
  if (count === 0) return null
  return (
    <div className="cart-bar">
      <span>
        <b>{count} phần</b> · {formatMoney(total)}
      </span>
      <Link className="btn btn-primary" to="/cart">
        Xem giỏ
      </Link>
    </div>
  )
}
