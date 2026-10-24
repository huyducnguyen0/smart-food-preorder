import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { CurrentUser } from '../../api/auth'
import { ApiError } from '../../api/client'
import * as menuApi from '../../api/menu'
import type { MenuCategory } from '../../api/menu'
import { renderWithAuth } from '../../test/renderWithAuth'
import { MenuPage } from './MenuPage'

vi.mock('../../api/menu')

const customer: CurrentUser = { id: 1, displayName: 'Khách Một', role: 'CUSTOMER' }

const menu: MenuCategory[] = [
  {
    id: 1,
    name: 'Burger',
    items: [
      {
        id: 12,
        name: 'Burger bò',
        description: 'Bò nướng',
        imageUrl: 'https://example.com/bo.jpg',
        price: 45000,
        available: true,
      },
      {
        id: 13,
        name: 'Burger gà giòn',
        description: null,
        imageUrl: null,
        price: 39000,
        available: false,
      },
    ],
  },
  {
    id: 3,
    name: 'Đồ uống',
    items: [
      { id: 31, name: 'Trà đào', description: null, imageUrl: null, price: 20000, available: true },
    ],
  },
]

const loginRoute = <Route path="/login" element={<p>TRANG ĐĂNG NHẬP</p>} />

describe('MenuPage', () => {
  beforeEach(() => {
    vi.mocked(menuApi.fetchMenu).mockReset()
  })

  it('hiện món theo nhóm kèm giá (UC-03 bước 2)', async () => {
    vi.mocked(menuApi.fetchMenu).mockResolvedValue(menu)
    renderWithAuth(<MenuPage />)

    expect(await screen.findByRole('heading', { name: 'Burger' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Đồ uống' })).toBeInTheDocument()
    expect(screen.getByText('Burger bò')).toBeInTheDocument()
    expect(screen.getByText('45.000đ')).toBeInTheDocument()
    expect(screen.getByText('20.000đ')).toBeInTheDocument()
  })

  it('món tạm hết có nhãn và không có nút thêm vào giỏ (BR-04, A2)', async () => {
    vi.mocked(menuApi.fetchMenu).mockResolvedValue(menu)
    renderWithAuth(<MenuPage />)

    await screen.findByText('Burger gà giòn')

    expect(screen.getByText('Tạm hết')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Thêm Burger bò vào giỏ' })).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Thêm Burger gà giòn vào giỏ' }),
    ).not.toBeInTheDocument()
  })

  it('lọc theo nhóm món (UC-03 bước 3)', async () => {
    vi.mocked(menuApi.fetchMenu).mockResolvedValue(menu)
    renderWithAuth(<MenuPage />)
    await screen.findByText('Burger bò')

    await userEvent.click(
      within(screen.getByRole('navigation', { name: 'Nhóm món' })).getByRole('button', {
        name: 'Đồ uống',
      }),
    )

    expect(screen.getByText('Trà đào')).toBeInTheDocument()
    expect(screen.queryByText('Burger bò')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Tất cả' }))
    expect(screen.getByText('Burger bò')).toBeInTheDocument()
  })

  it('khách vãng lai bấm thêm vào giỏ thì phải đăng nhập, món chưa được thêm (UC-03 A3)', async () => {
    vi.mocked(menuApi.fetchMenu).mockResolvedValue(menu)
    renderWithAuth(<MenuPage />, { extraRoutes: loginRoute })
    await screen.findByText('Burger bò')

    await userEvent.click(screen.getByRole('button', { name: 'Thêm Burger bò vào giỏ' }))

    expect(screen.getByText('TRANG ĐĂNG NHẬP')).toBeInTheDocument()
  })

  it('xem menu không cần đăng nhập, và khách đã đăng nhập thấy cùng nội dung', async () => {
    vi.mocked(menuApi.fetchMenu).mockResolvedValue(menu)
    renderWithAuth(<MenuPage />, { auth: { user: customer } })

    expect(await screen.findByText('Burger bò')).toBeInTheDocument()
    expect(screen.getByText('Trà đào')).toBeInTheDocument()
  })

  it('menu rỗng thì báo chưa có món, không phải lỗi (UC-03 A1)', async () => {
    vi.mocked(menuApi.fetchMenu).mockResolvedValue([])
    renderWithAuth(<MenuPage />)

    expect(await screen.findByText('Hiện chưa có món.')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('lỗi tải thì báo lỗi, không hiện món, và thử lại được (UC-03 E1)', async () => {
    vi.mocked(menuApi.fetchMenu)
      .mockRejectedValueOnce(new ApiError(0, null, 'x'))
      .mockResolvedValueOnce(menu)
    renderWithAuth(<MenuPage />)

    expect(await screen.findByRole('alert')).toHaveTextContent('kết nối')
    expect(screen.queryByText('Burger bò')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByText('Burger bò')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
