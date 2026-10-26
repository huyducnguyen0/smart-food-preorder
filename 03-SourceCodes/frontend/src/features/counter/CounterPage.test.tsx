import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { CurrentUser } from '../../api/auth'
import { ApiError } from '../../api/client'
import * as counterApi from '../../api/counter'
import type { CounterOrder } from '../../api/counter'
import { renderWithAuth } from '../../test/renderWithAuth'
import { CounterPage } from './CounterPage'

vi.mock('../../api/counter')

const counter: CurrentUser = { id: 5, displayName: 'Quầy Một', role: 'COUNTER' }

function order(overrides: Partial<CounterOrder>): CounterOrder {
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
    totalAmount: 90000,
    lines: [{ name: 'Burger bò', quantity: 2 }],
    ...overrides,
  }
}

const orders: CounterOrder[] = [
  order({ id: 1, pickupCode: 'K7M3QX' }),
  order({
    id: 2,
    pickupCode: 'P4W9TD',
    status: 'READY',
    paymentStatus: 'PAID',
    paymentMethod: 'BANK_TRANSFER',
    totalAmount: 45000,
    customerName: null,
    source: 'WALK_IN',
  }),
]

describe('CounterPage — ghi nhận thanh toán (UC-08)', () => {
  beforeEach(() => {
    vi.mocked(counterApi.fetchCounterOrders).mockReset().mockResolvedValue(orders)
    vi.mocked(counterApi.recordPayment)
      .mockReset()
      .mockResolvedValue({ id: 1, paymentStatus: 'PAID', paymentMethod: 'CASH' })
  })

  it('hiện từng đơn với mã nhận, khung, tên khách, món, số tiền cần thu và trạng thái thanh toán', async () => {
    renderWithAuth(<CounterPage />, { auth: { user: counter } })

    const first = (await screen.findByText('K7M3QX')).closest('li') as HTMLElement
    expect(within(first).getByText('12:00 – 12:15')).toBeInTheDocument()
    expect(within(first).getByText(/Khách Một/)).toBeInTheDocument()
    expect(within(first).getByText('90.000đ')).toBeInTheDocument()
    expect(within(first).getByText('Đã xác nhận')).toBeInTheDocument()
    const second = screen.getByText('P4W9TD').closest('li') as HTMLElement
    expect(within(second).getByText(/Khách mua tại quầy/)).toBeInTheDocument()
    expect(within(second).getByText('Đã thanh toán bằng chuyển khoản')).toBeInTheDocument()
  })

  it('đơn chưa thanh toán có hai nút thu tiền; đơn đã thanh toán thì không', async () => {
    renderWithAuth(<CounterPage />, { auth: { user: counter } })
    await screen.findByText('K7M3QX')

    expect(screen.getByRole('button', { name: 'Thu tiền mặt đơn K7M3QX' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Thu chuyển khoản đơn K7M3QX' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /đơn P4W9TD/ })).not.toBeInTheDocument()
  })

  it('thu tiền mặt gọi API đúng phương thức rồi làm mới danh sách', async () => {
    renderWithAuth(<CounterPage />, { auth: { user: counter } })

    await userEvent.click(await screen.findByRole('button', { name: 'Thu tiền mặt đơn K7M3QX' }))

    expect(counterApi.recordPayment).toHaveBeenCalledWith(1, 'CASH')
    await vi.waitFor(() => expect(counterApi.fetchCounterOrders).toHaveBeenCalledTimes(2))
  })

  it('thu chuyển khoản gọi API với phương thức chuyển khoản', async () => {
    renderWithAuth(<CounterPage />, { auth: { user: counter } })

    await userEvent.click(
      await screen.findByRole('button', { name: 'Thu chuyển khoản đơn K7M3QX' }),
    )

    expect(counterApi.recordPayment).toHaveBeenCalledWith(1, 'BANK_TRANSFER')
  })

  it('đơn đã được người khác ghi nhận: báo và làm mới, không coi là lỗi hệ thống', async () => {
    vi.mocked(counterApi.recordPayment).mockRejectedValue(new ApiError(409, 'ALREADY_PAID', 'x'))
    renderWithAuth(<CounterPage />, { auth: { user: counter } })

    await userEvent.click(await screen.findByRole('button', { name: 'Thu tiền mặt đơn K7M3QX' }))

    expect(
      await screen.findByText(/Đơn K7M3QX: Đơn này đã được ghi nhận thanh toán/),
    ).toBeInTheDocument()
    await vi.waitFor(() => expect(counterApi.fetchCounterOrders).toHaveBeenCalledTimes(2))
  })

  it('lỗi khác khi ghi nhận: báo lỗi cho quầy thử lại', async () => {
    vi.mocked(counterApi.recordPayment).mockRejectedValue(new ApiError(0, null, 'x'))
    renderWithAuth(<CounterPage />, { auth: { user: counter } })

    await userEvent.click(await screen.findByRole('button', { name: 'Thu tiền mặt đơn K7M3QX' }))

    expect(await screen.findByText(/kết nối/)).toBeInTheDocument()
  })

  it('tìm theo mã nhận, không phân biệt chữ hoa thường', async () => {
    renderWithAuth(<CounterPage />, { auth: { user: counter } })
    await screen.findByText('K7M3QX')

    await userEvent.type(screen.getByRole('searchbox', { name: 'Tìm theo mã nhận' }), 'p4w9')

    expect(screen.queryByText('K7M3QX')).not.toBeInTheDocument()
    expect(screen.getByText('P4W9TD')).toBeInTheDocument()
    await userEvent.clear(screen.getByRole('searchbox'))
    await userEvent.type(screen.getByRole('searchbox'), 'ZZZ')
    expect(screen.getByText(/Không có đơn nào khớp mã "ZZZ"/)).toBeInTheDocument()
  })

  it('chưa có đơn thì báo trống', async () => {
    vi.mocked(counterApi.fetchCounterOrders).mockResolvedValue([])
    renderWithAuth(<CounterPage />, { auth: { user: counter } })

    expect(await screen.findByText('Hôm nay chưa có đơn cần xử lý.')).toBeInTheDocument()
  })

  it('không tải được: báo lỗi; làm mới lỗi thì giữ danh sách cũ kèm cảnh báo', async () => {
    vi.mocked(counterApi.fetchCounterOrders)
      .mockResolvedValueOnce(orders)
      .mockRejectedValueOnce(new ApiError(500, 'INTERNAL_ERROR', 'x'))
    renderWithAuth(<CounterPage />, { auth: { user: counter } })
    await screen.findByText('K7M3QX')

    await userEvent.click(screen.getByRole('button', { name: 'Tải lại' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('có thể đã cũ')
    expect(screen.getByText('K7M3QX')).toBeInTheDocument()
  })
})
