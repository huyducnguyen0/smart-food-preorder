import { act, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as adminApi from '../api/admin'
import type { Dashboard, StaffAccount } from '../api/admin'
import type { CurrentUser } from '../api/auth'
import { ApiError } from '../api/client'
import * as counterApi from '../api/counter'
import type { CounterOrder } from '../api/counter'
import * as menuApi from '../api/menu'
import * as ordersApi from '../api/orders'
import type { OrderDetail } from '../api/orders'
import { renderWithAuth } from '../test/renderWithAuth'
import { AdminDashboardPage } from './admin/AdminDashboardPage'
import { AdminStaffPage } from './admin/AdminStaffPage'
import { CounterPage } from './counter/CounterPage'
import { ORDER_POLL_MS, OrderDetailPage } from './orders/OrderDetailPage'

vi.mock('../api/admin')
vi.mock('../api/counter')
vi.mock('../api/menu')
vi.mock('../api/orders', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/orders')>()
  return { ...actual, fetchOrder: vi.fn(), cancelMyOrder: vi.fn() }
})

const customer: CurrentUser = { id: 1, displayName: 'Khách Một', role: 'CUSTOMER' }
const counter: CurrentUser = { id: 5, displayName: 'Quầy Một', role: 'COUNTER' }
const manager: CurrentUser = { id: 9, displayName: 'Quản Lý', role: 'MANAGER' }

const detail: OrderDetail = {
  id: 41,
  pickupCode: 'K7M3QX',
  slotStart: '2031-03-05T12:00',
  slotEnd: '2031-03-05T12:15',
  status: 'CONFIRMED',
  paymentStatus: 'UNPAID',
  paymentMethod: null,
  totalAmount: 45000,
  confirmedAt: '2031-03-05T10:00:00+07:00',
  lines: [{ menuItemId: 1, name: 'Burger bò', quantity: 1, unitPrice: 45000, lineTotal: 45000 }],
  cancelReason: null,
}

function renderDetail() {
  return renderWithAuth(<p />, {
    path: '/orders/41',
    auth: { user: customer },
    extraRoutes: <Route path="/orders/:id" element={<OrderDetailPage />} />,
  })
}

