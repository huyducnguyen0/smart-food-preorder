import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { CurrentUser } from '../api/auth'
import { saveCart } from '../features/cart/cartStorage'
import { renderWithAuth } from '../test/renderWithAuth'
import { AppShell } from './AppShell'

const customer: CurrentUser = { id: 1, displayName: 'Khách Một', role: 'CUSTOMER' }
const kitchen: CurrentUser = { id: 2, displayName: 'Bếp Một', role: 'KITCHEN' }
const manager: CurrentUser = { id: 3, displayName: 'Quản Lý', role: 'MANAGER' }

describe('AppShell', () => {
  it('khách vãng lai chỉ thấy Thực đơn và Đăng nhập', () => {
    renderWithAuth(<AppShell />)

    const tabs = screen.getByRole('navigation', { name: 'Điều hướng chính' })
    expect(tabs).toHaveTextContent('Thực đơn')
    expect(tabs).toHaveTextContent('Đăng nhập')
    expect(tabs).not.toHaveTextContent('Đơn của tôi')
    expect(tabs).not.toHaveTextContent('Đăng xuất')
  })

  it('khách đã đăng nhập thấy Thực đơn, Đơn của tôi và Đăng xuất', () => {
    renderWithAuth(<AppShell />, { auth: { user: customer } })

    const tabs = screen.getByRole('navigation', { name: 'Điều hướng chính' })
    expect(tabs).toHaveTextContent('Đơn của tôi')
    expect(tabs).toHaveTextContent('Đăng xuất')
    expect(tabs).not.toHaveTextContent('Đăng nhập')
  })

  it('quản lý chưa có chức năng riêng ở Sprint 1: không thấy Đơn của tôi', () => {
    renderWithAuth(<AppShell />, { auth: { user: manager } })

    expect(screen.getByRole('navigation', { name: 'Điều hướng chính' })).not.toHaveTextContent(
      'Đơn của tôi',
    )
  })

  it('nhân viên bếp dùng màn rộng với đầu trang riêng, không có thanh tab của khách', () => {
    renderWithAuth(<AppShell />, { auth: { user: kitchen } })

    expect(screen.getByRole('heading', { name: 'Hàng chờ bếp' })).toBeInTheDocument()
    expect(screen.getByText('Bếp Một')).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Điều hướng chính' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Đăng xuất' })).toBeInTheDocument()
  })

  it('khách thấy biểu tượng giỏ kèm số phần; khách vãng lai và bếp không có giỏ', () => {
    saveCart(customer.id, [
      { menuItemId: 1, name: 'Burger bò', imageUrl: null, unitPrice: 45000, quantity: 2 },
      { menuItemId: 2, name: 'Trà đào', imageUrl: null, unitPrice: 20000, quantity: 1 },
    ])
    const { unmount } = renderWithAuth(<AppShell />, { auth: { user: customer } })
    expect(screen.getByRole('link', { name: 'Giỏ hàng, 3 phần' })).toHaveAttribute('href', '/cart')
    unmount()

    renderWithAuth(<AppShell />)
    expect(screen.queryByRole('link', { name: /Giỏ hàng/ })).not.toBeInTheDocument()
  })
})
