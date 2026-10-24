import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route } from 'react-router'
import { describe, expect, it } from 'vitest'
import type { CurrentUser } from '../../api/auth'
import { renderWithAuth } from '../../test/renderWithAuth'
import type { CartLine } from './cartContext'
import { CartPage } from './CartPage'
import { saveCart } from './cartStorage'

const customer: CurrentUser = { id: 1, displayName: 'Khách Một', role: 'CUSTOMER' }

const burger: CartLine = {
  menuItemId: 12,
  name: 'Burger bò',
  imageUrl: null,
  unitPrice: 45000,
  quantity: 2,
}
const tea: CartLine = {
  menuItemId: 31,
  name: 'Trà đào',
  imageUrl: null,
  unitPrice: 20000,
  quantity: 1,
}

const routes = (
  <>
    <Route path="/" element={<p>TRANG THỰC ĐƠN</p>} />
    <Route path="/checkout" element={<p>TRANG CHỌN GIỜ</p>} />
  </>
)

function renderCart(lines: CartLine[]) {
  saveCart(customer.id, lines)
  return renderWithAuth(<CartPage />, {
    path: '/cart',
    auth: { user: customer },
    extraRoutes: routes,
  })
}

describe('CartPage', () => {
  it('hiện từng dòng với đơn giá, thành tiền và tổng tiền (UC-04 bước 4)', () => {
    renderCart([burger, tea])

    expect(screen.getByText('Burger bò')).toBeInTheDocument()
    expect(screen.getByText('90.000đ')).toBeInTheDocument() // 2 × 45.000
    expect(screen.getByText('20.000đ', { selector: '.price' })).toBeInTheDocument()
    expect(screen.getByText('110.000đ')).toBeInTheDocument() // tổng
    expect(screen.getByText(/Thanh toán tại quầy/)).toBeInTheDocument()
  })

  it('đổi số lượng thì thành tiền và tổng cập nhật ngay (UC-04 bước 3–4)', async () => {
    renderCart([burger, tea])

    await userEvent.click(screen.getByRole('button', { name: 'Tăng số lượng Trà đào' }))
    expect(screen.getByRole('status', { name: 'Số lượng Trà đào' })).toHaveTextContent('2')
    expect(screen.getByText('40.000đ', { selector: '.price' })).toBeInTheDocument() // 2 × 20.000
    expect(screen.getByText('130.000đ')).toBeInTheDocument() // 90.000 + 40.000
  })

  it('không giảm dưới 1; muốn bỏ món thì dùng nút Bỏ', async () => {
    renderCart([tea])

    expect(screen.getByRole('button', { name: 'Giảm số lượng Trà đào' })).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'Bỏ Trà đào khỏi giỏ' }))
    expect(screen.getByText('Giỏ hàng đang trống')).toBeInTheDocument()
  })

  it('giỏ trống: báo trống, không có nút chọn giờ nhận, quay lại thực đơn được (A2)', async () => {
    renderCart([])

    expect(screen.getByText('Giỏ hàng đang trống')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Chọn giờ nhận' })).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('link', { name: 'Xem thực đơn' }))
    expect(screen.getByText('TRANG THỰC ĐƠN')).toBeInTheDocument()
  })

  it('giỏ có món thì chọn giờ nhận được', async () => {
    renderCart([burger])

    await userEvent.click(screen.getByRole('link', { name: 'Chọn giờ nhận' }))

    expect(screen.getByText('TRANG CHỌN GIỜ')).toBeInTheDocument()
  })

  it('giỏ còn nguyên khi dựng lại giao diện, ví dụ sau khi phiên hết hạn rồi đăng nhập lại (E11)', async () => {
    const first = renderCart([tea])
    await userEvent.click(screen.getByRole('button', { name: 'Tăng số lượng Trà đào' }))
    first.unmount()

    renderWithAuth(<CartPage />, { path: '/cart', auth: { user: customer }, extraRoutes: routes })

    expect(screen.getByRole('status', { name: 'Số lượng Trà đào' })).toHaveTextContent('2')
  })

  it('mỗi tài khoản một giỏ riêng', () => {
    saveCart(1, [burger])
    renderWithAuth(<CartPage />, {
      path: '/cart',
      auth: { user: { id: 2, displayName: 'Khách Hai', role: 'CUSTOMER' } },
      extraRoutes: routes,
    })

    expect(screen.getByText('Giỏ hàng đang trống')).toBeInTheDocument()
  })
})