// ---------- UC-06 khách hủy đơn ----------
describe('OrderDetailPage — khách hủy đơn (UC-06)', () => {
  beforeEach(() => {
    vi.mocked(ordersApi.fetchOrder).mockReset()
    vi.mocked(ordersApi.cancelMyOrder).mockReset()
  })

  it('đơn đã xác nhận: hỏi lại rồi hủy, trang hiện trạng thái đã hủy', async () => {
    vi.mocked(ordersApi.fetchOrder)
      .mockResolvedValueOnce(detail)
      .mockResolvedValueOnce({ ...detail, status: 'CANCELLED' })
    vi.mocked(ordersApi.cancelMyOrder).mockResolvedValue({ id: 41, status: 'CANCELLED' })
    renderDetail()

    await userEvent.click(await screen.findByRole('button', { name: 'Hủy đơn' }))
    expect(ordersApi.cancelMyOrder).not.toHaveBeenCalled()
    await userEvent.click(screen.getByRole('button', { name: 'Xác nhận hủy' }))

    expect(ordersApi.cancelMyOrder).toHaveBeenCalledWith(41)
    expect(await screen.findByText('Đã hủy')).toBeInTheDocument()
    expect(screen.getByText('Đơn đã được hủy.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Hủy đơn' })).not.toBeInTheDocument()
  })

  it('đã vào cửa sổ chuẩn bị: báo mốc và hướng dẫn liên hệ cửa hàng', async () => {
    vi.mocked(ordersApi.fetchOrder).mockResolvedValue(detail)
    vi.mocked(ordersApi.cancelMyOrder).mockRejectedValue(
      new ApiError(409, 'CANCEL_NOT_ALLOWED', 'x', { cancelDeadline: '2031-03-05T11:45' }),
    )
    renderDetail()

    await userEvent.click(await screen.findByRole('button', { name: 'Hủy đơn' }))
    await userEvent.click(screen.getByRole('button', { name: 'Xác nhận hủy' }))

    expect(await screen.findByText(/từ 11:45.*liên hệ cửa hàng/)).toBeInTheDocument()
  })

  it('đơn đang chuẩn bị không có nút hủy; đơn cửa hàng hủy hiện lý do', async () => {
    vi.mocked(ordersApi.fetchOrder).mockResolvedValue({
      ...detail,
      status: 'CANCELLED',
      cancelReason: 'Hết nguyên liệu',
    })
    renderDetail()

    expect(await screen.findByText(/Lý do: Hết nguyên liệu/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Hủy đơn' })).not.toBeInTheDocument()
  })
})

// ---------- PB-14 tự cập nhật ----------
describe('OrderDetailPage — tự cập nhật trạng thái (PB-14)', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.mocked(ordersApi.fetchOrder).mockReset()
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('đơn chuyển sang sẵn sàng thì tự hiện thông báo nổi bật, không cần bấm tải lại', async () => {
    vi.mocked(ordersApi.fetchOrder)
      .mockResolvedValueOnce({ ...detail, status: 'PREPARING' })
      .mockResolvedValue({ ...detail, status: 'READY' })
    renderDetail()
    expect(await screen.findByText('Đang chuẩn bị')).toBeInTheDocument()
    expect(screen.queryByText(/đã sẵn sàng/)).not.toBeInTheDocument()

    await act(async () => {
      await vi.advanceTimersByTimeAsync(ORDER_POLL_MS)
    })

    expect(await screen.findByText(/Đơn của bạn đã sẵn sàng/)).toBeInTheDocument()
    expect(ordersApi.fetchOrder).toHaveBeenCalledTimes(2)
  })

  it('đơn đã kết thúc thì không hỏi lại backend nữa', async () => {
    vi.mocked(ordersApi.fetchOrder).mockResolvedValue({ ...detail, status: 'PICKED_UP' })
    renderDetail()
    expect(await screen.findByText('Đã nhận')).toBeInTheDocument()

    await act(async () => {
      await vi.advanceTimersByTimeAsync(ORDER_POLL_MS * 3)
    })

    expect(ordersApi.fetchOrder).toHaveBeenCalledTimes(1)
  })
})

// ---------- UC-11, UC-10 tại quầy ----------
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

describe('CounterPage — hủy đơn và không đến nhận (UC-10, UC-11)', () => {
  beforeEach(() => {
    vi.mocked(menuApi.fetchMenu).mockResolvedValue([])
    vi.mocked(counterApi.fetchCounterOrders).mockReset()
    vi.mocked(counterApi.cancelStoreOrder).mockReset()
    vi.mocked(counterApi.markNoShow).mockReset()
  })

  it('nhân viên quầy chỉ thấy nút hủy ở đơn walk-in chưa làm; lý do bắt buộc', async () => {
    vi.mocked(counterApi.fetchCounterOrders).mockResolvedValue([
      counterOrder({ id: 1, pickupCode: 'PRE111' }),
      counterOrder({ id: 2, pickupCode: 'WLK222', source: 'WALK_IN', customerName: null }),
    ])
    vi.mocked(counterApi.cancelStoreOrder).mockResolvedValue({ id: 2, status: 'CANCELLED' })
    renderWithAuth(<CounterPage />, { auth: { user: counter } })

    expect(await screen.findByText('PRE111')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Hủy đơn PRE111' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Hủy đơn WLK222' }))
    const confirm = screen.getByRole('button', { name: 'Xác nhận hủy đơn WLK222' })
    expect(confirm).toBeDisabled()

    await userEvent.type(screen.getByLabelText('Lý do hủy đơn WLK222'), '  Khách đổi ý ')
    await userEvent.click(confirm)

    expect(counterApi.cancelStoreOrder).toHaveBeenCalledWith(2, 'Khách đổi ý')
    expect(await screen.findByText('Đã hủy đơn WLK222.')).toBeInTheDocument()
  })

  it('quản lý hủy được đơn pre-order đang chuẩn bị', async () => {
    vi.mocked(counterApi.fetchCounterOrders).mockResolvedValue([
      counterOrder({ id: 3, pickupCode: 'PRE333', status: 'PREPARING' }),
    ])
    renderWithAuth(<CounterPage />, { auth: { user: manager } })

    expect(await screen.findByRole('button', { name: 'Hủy đơn PRE333' })).toBeInTheDocument()
  })

  it('đơn sẵn sàng quá hạn được gắn cờ và đánh dấu không đến nhận', async () => {
    vi.mocked(counterApi.fetchCounterOrders).mockResolvedValue([
      counterOrder({ id: 4, pickupCode: 'RDY444', status: 'READY', overdue: true }),
      counterOrder({ id: 5, pickupCode: 'RDY555', status: 'READY' }),
    ])
    vi.mocked(counterApi.markNoShow).mockResolvedValue({ id: 4, status: 'NO_SHOW' })
    renderWithAuth(<CounterPage />, { auth: { user: counter } })

    await userEvent.click(
      await screen.findByRole('button', { name: 'Đánh dấu không đến nhận đơn RDY444' }),
    )

    expect(counterApi.markNoShow).toHaveBeenCalledWith(4)
    expect(
      screen.queryByRole('button', { name: 'Đánh dấu không đến nhận đơn RDY555' }),
    ).not.toBeInTheDocument()
    expect(await screen.findByText(/RDY444 không đến nhận/)).toBeInTheDocument()
  })

  it('chưa quá hạn theo backend: báo lý do', async () => {
    vi.mocked(counterApi.fetchCounterOrders).mockResolvedValue([
      counterOrder({ id: 4, pickupCode: 'RDY444', status: 'READY', overdue: true }),
    ])
    vi.mocked(counterApi.markNoShow).mockRejectedValue(new ApiError(409, 'NO_SHOW_TOO_EARLY', 'x'))
    renderWithAuth(<CounterPage />, { auth: { user: counter } })

    await userEvent.click(
      await screen.findByRole('button', { name: 'Đánh dấu không đến nhận đơn RDY444' }),
    )

    expect(await screen.findByText(/Chưa quá hạn chờ khách/)).toBeInTheDocument()
  })
})

