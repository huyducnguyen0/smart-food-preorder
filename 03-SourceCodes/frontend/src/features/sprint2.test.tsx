import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as adminApi from '../api/admin'
import type { AdminDay, AdminMenu } from '../api/admin'
import type { CurrentUser } from '../api/auth'
import { ApiError } from '../api/client'
import * as counterApi from '../api/counter'
import * as menuApi from '../api/menu'
import type { OrderDetail } from '../api/orders'
import { renderWithAuth } from '../test/renderWithAuth'
import { AdminMenuPage } from './admin/AdminMenuPage'
import { AdminSlotsPage } from './admin/AdminSlotsPage'
import { RegisterPage } from './auth/RegisterPage'
import { WalkInPanel } from './counter/WalkInPanel'

vi.mock('../api/admin')
vi.mock('../api/counter')
vi.mock('../api/menu')

const manager: CurrentUser = { id: 9, displayName: 'Quản Lý', role: 'MANAGER' }
const counter: CurrentUser = { id: 5, displayName: 'Quầy Một', role: 'COUNTER' }

// ---------- UC-01 Đăng ký ----------
describe('RegisterPage (UC-01)', () => {
  it('thiếu trường thì báo từng trường, không gọi backend', async () => {
    const signUp = vi.fn()
    renderWithAuth(<RegisterPage />, { path: '/register', auth: { signUp } })

    await userEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))

    expect(screen.getByText('Vui lòng nhập email hoặc số điện thoại')).toBeInTheDocument()
    expect(screen.getByText('Vui lòng nhập tên')).toBeInTheDocument()
    expect(screen.getByText('Mật khẩu tối thiểu 8 ký tự')).toBeInTheDocument()
    expect(signUp).not.toHaveBeenCalled()
  })

  it('đăng ký thành công thì vào thực đơn', async () => {
    const signUp = vi.fn().mockResolvedValue({ id: 20, displayName: 'Mới', role: 'CUSTOMER' })
    renderWithAuth(<RegisterPage />, {
      path: '/register',
      auth: { signUp },
      extraRoutes: <Route path="/" element={<p>TRANG THỰC ĐƠN</p>} />,
    })

    await userEvent.type(screen.getByLabelText('Email hoặc số điện thoại'), 'moi@example.com')
    await userEvent.type(screen.getByLabelText('Tên hiển thị'), 'Mới')
    await userEvent.type(screen.getByLabelText('Mật khẩu'), 'matkhau123')
    await userEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))

    expect(signUp).toHaveBeenCalledWith({
      loginId: 'moi@example.com',
      displayName: 'Mới',
      password: 'matkhau123',
    })
    expect(await screen.findByText('TRANG THỰC ĐƠN')).toBeInTheDocument()
  })

  it('định danh đã có tài khoản: báo lý do, ở lại trang', async () => {
    const signUp = vi.fn().mockRejectedValue(new ApiError(409, 'LOGIN_ID_TAKEN', 'x'))
    renderWithAuth(<RegisterPage />, { path: '/register', auth: { signUp } })

    await userEvent.type(screen.getByLabelText('Email hoặc số điện thoại'), 'khach1@example.com')
    await userEvent.type(screen.getByLabelText('Tên hiển thị'), 'A')
    await userEvent.type(screen.getByLabelText('Mật khẩu'), 'matkhau123')
    await userEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('đã có tài khoản')
  })

  it('lỗi theo trường từ backend hiện dưới đúng ô', async () => {
    const signUp = vi.fn().mockRejectedValue(
      new ApiError(400, 'VALIDATION_ERROR', 'x', {
        errors: [{ field: 'loginId', message: 'Email hoặc số điện thoại không đúng dạng' }],
      }),
    )
    renderWithAuth(<RegisterPage />, { path: '/register', auth: { signUp } })

    await userEvent.type(screen.getByLabelText('Email hoặc số điện thoại'), 'abc')
    await userEvent.type(screen.getByLabelText('Tên hiển thị'), 'A')
    await userEvent.type(screen.getByLabelText('Mật khẩu'), 'matkhau123')
    await userEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))

    expect(await screen.findByText('Email hoặc số điện thoại không đúng dạng')).toBeInTheDocument()
  })
})

