import { screen } from '@testing-library/react'
import { Route } from 'react-router'
import { describe, expect, it } from 'vitest'
import type { CurrentUser } from '../api/auth'
import { renderWithAuth } from '../test/renderWithAuth'
import { RequireRole } from './RequireRole'

const customer: CurrentUser = { id: 1, displayName: 'Khách', role: 'CUSTOMER' }
const kitchen: CurrentUser = { id: 2, displayName: 'Bếp', role: 'KITCHEN' }

const guarded = (
  <RequireRole roles={['KITCHEN']}>
    <p>TRANG CỦA BẾP</p>
  </RequireRole>
)
const others = (
  <>
    <Route path="/login" element={<p>TRANG ĐĂNG NHẬP</p>} />
    <Route path="/forbidden" element={<p>KHÔNG CÓ QUYỀN</p>} />
  </>
)

describe('RequireRole', () => {
  it('đúng vai trò thì hiện nội dung', () => {
    renderWithAuth(guarded, { path: '/kitchen', auth: { user: kitchen }, extraRoutes: others })

    expect(screen.getByText('TRANG CỦA BẾP')).toBeInTheDocument()
  })

  it('chưa đăng nhập thì chuyển tới đăng nhập (UC-02 E4)', () => {
    renderWithAuth(guarded, { path: '/kitchen', extraRoutes: others })

    expect(screen.getByText('TRANG ĐĂNG NHẬP')).toBeInTheDocument()
    expect(screen.queryByText('TRANG CỦA BẾP')).not.toBeInTheDocument()
  })

  it('sai vai trò thì chuyển tới trang không có quyền (UC-02 E5)', () => {
    renderWithAuth(guarded, { path: '/kitchen', auth: { user: customer }, extraRoutes: others })

    expect(screen.getByText('KHÔNG CÓ QUYỀN')).toBeInTheDocument()
    expect(screen.queryByText('TRANG CỦA BẾP')).not.toBeInTheDocument()
  })

  it('đang hỏi backend phiên còn hiệu lực không thì chờ, chưa chuyển trang', () => {
    renderWithAuth(guarded, { path: '/kitchen', auth: { status: 'loading' }, extraRoutes: others })

    expect(screen.getByRole('status')).toHaveTextContent('Đang tải')
    expect(screen.queryByText('TRANG ĐĂNG NHẬP')).not.toBeInTheDocument()
  })
})