// ---------- UC-15 tài khoản nhân viên ----------
const staff: StaffAccount[] = [
  { id: 3, loginId: 'quay1@example.com', displayName: 'Quầy Một', role: 'COUNTER', locked: false },
  { id: 4, loginId: 'bep1@example.com', displayName: 'Bếp Một', role: 'KITCHEN', locked: true },
]

describe('AdminStaffPage (UC-15)', () => {
  beforeEach(() => {
    vi.mocked(adminApi.fetchStaff).mockReset().mockResolvedValue(staff)
    vi.mocked(adminApi.createStaff).mockReset()
    vi.mocked(adminApi.setStaffLocked).mockReset()
  })

  it('liệt kê nhân viên với vai trò và trạng thái khóa', async () => {
    renderWithAuth(<AdminStaffPage />, { auth: { user: manager } })

    const list = await screen.findByRole('region', { name: 'Danh sách nhân viên' })
    expect(within(list).getByText(/quay1@example.com · Quầy/)).toBeInTheDocument()
    expect(within(list).getByText(/bep1@example.com · Bếp · Đã khóa/)).toBeInTheDocument()
  })

  it('tạo tài khoản bếp', async () => {
    vi.mocked(adminApi.createStaff).mockResolvedValue({
      id: 9,
      loginId: 'bep2@example.com',
      displayName: 'Bếp Hai',
      role: 'KITCHEN',
      locked: false,
    })
    renderWithAuth(<AdminStaffPage />, { auth: { user: manager } })
    await screen.findByText(/quay1@example.com/)

    await userEvent.type(screen.getByLabelText('Email hoặc số điện thoại'), 'bep2@example.com')
    await userEvent.type(screen.getByLabelText('Tên hiển thị'), 'Bếp Hai')
    await userEvent.type(screen.getByLabelText('Mật khẩu ban đầu'), 'matkhau123')
    await userEvent.selectOptions(screen.getByLabelText('Vai trò'), 'KITCHEN')
    await userEvent.click(screen.getByRole('button', { name: 'Tạo tài khoản' }))

    expect(adminApi.createStaff).toHaveBeenCalledWith({
      loginId: 'bep2@example.com',
      displayName: 'Bếp Hai',
      password: 'matkhau123',
      role: 'KITCHEN',
    })
    expect(await screen.findByText('Đã tạo tài khoản bep2@example.com.')).toBeInTheDocument()
  })

  it('thiếu dữ liệu thì không gọi backend', async () => {
    renderWithAuth(<AdminStaffPage />, { auth: { user: manager } })
    await screen.findByText(/quay1@example.com/)

    await userEvent.click(screen.getByRole('button', { name: 'Tạo tài khoản' }))

    expect(adminApi.createStaff).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent('tối thiểu 8 ký tự')
  })

  it('khóa và mở khóa', async () => {
    vi.mocked(adminApi.setStaffLocked).mockResolvedValue([{ ...staff[0], locked: true }, staff[1]])
    renderWithAuth(<AdminStaffPage />, { auth: { user: manager } })

    await userEvent.click(await screen.findByRole('button', { name: 'Khóa quay1@example.com' }))

    expect(adminApi.setStaffLocked).toHaveBeenCalledWith(3, true)
    expect(
      await screen.findByRole('button', { name: 'Mở khóa quay1@example.com' }),
    ).toBeInTheDocument()
  })
})

