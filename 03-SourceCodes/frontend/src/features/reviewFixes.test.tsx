import { screen, within } from '@testing-library/react'
import { Route } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as adminApi from '../api/admin'
import type { Dashboard } from '../api/admin'
import type { CurrentUser } from '../api/auth'
import * as counterApi from '../api/counter'
import type { CounterOrder } from '../api/counter'
import * as menuApi from '../api/menu'
import * as ordersApi from '../api/orders'
import type { OrderDetail } from '../api/orders'
import { renderWithAuth } from '../test/renderWithAuth'
import { AdminDashboardPage } from './admin/AdminDashboardPage'
import { CounterPage } from './counter/CounterPage'
import { OrderDetailPage } from './orders/OrderDetailPage'

// Các sửa đổi sau buổi review 2026-10-31: đơn đã thanh toán, đơn tồn từ ngày trước.

vi.mock('../api/admin')
vi.mock('../api/counter')
vi.mock('../api/menu')
vi.mock('../api/orders', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/orders')>()
  return { ...actual, fetchOrder: vi.fn(), cancelMyOrder: vi.fn() }
})

const customer: CurrentUser = { id: 1, displayName: 'Khách Một', role: 'CUSTOMER' }
const manager: CurrentUser = { id: 9, displayName: 'Quản Lý', role: 'MANAGER' }

const detail: OrderDetail = {
  id: 41,
  pickupCode: 'K7M3QX',
  slotStart: '2031-03-05T12:00',
  slotEnd: '2031-03-05T12:15',
  status: 'CONFIRMED',
  paymentStatus: 'PAID',
  paymentMethod: 'CASH',
  totalAmount: 45000,
  confirmedAt: '2031-03-05T10:00:00+07:00',
  lines: [{ menuItemId: 1, name: 'Burger bò', quantity: 1, unitPrice: 45000, lineTotal: 45000 }],
  cancelReason: null,
}

describe('Đơn đã thanh toán (khách không tự hủy)', () => {
  beforeEach(() => {
    vi.mocked(ordersApi.fetchOrder).mockReset().mockResolvedValue(detail)
  })

  it('không có nút hủy, hướng dẫn liên hệ cửa hàng', async () => {
    renderWithAuth(<p />, {
      path: '/orders/41',
      auth: { user: customer },
      extraRoutes: <Route path="/orders/:id" element={<OrderDetailPage />} />,
    })

    expect(await screen.findByText(/Đơn đã thanh toán. Muốn hủy/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Hủy đơn' })).not.toBeInTheDocument()
  })

  it('đơn chưa thanh toán vẫn có nút hủy', async () => {
    vi.mocked(ordersApi.fetchOrder).mockResolvedValue({
      ...detail,
      paymentStatus: 'UNPAID',
      paymentMethod: null,
    })
    renderWithAuth(<p />, {
      path: '/orders/41',
      auth: { user: customer },
      extraRoutes: <Route path="/orders/:id" element={<OrderDetailPage />} />,
    })

    expect(await screen.findByRole('button', { name: 'Hủy đơn' })).toBeInTheDocument()
  })

  it('đơn chưa thanh toán ghi rõ hạn và điều kiện tự hủy (BR-23)', async () => {
    vi.mocked(ordersApi.fetchOrder).mockResolvedValue({
      ...detail,
      paymentStatus: 'UNPAID',
      paymentMethod: null,
      cancelDeadline: '2031-03-05T11:45',
    })
    renderWithAuth(<p />, {
      path: '/orders/41',
      auth: { user: customer },
      extraRoutes: <Route path="/orders/:id" element={<OrderDetailPage />} />,
    })

    expect(
      await screen.findByText('Bạn có thể tự hủy tới 11:45 nếu cửa hàng chưa bắt đầu làm món.'),
    ).toBeInTheDocument()
  })
})

function counterOrder(overrides: Partial<CounterOrder>): CounterOrder {
  return {
    id: 1,
    pickupCode: 'AAAAAA',
    customerName: 'Khách Một',
    slotStart: '2031-03-05T12:00',
    slotEnd: '2031-03-05T12:15',
    source: 'PRE_ORDER',
    status: 'CONFIRMED',
    paymentStatus: 'UNPAID',
    paymentMethod: null,
    totalAmount: 45000,
    lines: [{ name: 'Burger bò', quantity: 1 }],
    overdue: false,
    stale: false,
    ...overrides,
  }
}

describe('Màn quầy: đơn tồn từ ngày trước', () => {
  beforeEach(() => {
    vi.mocked(menuApi.fetchMenu).mockResolvedValue([])
  })

  it('tách riêng đơn tồn, có ngày, quản lý hủy được và đánh dấu không đến nhận được', async () => {
    vi.mocked(counterApi.fetchCounterOrders).mockResolvedValue([
      counterOrder({ id: 1, pickupCode: 'OLD111', slotStart: '2031-03-04T12:00', stale: true }),
      counterOrder({
        id: 2,
        pickupCode: 'OLD222',
        slotStart: '2031-03-04T12:15',
        status: 'READY',
        overdue: true,
        stale: true,
      }),
      counterOrder({ id: 3, pickupCode: 'NEW333' }),
    ])
    renderWithAuth(<CounterPage />, { auth: { user: manager } })

    const section = await screen.findByRole('region', { name: 'Đơn tồn từ ngày trước' })
    expect(within(section).getByText('Đơn tồn từ ngày trước (2)')).toBeInTheDocument()
    expect(within(section).getByText('OLD111')).toBeInTheDocument()
    expect(within(section).getAllByText(/04\/03\/2031/, { selector: '.k-time' })).toHaveLength(2)
    expect(within(section).queryByText('NEW333')).not.toBeInTheDocument()
    expect(within(section).getByRole('button', { name: 'Hủy đơn OLD111' })).toBeInTheDocument()
    expect(
      within(section).getByRole('button', { name: 'Đánh dấu không đến nhận đơn OLD222' }),
    ).toBeInTheDocument()
    expect(screen.getByText('NEW333')).toBeInTheDocument()
  })

  it('không có đơn tồn thì không hiện mục này', async () => {
    vi.mocked(counterApi.fetchCounterOrders).mockResolvedValue([counterOrder({ id: 3 })])
    renderWithAuth(<CounterPage />, { auth: { user: manager } })

    expect(await screen.findByText('AAAAAA')).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Đơn tồn từ ngày trước' })).not.toBeInTheDocument()
  })
})

describe('Màn theo dõi: cảnh báo đơn tồn', () => {
  it('có đơn tồn thì nhắc vào màn Quầy', async () => {
    const dashboard: Dashboard = {
      date: '2031-03-05',
      slots: [],
      activeOrders: [],
      stats: {
        preOrders: 0,
        walkIns: 0,
        pickedUp: 0,
        cancelled: 0,
        noShow: 0,
        lateNow: 0,
        heldPoints: 0,
        quotaPoints: 880,
        quotaUsagePercent: 0,
        staleOpen: 3,
      },
    }
    vi.mocked(adminApi.fetchDashboard).mockResolvedValue(dashboard)
    renderWithAuth(<AdminDashboardPage />, { auth: { user: manager } })

    expect(await screen.findByText(/Có 3 đơn tồn từ ngày trước/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Quầy' })).toHaveAttribute('href', '/counter')
  })
})
