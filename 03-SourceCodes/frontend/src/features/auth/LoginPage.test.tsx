import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import type { CurrentUser } from '../../api/auth'
import { ApiError } from '../../api/client'
import { renderWithAuth } from '../../test/renderWithAuth'
import { LoginPage } from './LoginPage'

const customer: CurrentUser = { id: 7, displayName: 'Khách Một', role: 'CUSTOMER' }
const kitchen: CurrentUser = { id: 9, displayName: 'Bếp Một', role: 'KITCHEN' }

const destinations = (
  <>
    <Route path="/" element={<p>TRANG CHỦ</p>} />
    <Route path="/kitchen" element={<p>HÀNG CHỜ BẾP</p>} />
    <Route path="/orders" element={<p>ĐƠN CỦA TÔI</p>} />
  </>
)

async function fillAndSubmit(loginId: string, password: string) {
  const user = userEvent.setup()
  if (loginId) await user.type(screen.getByLabelText('Email hoặc số điện thoại'), loginId)
  if (password) await user.type(screen.getByLabelText('Mật khẩu'), password)
  await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))
}

describe('LoginPage', () => {
  it('bỏ trống cả hai ô thì báo từng trường và không gọi backend (UC-02 E3)', async () => {
    const signIn = vi.fn()
    renderWithAuth(<LoginPage />, { path: '/login', auth: { signIn }, extraRoutes: destinations })

    await fillAndSubmit('', '')

    expect(screen.getByText('Vui lòng nhập email hoặc số điện thoại')).toBeInTheDocument()
    expect(screen.getByText('Vui lòng nhập mật khẩu')).toBeInTheDocument()
    expect(signIn).not.toHaveBeenCalled()
  })

  it('chỉ thiếu mật khẩu thì chỉ báo ô mật khẩu', async () => {
    renderWithAuth(<LoginPage />, { path: '/login', extraRoutes: destinations })

    await fillAndSubmit('khach1@example.com', '')

    expect(screen.queryByText('Vui lòng nhập email hoặc số điện thoại')).not.toBeInTheDocument()
    expect(screen.getByText('Vui lòng nhập mật khẩu')).toBeInTheDocument()
  })

  it('sai thông tin thì báo lỗi chung, giữ định danh và xóa ô mật khẩu (UC-02 E1)', async () => {
    const signIn = vi.fn().mockRejectedValue(new ApiError(401, 'INVALID_CREDENTIALS', 'x'))
    renderWithAuth(<LoginPage />, { path: '/login', auth: { signIn }, extraRoutes: destinations })

    await fillAndSubmit('khach1@example.com', 'sai-mat-khau')

    expect(await screen.findByRole('alert')).toHaveTextContent('Thông tin đăng nhập không đúng.')
    expect(screen.getByLabelText('Email hoặc số điện thoại')).toHaveValue('khach1@example.com')
    expect(screen.getByLabelText('Mật khẩu')).toHaveValue('')
  })

  it('tài khoản bị khóa thì báo đúng lý do (UC-02 E2)', async () => {
    const signIn = vi.fn().mockRejectedValue(new ApiError(403, 'ACCOUNT_LOCKED', 'x'))
    renderWithAuth(<LoginPage />, { path: '/login', auth: { signIn }, extraRoutes: destinations })

    await fillAndSubmit('khoa@example.com', 'mat-khau')

    expect(await screen.findByRole('alert')).toHaveTextContent('Tài khoản đã bị khóa')
  })

  it('không kết nối được máy chủ thì báo lỗi kết nối', async () => {
    const signIn = vi.fn().mockRejectedValue(new ApiError(0, null, 'x'))
    renderWithAuth(<LoginPage />, { path: '/login', auth: { signIn }, extraRoutes: destinations })

    await fillAndSubmit('khach1@example.com', 'mat-khau')

    expect(await screen.findByRole('alert')).toHaveTextContent('kết nối')
  })

  it('đăng nhập thành công: khách vào thực đơn, nhân viên bếp vào hàng chờ bếp', async () => {
    const signIn = vi.fn().mockResolvedValue(customer)
    const view = renderWithAuth(<LoginPage />, {
      path: '/login',
      auth: { signIn },
      extraRoutes: destinations,
    })
    await fillAndSubmit('khach1@example.com', 'mat-khau')
    expect(await screen.findByText('TRANG CHỦ')).toBeInTheDocument()
    expect(signIn).toHaveBeenCalledWith('khach1@example.com', 'mat-khau')
    view.unmount()

    renderWithAuth(<LoginPage />, {
      path: '/login',
      auth: { signIn: vi.fn().mockResolvedValue(kitchen) },
      extraRoutes: destinations,
    })
    await fillAndSubmit('bep1@example.com', 'mat-khau')
    expect(await screen.findByText('HÀNG CHỜ BẾP')).toBeInTheDocument()
  })

  it('được chuyển tới đăng nhập từ một trang thì đăng nhập xong quay lại trang đó (UC-02 E4)', async () => {
    const signIn = vi.fn().mockResolvedValue(customer)
    renderWithAuth(<LoginPage />, {
      path: { pathname: '/login', state: { from: '/orders' } },
      auth: { signIn },
      extraRoutes: destinations,
    })

    await fillAndSubmit('khach1@example.com', 'mat-khau')

    expect(await screen.findByText('ĐƠN CỦA TÔI')).toBeInTheDocument()
  })

  it('đã đăng nhập mà mở trang đăng nhập thì chuyển thẳng tới màn hình chính của vai trò', async () => {
    renderWithAuth(<LoginPage />, {
      path: '/login',
      auth: { user: kitchen },
      extraRoutes: destinations,
    })

    await waitFor(() => expect(screen.getByText('HÀNG CHỜ BẾP')).toBeInTheDocument())
  })
})