// ---------- PB-17 theo dõi ----------
const dashboard: Dashboard = {
  date: '2031-03-05',
  slots: [
    {
      start: '2031-03-05T12:00',
      end: '2031-03-05T12:15',
      quota: 20,
      quotaOverride: null,
      heldPoints: 12,
      openOrders: 2,
      plannedPoints: 12,
      pickupOrders: 2,
    },
    {
      start: '2031-03-05T12:15',
      end: '2031-03-05T12:30',
      quota: 20,
      quotaOverride: null,
      heldPoints: 0,
      openOrders: 0,
      plannedPoints: 0,
      pickupOrders: 0,
    },
  ],
  activeOrders: [
    {
      id: 1,
      pickupCode: 'LATE01',
      slotStart: '2031-03-05T12:00',
      status: 'PREPARING',
      source: 'PRE_ORDER',
      late: true,
    },
    {
      id: 2,
      pickupCode: 'OKAY02',
      slotStart: '2031-03-05T12:00',
      status: 'READY',
      source: 'WALK_IN',
      late: false,
    },
  ],
  stats: {
    preOrders: 3,
    walkIns: 1,
    pickedUp: 1,
    cancelled: 1,
    noShow: 0,
    lateNow: 1,
    heldPoints: 12,
    quotaPoints: 880,
    quotaUsagePercent: 1,
    staleOpen: 0,
  },
}

describe('AdminDashboardPage (PB-17)', () => {
  it('hiện thống kê, đơn trễ và các khung đang có đơn', async () => {
    vi.mocked(adminApi.fetchDashboard).mockResolvedValue(dashboard)
    renderWithAuth(<AdminDashboardPage />, { auth: { user: manager } })

    const stats = await screen.findByRole('region', { name: 'Thống kê hôm nay' })
    expect(within(stats).getByText('Đơn đặt trước').nextSibling).toHaveTextContent('3')
    expect(within(stats).getByText('Đang trễ').nextSibling).toHaveTextContent('1')
    expect(within(stats).getByText('1% (12/880)')).toBeInTheDocument()

    const active = screen.getByRole('list', { name: 'Đơn đang xử lý' })
    const late = within(active).getByText('LATE01').closest('li') as HTMLElement
    expect(within(late).getByText('Trễ')).toBeInTheDocument()
    const ok = within(active).getByText('OKAY02').closest('li') as HTMLElement
    expect(within(ok).queryByText('Trễ')).not.toBeInTheDocument()

    const slots = screen.getByRole('list', { name: 'Tải theo khung' })
    expect(within(slots).getAllByRole('listitem')).toHaveLength(1)
    expect(within(slots).getByText('Bếp làm 12/20 · nhận 2 đơn')).toBeInTheDocument()
    expect(screen.getByText(/không phải năng lực bếp/)).toBeInTheDocument()
  })

  it('không tải được thì báo lỗi', async () => {
    vi.mocked(adminApi.fetchDashboard).mockRejectedValue(new ApiError(0, null, 'x'))
    renderWithAuth(<AdminDashboardPage />, { auth: { user: manager } })

    expect(await screen.findByRole('alert')).toHaveTextContent('kết nối')
  })
})
