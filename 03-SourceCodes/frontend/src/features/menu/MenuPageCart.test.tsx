import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { CurrentUser } from '../../api/auth'
import { ApiError } from '../../api/client'
import * as menuApi from '../../api/menu'
import type { MenuCategory, MenuItem } from '../../api/menu'
import { renderWithAuth } from '../../test/renderWithAuth'
import { loadCart } from '../cart/cartStorage'
import { MenuPage } from './MenuPage'

vi.mock('../../api/menu')

const customer: CurrentUser = { id: 1, displayName: 'Khách Một', role: 'CUSTOMER' }
const manager: CurrentUser = { id: 3, displayName: 'Quản Lý', role: 'MANAGER' }

const burger: MenuItem = {
  id: 12,
  name: 'Burger bò',
  description: null,
  imageUrl: null,
  price: 45000,
  available: true,
}
const menu: MenuCategory[] = [{ id: 1, name: 'Burger', items: [burger] }]

describe('MenuPage — thêm vào giỏ (UC-04.1)', () => {
  beforeEach(() => {
    vi.mocked(menuApi.fetchMenu).mockReset().mockResolvedValue(menu)
    vi.mocked(menuApi.fetchMenuItem).mockReset().mockResolvedValue(burger)
  })

  async function addBurger() {
    await screen.findByText('Burger bò')
    await userEvent.click(screen.getByRole('button', { name: 'Thêm Burger bò vào giỏ' }))
  }

  it('khách thêm món: giỏ ghi nhận giá đang hiển thị và thanh giỏ hiện ra (bước 1–2)', async () => {
    renderWithAuth(<MenuPage />, { auth: { user: customer } })
    expect(screen.queryByRole('link', { name: 'Xem giỏ' })).not.toBeInTheDocument()

    await addBurger()

    expect(await screen.findByRole('link', { name: 'Xem giỏ' })).toBeInTheDocument()
    expect(screen.getByText('1 phần')).toBeInTheDocument()
    expect(loadCart(customer.id)).toEqual([
      { menuItemId: 12, name: 'Burger bò', imageUrl: null, unitPrice: 45000, quantity: 1 },
    ])
    expect(menuApi.fetchMenuItem).toHaveBeenCalledWith(12)
  })

  it('thêm món đã có thì tăng số lượng, không thêm dòng mới (bước 2)', async () => {
    renderWithAuth(<MenuPage />, { auth: { user: customer } })

    await addBurger()
    await screen.findByText('1 phần')
    await userEvent.click(screen.getByRole('button', { name: 'Thêm Burger bò vào giỏ' }))

    expect(await screen.findByText('2 phần')).toBeInTheDocument()
    expect(screen.getByText(/90\.000đ/)).toBeInTheDocument()
    expect(loadCart(customer.id)).toHaveLength(1)
  })

  it('món vừa tạm hết: không thêm, báo khách và tải lại menu (E1)', async () => {
    vi.mocked(menuApi.fetchMenuItem).mockResolvedValue({ ...burger, available: false })
    renderWithAuth(<MenuPage />, { auth: { user: customer } })

    await addBurger()

    expect(await screen.findByRole('alert')).toHaveTextContent('Burger bò vừa tạm hết')
    expect(loadCart(customer.id)).toEqual([])
    expect(screen.queryByRole('link', { name: 'Xem giỏ' })).not.toBeInTheDocument()
    expect(menuApi.fetchMenu).toHaveBeenCalledTimes(2)
  })

  it('món đã ngừng bán: không thêm, báo khách và tải lại menu (E1)', async () => {
    vi.mocked(menuApi.fetchMenuItem).mockRejectedValue(
      new ApiError(404, 'MENU_ITEM_NOT_FOUND', 'x'),
    )
    renderWithAuth(<MenuPage />, { auth: { user: customer } })

    await addBurger()

    expect(await screen.findByRole('alert')).toHaveTextContent('không còn trong thực đơn')
    expect(loadCart(customer.id)).toEqual([])
    expect(menuApi.fetchMenu).toHaveBeenCalledTimes(2)
  })

  it('không kiểm tra được vì lỗi mạng: không thêm và báo lỗi, không làm mới menu', async () => {
    vi.mocked(menuApi.fetchMenuItem).mockRejectedValue(new ApiError(0, null, 'x'))
    renderWithAuth(<MenuPage />, { auth: { user: customer } })

    await addBurger()

    expect(await screen.findByRole('alert')).toHaveTextContent('kết nối')
    expect(loadCart(customer.id)).toEqual([])
    expect(menuApi.fetchMenu).toHaveBeenCalledTimes(1)
  })

  it('khách vãng lai vẫn bị chuyển đến đăng nhập và không có giỏ (UC-03 A3)', async () => {
    renderWithAuth(<MenuPage />, {
      extraRoutes: <Route path="/login" element={<p>TRANG ĐĂNG NHẬP</p>} />,
    })

    await addBurger()

    expect(screen.getByText('TRANG ĐĂNG NHẬP')).toBeInTheDocument()
    expect(menuApi.fetchMenuItem).not.toHaveBeenCalled()
  })

  it('vai trò không phải khách thì không có nút thêm vào giỏ', async () => {
    renderWithAuth(<MenuPage />, { auth: { user: manager } })

    await screen.findByText('Burger bò')

    expect(screen.queryByRole('button', { name: 'Thêm Burger bò vào giỏ' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Xem giỏ' })).not.toBeInTheDocument()
  })
})
