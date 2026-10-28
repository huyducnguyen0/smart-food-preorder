import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { CurrentUser } from '../../api/auth'
import { ApiError } from '../../api/client'
import * as counterApi from '../../api/counter'
import * as menuApi from '../../api/menu'
import type { CounterOrder } from '../../api/counter'
import { renderWithAuth } from '../../test/renderWithAuth'
import { CounterPage } from './CounterPage'

vi.mock('../../api/counter')
// Khung tạo đơn walk-in tải menu; các test ở đây không dùng tới nên trả menu rỗng.
vi.mock('../../api/menu')
beforeEach(() => {
  vi.mocked(menuApi.fetchMenu).mockResolvedValue([])
})

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
    overdue: false,
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
    expect(screen.queryByRole('button', { name: /Thu .* đơn P4W9TD/ })).not.toBeInTheDocument()
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

describe('CounterPage — bàn giao (UC-09)', () => {
  const readyPaid = order({
    id: 3,
    pickupCode: 'R3ADY2',
    status: 'READY',
    paymentStatus: 'PAID',
    paymentMethod: 'CASH',
  })
  const readyUnpaid = order({ id: 4, pickupCode: 'UNP4ID', status: 'READY' })

  beforeEach(() => {
    vi.mocked(counterApi.fetchCounterOrders)
      .mockReset()
      .mockResolvedValue([
        readyPaid,
        readyUnpaid,
        order({ id: 5, pickupCode: 'C0NF1R', paymentStatus: 'PAID', paymentMethod: 'CASH' }),
      ])
    vi.mocked(counterApi.handOverOrder)
      .mockReset()
      .mockResolvedValue({ id: 3, status: 'PICKED_UP' })
  })

  it('chỉ đơn sẵn sàng và đã thanh toán mới có ô nhập mã và nút Bàn giao', async () => {
    renderWithAuth(<CounterPage />, { auth: { user: counter } })
    await screen.findByText('R3ADY2')

    expect(screen.getByRole('button', { name: 'Bàn giao đơn R3ADY2' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Bàn giao đơn UNP4ID' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Bàn giao đơn C0NF1R' })).not.toBeInTheDocument()
    expect(screen.getByText(/Thu tiền trước khi bàn giao/)).toBeInTheDocument()
  })

  it('nút Bàn giao chỉ bấm được khi đã nhập mã', async () => {
    renderWithAuth(<CounterPage />, { auth: { user: counter } })
    const button = await screen.findByRole('button', { name: 'Bàn giao đơn R3ADY2' })
    expect(button).toBeDisabled()

    await userEvent.type(screen.getByLabelText('Mã nhận khách đọc cho đơn R3ADY2'), 'r3ady2')

    expect(button).toBeEnabled()
  })

  it('bàn giao gửi mã đã nhập rồi làm mới danh sách', async () => {
    renderWithAuth(<CounterPage />, { auth: { user: counter } })
    await screen.findByText('R3ADY2')

    await userEvent.type(screen.getByLabelText('Mã nhận khách đọc cho đơn R3ADY2'), 'R3ADY2')
    await userEvent.click(screen.getByRole('button', { name: 'Bàn giao đơn R3ADY2' }))

    expect(counterApi.handOverOrder).toHaveBeenCalledWith(3, 'R3ADY2')
    await vi.waitFor(() => expect(counterApi.fetchCounterOrders).toHaveBeenCalledTimes(2))
  })

  it('sai mã: báo lý do cho quầy, đơn không đổi', async () => {
    vi.mocked(counterApi.handOverOrder).mockRejectedValue(
      new ApiError(409, 'PICKUP_CODE_MISMATCH', 'x'),
    )
    renderWithAuth(<CounterPage />, { auth: { user: counter } })
    await screen.findByText('R3ADY2')

    await userEvent.type(screen.getByLabelText('Mã nhận khách đọc cho đơn R3ADY2'), 'ZZZZZZ')
    await userEvent.click(screen.getByRole('button', { name: 'Bàn giao đơn R3ADY2' }))

    expect(await screen.findByText(/Đơn R3ADY2: Mã nhận không khớp/)).toBeInTheDocument()
  })

  it('đơn đã bàn giao ở nơi khác: báo trạng thái đã đổi và làm mới', async () => {
    vi.mocked(counterApi.handOverOrder).mockRejectedValue(
      new ApiError(409, 'INVALID_STATUS_TRANSITION', 'x'),
    )
    renderWithAuth(<CounterPage />, { auth: { user: counter } })
    await screen.findByText('R3ADY2')

    await userEvent.type(screen.getByLabelText('Mã nhận khách đọc cho đơn R3ADY2'), 'R3ADY2')
    await userEvent.click(screen.getByRole('button', { name: 'Bàn giao đơn R3ADY2' }))

    expect(await screen.findByText(/Đơn R3ADY2: Đơn đã thay đổi trạng thái/)).toBeInTheDocument()
    await vi.waitFor(() => expect(counterApi.fetchCounterOrders).toHaveBeenCalledTimes(2))
  })
})