// ---------- UC-13 Quản lý món ----------
const adminMenu: AdminMenu = {
  categories: [
    { id: 1, name: 'Burger' },
    { id: 3, name: 'Đồ uống' },
  ],
  items: [
    {
      id: 12,
      categoryId: 1,
      name: 'Burger bò',
      description: null,
      imageUrl: null,
      price: 45000,
      workloadPoints: 5,
      available: true,
      discontinued: false,
    },
    {
      id: 18,
      categoryId: 1,
      name: 'Combo cũ',
      description: null,
      imageUrl: null,
      price: 99000,
      workloadPoints: 10,
      available: true,
      discontinued: true,
    },
  ],
}

describe('AdminMenuPage (UC-13)', () => {
  beforeEach(() => {
    vi.mocked(adminApi.fetchAdminMenu).mockReset().mockResolvedValue(adminMenu)
    vi.mocked(adminApi.createMenuItem).mockReset().mockResolvedValue(adminMenu.items[0])
    vi.mocked(adminApi.updateMenuItem).mockReset().mockResolvedValue(adminMenu.items[0])
    vi.mocked(adminApi.setItemAvailable).mockReset().mockResolvedValue(adminMenu.items[0])
    vi.mocked(adminApi.setItemDiscontinued).mockReset().mockResolvedValue(adminMenu.items[0])
  })

  it('hiện mọi món theo nhóm, có điểm công việc và nhãn ngừng bán', async () => {
    renderWithAuth(<AdminMenuPage />, { auth: { user: manager } })

    const burger = await screen.findByRole('region', { name: 'Burger' })
    expect(within(burger).getByText('Burger bò')).toBeInTheDocument()
    expect(within(burger).getByText('5 điểm')).toBeInTheDocument()
    expect(within(burger).getByText('Ngừng bán', { selector: '.tag' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Bán lại Combo cũ' })).toBeInTheDocument()
  })

  it('thêm món gửi đúng dữ liệu rồi tải lại danh sách', async () => {
    renderWithAuth(<AdminMenuPage />, { auth: { user: manager } })
    await screen.findByText('Burger bò')

    await userEvent.selectOptions(screen.getByLabelText('Nhóm'), '1')
    await userEvent.type(screen.getByLabelText('Tên món'), 'Burger cá')
    await userEvent.type(screen.getByLabelText('Giá (đồng)'), '42000')
    await userEvent.type(screen.getByLabelText('Điểm công việc'), '4')
    await userEvent.click(screen.getByRole('button', { name: 'Thêm món' }))

    expect(adminApi.createMenuItem).toHaveBeenCalledWith({
      categoryId: 1,
      name: 'Burger cá',
      description: '',
      imageUrl: '',
      price: 42000,
      workloadPoints: 4,
    })
    expect(await screen.findByText('Đã thêm món "Burger cá".')).toBeInTheDocument()
    expect(adminApi.fetchAdminMenu).toHaveBeenCalledTimes(2)
  })

  it('giá hoặc điểm sai thì báo, không gửi', async () => {
    renderWithAuth(<AdminMenuPage />, { auth: { user: manager } })
    await screen.findByText('Burger bò')

    await userEvent.selectOptions(screen.getByLabelText('Nhóm'), '1')
    await userEvent.type(screen.getByLabelText('Tên món'), 'A')
    await userEvent.type(screen.getByLabelText('Giá (đồng)'), '-5')
    await userEvent.type(screen.getByLabelText('Điểm công việc'), '1')
    await userEvent.click(screen.getByRole('button', { name: 'Thêm món' }))

    expect(screen.getByText('Giá phải là số nguyên không âm.')).toBeInTheDocument()
    expect(adminApi.createMenuItem).not.toHaveBeenCalled()
  })

  it('sửa món: điền sẵn dữ liệu, lưu gọi API sửa', async () => {
    renderWithAuth(<AdminMenuPage />, { auth: { user: manager } })

    await userEvent.click(await screen.findByRole('button', { name: 'Sửa Burger bò' }))
    expect(screen.getByLabelText('Tên món')).toHaveValue('Burger bò')
    await userEvent.clear(screen.getByLabelText('Giá (đồng)'))
    await userEvent.type(screen.getByLabelText('Giá (đồng)'), '50000')
    await userEvent.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(adminApi.updateMenuItem).toHaveBeenCalledWith(
      12,
      expect.objectContaining({ price: 50000 }),
    )
  })

  it('báo tạm hết và ngừng bán gọi đúng API', async () => {
    renderWithAuth(<AdminMenuPage />, { auth: { user: manager } })

    await userEvent.click(await screen.findByRole('button', { name: 'Báo tạm hết Burger bò' }))
    expect(adminApi.setItemAvailable).toHaveBeenCalledWith(12, false)
    await userEvent.click(await screen.findByRole('button', { name: 'Ngừng bán Burger bò' }))
    expect(adminApi.setItemDiscontinued).toHaveBeenCalledWith(12, true)
  })
})

// ---------- UC-14 Cấu hình ----------
const day: AdminDay = {
  date: '2031-03-05',
  openTime: '10:00',
  closeTime: '21:00',
  defaultQuotaPoints: 20,
  slotMinutes: 15,
  holdMinutes: 45,
  preOrderSharePercent: 70,
  pickupLimitOrders: 8,
  slots: [
    {
      start: '2031-03-05T12:00',
      end: '2031-03-05T12:15',
      quota: 20,
      quotaOverride: null,
      heldPoints: 10,
      openOrders: 1,
      plannedPoints: 10,
      pickupOrders: 1,
    },
    {
      start: '2031-03-05T12:15',
      end: '2031-03-05T12:30',
      quota: 8,
      quotaOverride: 8,
      heldPoints: 0,
      openOrders: 0,
      plannedPoints: 0,
      pickupOrders: 0,
    },
  ],
}

describe('AdminSlotsPage (UC-14)', () => {
  beforeEach(() => {
    vi.mocked(adminApi.fetchAdminDay).mockReset().mockResolvedValue(day)
    vi.mocked(adminApi.updateSettings).mockReset().mockResolvedValue(day)
    vi.mocked(adminApi.setSlotQuota).mockReset().mockResolvedValue(day)
  })

  it('hiện cấu hình và từng khung với điểm bếp làm, công suất, số đơn nhận (PB-22)', async () => {
    renderWithAuth(<AdminSlotsPage />, { auth: { user: manager } })

    expect(await screen.findByText('Bếp làm 10/20 · nhận 1 đơn')).toBeInTheDocument()
    expect(screen.getByText('Bếp làm 0/8 (riêng) · nhận 0 đơn')).toBeInTheDocument()
    expect(screen.getByLabelText('Mở cửa')).toHaveValue('10:00')
    expect(screen.getByLabelText('Công suất bếp (điểm/15 phút)')).toHaveValue('20')
  })

  it('lưu cấu hình gửi giờ, công suất và thông số lịch bếp', async () => {
    renderWithAuth(<AdminSlotsPage />, { auth: { user: manager } })
    await screen.findByText('Bếp làm 10/20 · nhận 1 đơn')

    await userEvent.clear(screen.getByLabelText('Công suất bếp (điểm/15 phút)'))
    await userEvent.type(screen.getByLabelText('Công suất bếp (điểm/15 phút)'), '25')
    await userEvent.clear(screen.getByLabelText('Phần cho đặt trước (%)'))
    await userEvent.type(screen.getByLabelText('Phần cho đặt trước (%)'), '60')
    await userEvent.click(screen.getByRole('button', { name: 'Lưu cấu hình' }))

    expect(adminApi.updateSettings).toHaveBeenCalledWith({
      openTime: '10:00',
      closeTime: '21:00',
      defaultQuotaPoints: 25,
      holdMinutes: 45,
      preOrderSharePercent: 60,
      pickupLimitOrders: 8,
    })
  })

  it('giảm công suất làm bếp không kịp đơn đã nhận: hiện câu lỗi cụ thể của backend', async () => {
    vi.mocked(adminApi.setSlotQuota).mockRejectedValue(
      new ApiError(
        409,
        'QUOTA_BELOW_HELD',
        'Khung 12:00 đang giữ 10 điểm, không đặt quota thấp hơn được',
      ),
    )
    renderWithAuth(<AdminSlotsPage />, { auth: { user: manager } })
    await screen.findByText('Bếp làm 10/20 · nhận 1 đơn')

    await userEvent.clear(screen.getByLabelText('Công suất khung 12:00'))
    await userEvent.type(screen.getByLabelText('Công suất khung 12:00'), '5')
    await userEvent.click(screen.getByRole('button', { name: 'Lưu công suất khung 12:00' }))

    expect(adminApi.setSlotQuota).toHaveBeenCalledWith('12:00', 5)
    expect(await screen.findByText(/đang giữ 10 điểm/)).toBeInTheDocument()
  })

  it('khung có quota riêng thì trả về mặc định được', async () => {
    renderWithAuth(<AdminSlotsPage />, { auth: { user: manager } })

    await userEvent.click(await screen.findByRole('button', { name: 'Về mặc định khung 12:15' }))

    expect(adminApi.setSlotQuota).toHaveBeenCalledWith('12:15', null)
  })
})

// ---------- UC-07 Walk-in ----------
const placed: OrderDetail = {
  id: 77,
  pickupCode: 'W4LK22',
  slotStart: '2031-03-05T11:00',
  slotEnd: '2031-03-05T11:15',
  status: 'CONFIRMED',
  paymentStatus: 'PAID',
  paymentMethod: 'CASH',
  totalAmount: 90000,
  confirmedAt: '2031-03-05T11:02:00+07:00',
  lines: [{ menuItemId: 12, name: 'Burger bò', quantity: 2, unitPrice: 45000, lineTotal: 90000 }],
}

describe('WalkInPanel (UC-07)', () => {
  beforeEach(() => {
    vi.mocked(menuApi.fetchMenu)
      .mockReset()
      .mockResolvedValue([
        {
          id: 1,
          name: 'Burger',
          items: [
            {
              id: 12,
              name: 'Burger bò',
              description: null,
              imageUrl: null,
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
      ])
    vi.mocked(counterApi.estimateWalkIn)
      .mockReset()
      .mockResolvedValue({
        slot: { start: '2031-03-05T11:00', end: '2031-03-05T11:15' },
        totalAmount: 90000,
      })
    vi.mocked(counterApi.confirmWalkIn).mockReset().mockResolvedValue(placed)
  })

  async function pickTwoBurgers() {
    await userEvent.click(await screen.findByRole('button', { name: 'Thêm Burger bò' }))
    await userEvent.click(screen.getByRole('button', { name: 'Thêm Burger bò' }))
  }

  it('chỉ hiện món còn bán; chưa chọn món thì chưa xem khung được', async () => {
    renderWithAuth(<WalkInPanel onCreated={() => undefined} />, { auth: { user: counter } })

    expect(await screen.findByText('Burger bò')).toBeInTheDocument()
    expect(screen.queryByText('Burger gà giòn')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Xem khung dự kiến' })).toBeDisabled()
  })

  it('xem khung dự kiến rồi xác nhận khi khách đồng ý: hiện mã nhận', async () => {
    const onCreated = vi.fn()
    renderWithAuth(<WalkInPanel onCreated={onCreated} />, { auth: { user: counter } })
    await pickTwoBurgers()

    await userEvent.click(screen.getByRole('button', { name: 'Xem khung dự kiến' }))
    expect(await screen.findByText('11:00 – 11:15')).toBeInTheDocument()
    expect(counterApi.estimateWalkIn).toHaveBeenCalledWith([
      { menuItemId: 12, quantity: 2, unitPrice: 45000 },
    ])

    await userEvent.click(screen.getByRole('button', { name: 'Đã thu tiền mặt' }))

    const request = vi.mocked(counterApi.confirmWalkIn).mock.calls[0][0]
    expect(request).toMatchObject({ slotStart: '2031-03-05T11:00', paymentMethod: 'CASH' })
    expect(await screen.findByText('W4LK22')).toBeInTheDocument()
    expect(onCreated).toHaveBeenCalled()
  })

  it('khung vừa hết chỗ: hiện khung mới để báo lại khách, giữ cùng mã yêu cầu khi xác nhận lại', async () => {
    vi.mocked(counterApi.confirmWalkIn)
      .mockRejectedValueOnce(
        new ApiError(409, 'SLOT_FULL', 'x', {
          nextSlot: { start: '2031-03-05T11:15', end: '2031-03-05T11:30' },
        }),
      )
      .mockResolvedValueOnce(placed)
    renderWithAuth(<WalkInPanel onCreated={() => undefined} />, { auth: { user: counter } })
    await pickTwoBurgers()
    await userEvent.click(screen.getByRole('button', { name: 'Xem khung dự kiến' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Đã thu tiền mặt' }))

    expect(await screen.findByText(/Khung mới: 11:15 – 11:30/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Đã thu tiền mặt' }))

    const [first, second] = vi.mocked(counterApi.confirmWalkIn).mock.calls
    expect(second[0].slotStart).toBe('2031-03-05T11:15')
    expect(second[0].requestId).toBe(first[0].requestId)
  })

  it('không còn khung nào: báo lý do', async () => {
    vi.mocked(counterApi.estimateWalkIn).mockRejectedValue(
      new ApiError(409, 'NO_SLOT_AVAILABLE', 'x'),
    )
    renderWithAuth(<WalkInPanel onCreated={() => undefined} />, { auth: { user: counter } })
    await pickTwoBurgers()

    await userEvent.click(screen.getByRole('button', { name: 'Xem khung dự kiến' }))

    expect(await screen.findByText(/không còn khung nào đủ chỗ/)).toBeInTheDocument()
  })
})
