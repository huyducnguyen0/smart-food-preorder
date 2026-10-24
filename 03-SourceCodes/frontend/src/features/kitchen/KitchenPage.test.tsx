import { act, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { CurrentUser } from '../../api/auth'
import { ApiError } from '../../api/client'
import * as kitchenApi from '../../api/kitchen'
import type { KitchenOrder, KitchenQueue } from '../../api/kitchen'
import { renderWithAuth } from '../../test/renderWithAuth'
import { KitchenPage, REFRESH_MS } from './KitchenPage'

vi.mock('../../api/kitchen')

const kitchen: CurrentUser = { id: 4, displayName: 'Bếp Một', role: 'KITCHEN' }

function order(overrides: Partial<KitchenOrder>): KitchenOrder {
  return {
    id: 1,
    pickupCode: 'AAAAAA',
    slotStart: '2031-03-05T11:30',
    slotEnd: '2031-03-05T11:45',
    source: 'PRE_ORDER',
    confirmedAt: '2031-03-05T10:52:31+07:00',
    late: false,
    lines: [{ name: 'Burger bò', quantity: 2 }],
    ...overrides,
  }
}

const queue: KitchenQueue = {
  serverTime: '2031-03-05T11:20:05+07:00',
  preparing: [
    order({
      id: 1,
      pickupCode: 'P4W9TD',
      slotStart: '2031-03-05T11:15',
      late: true,
      confirmedAt: '2031-03-05T10:40:00+07:00',
      lines: [
        { name: 'Burger bò', quantity: 2 },
        { name: 'Khoai tây chiên', quantity: 1 },
      ],
    }),
  ],
  dueSoon: [order({ id: 2, pickupCode: 'D3F7KA' })],
  upcoming: [
    order({ id: 3, pickupCode: 'M2C8QA', slotStart: '2031-03-05T12:15', source: 'WALK_IN' }),
  ],
}

describe('KitchenPage (UC-12.1)', () => {
  beforeEach(() => {
    vi.mocked(kitchenApi.fetchKitchenQueue).mockReset().mockResolvedValue(queue)
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('chia ba nhóm, mỗi đơn có mã nhận, giờ khung, nguồn, món và giờ xác nhận (bước 2)', async () => {
    renderWithAuth(<KitchenPage />, { auth: { user: kitchen } })

    const working = await screen.findByRole('region', { name: 'Đang làm' })
    expect(within(working).getByText('P4W9TD')).toBeInTheDocument()
    expect(within(working).getByText('11:15')).toBeInTheDocument()
    expect(within(working).getByText(/Khoai tây chiên/)).toBeInTheDocument()
    expect(within(working).getByText('Xác nhận 10:40')).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Cần chuẩn bị' })).toHaveTextContent('D3F7KA')
    const upcoming = screen.getByRole('region', { name: 'Sắp tới' })
    expect(upcoming).toHaveTextContent('M2C8QA')
    expect(upcoming).toHaveTextContent('Walk-in')
    expect(screen.getByText(/Cập nhật lúc 11:20/)).toBeInTheDocument()
  })

  it('đơn trễ có nhãn TRỄ, đơn chưa trễ thì không (BR-26)', async () => {
    renderWithAuth(<KitchenPage />, { auth: { user: kitchen } })

    const working = await screen.findByRole('region', { name: 'Đang làm' })
    expect(within(working).getByText('TRỄ')).toBeInTheDocument()
    expect(
      within(screen.getByRole('region', { name: 'Cần chuẩn bị' })).queryByText('TRỄ'),
    ).toBeNull()
  })

  it('không hiện giá hay trạng thái thanh toán (UC-12 Ghi chú)', async () => {
    renderWithAuth(<KitchenPage />, { auth: { user: kitchen } })
    await screen.findByText('P4W9TD')

    expect(document.body).not.toHaveTextContent(/[0-9][0-9.]*đ|thanh toán/i)
  })

  it('hàng chờ trống thì báo chưa có đơn (A1)', async () => {
    vi.mocked(kitchenApi.fetchKitchenQueue).mockResolvedValue({
      ...queue,
      preparing: [],
      dueSoon: [],
      upcoming: [],
    })
    renderWithAuth(<KitchenPage />, { auth: { user: kitchen } })

    expect(await screen.findByText('Chưa có đơn cần làm.')).toBeInTheDocument()
  })

  it('bấm Tải lại thì lấy dữ liệu mới', async () => {
    vi.mocked(kitchenApi.fetchKitchenQueue)
      .mockResolvedValueOnce(queue)
      .mockResolvedValueOnce({ ...queue, dueSoon: [order({ id: 9, pickupCode: 'NEW999' })] })
    renderWithAuth(<KitchenPage />, { auth: { user: kitchen } })
    await screen.findByText('D3F7KA')

    await userEvent.click(screen.getByRole('button', { name: 'Tải lại' }))

    expect(await screen.findByText('NEW999')).toBeInTheDocument()
    expect(screen.queryByText('D3F7KA')).not.toBeInTheDocument()
  })

  it('tự tải lại sau mỗi 30 giây', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    renderWithAuth(<KitchenPage />, { auth: { user: kitchen } })
    await screen.findByText('D3F7KA')
    expect(kitchenApi.fetchKitchenQueue).toHaveBeenCalledTimes(1)

    await act(async () => {
      await vi.advanceTimersByTimeAsync(REFRESH_MS)
    })

    expect(kitchenApi.fetchKitchenQueue).toHaveBeenCalledTimes(2)
  })

  it('lỗi tải lần đầu: báo lỗi, không hiện danh sách (E3)', async () => {
    vi.mocked(kitchenApi.fetchKitchenQueue).mockRejectedValue(new ApiError(0, null, 'x'))
    renderWithAuth(<KitchenPage />, { auth: { user: kitchen } })

    expect(await screen.findByRole('alert')).toHaveTextContent('kết nối')
    expect(screen.queryByRole('region', { name: 'Đang làm' })).not.toBeInTheDocument()
  })

  it('lỗi khi làm mới: giữ danh sách cũ nhưng cảnh báo có thể đã cũ (E3)', async () => {
    vi.mocked(kitchenApi.fetchKitchenQueue)
      .mockResolvedValueOnce(queue)
      .mockRejectedValueOnce(new ApiError(500, 'INTERNAL_ERROR', 'x'))
    renderWithAuth(<KitchenPage />, { auth: { user: kitchen } })
    await screen.findByText('D3F7KA')

    await userEvent.click(screen.getByRole('button', { name: 'Tải lại' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('có thể đã cũ')
    expect(screen.getByText('D3F7KA')).toBeInTheDocument()
  })
})
