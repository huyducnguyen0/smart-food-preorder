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
    prepareFrom: '2031-03-05T11:15',
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
    order({
      id: 3,
      pickupCode: 'M2C8QA',
      slotStart: '2031-03-05T12:15',
      prepareFrom: '2031-03-05T12:00',
      source: 'WALK_IN',
    }),
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

  describe('thao tác của bếp (UC-12.2)', () => {
    beforeEach(() => {
      vi.mocked(kitchenApi.startOrder).mockReset().mockResolvedValue({ id: 2, status: 'PREPARING' })
      vi.mocked(kitchenApi.completeOrder).mockReset().mockResolvedValue({ id: 1, status: 'READY' })
    })

    it('Bắt đầu đơn cần chuẩn bị rồi làm mới hàng chờ (bước 4–5)', async () => {
      renderWithAuth(<KitchenPage />, { auth: { user: kitchen } })

      await userEvent.click(await screen.findByRole('button', { name: 'Bắt đầu đơn D3F7KA' }))

      expect(kitchenApi.startOrder).toHaveBeenCalledWith(2)
      await vi.waitFor(() => expect(kitchenApi.fetchKitchenQueue).toHaveBeenCalledTimes(2))
    })

    it('bếp được làm trước đơn sắp tới lúc vắng, có gợi ý giờ cần chuẩn bị (BR-21)', async () => {
      renderWithAuth(<KitchenPage />, { auth: { user: kitchen } })

      const upcoming = await screen.findByRole('region', { name: 'Sắp tới' })
      expect(within(upcoming).getByText(/Cần chuẩn bị từ 12:00/)).toBeInTheDocument()
      await userEvent.click(await screen.findByRole('button', { name: 'Bắt đầu đơn M2C8QA' }))

      expect(kitchenApi.startOrder).toHaveBeenCalledWith(3)
    })

    it('Hoàn tất đơn đang làm (bước 6–7)', async () => {
      renderWithAuth(<KitchenPage />, { auth: { user: kitchen } })

      await userEvent.click(await screen.findByRole('button', { name: 'Hoàn tất đơn P4W9TD' }))

      expect(kitchenApi.completeOrder).toHaveBeenCalledWith(1)
      expect(kitchenApi.startOrder).not.toHaveBeenCalled()
      await vi.waitFor(() => expect(kitchenApi.fetchKitchenQueue).toHaveBeenCalledTimes(2))
    })

    it('đơn đã đổi trạng thái ở nơi khác: báo và làm mới, không coi là lỗi hệ thống (E1)', async () => {
      vi.mocked(kitchenApi.startOrder).mockRejectedValue(
        new ApiError(409, 'INVALID_STATUS_TRANSITION', 'x'),
      )
      renderWithAuth(<KitchenPage />, { auth: { user: kitchen } })

      await userEvent.click(await screen.findByRole('button', { name: 'Bắt đầu đơn D3F7KA' }))

      expect(await screen.findByText(/Đơn D3F7KA đã đổi trạng thái/)).toBeInTheDocument()
      await vi.waitFor(() => expect(kitchenApi.fetchKitchenQueue).toHaveBeenCalledTimes(2))
    })

    it('lỗi khác: báo lỗi cho bếp thử lại (E3)', async () => {
      vi.mocked(kitchenApi.completeOrder).mockRejectedValue(new ApiError(0, null, 'x'))
      renderWithAuth(<KitchenPage />, { auth: { user: kitchen } })

      await userEvent.click(await screen.findByRole('button', { name: 'Hoàn tất đơn P4W9TD' }))

      expect(await screen.findByText(/kết nối/)).toBeInTheDocument()
    })

    it('đang xử lý một đơn thì khóa các nút khác, tránh bấm chồng', async () => {
      let finish: () => void = () => undefined
      vi.mocked(kitchenApi.startOrder).mockReturnValue(
        new Promise((resolve) => {
          finish = () => resolve({ id: 2, status: 'PREPARING' })
        }),
      )
      renderWithAuth(<KitchenPage />, { auth: { user: kitchen } })

      await userEvent.click(await screen.findByRole('button', { name: 'Bắt đầu đơn D3F7KA' }))

      expect(screen.getByRole('button', { name: 'Hoàn tất đơn P4W9TD' })).toBeDisabled()
      await act(async () => finish())
      await vi.waitFor(() =>
        expect(screen.getByRole('button', { name: 'Hoàn tất đơn P4W9TD' })).toBeEnabled(),
      )
    })
  })
})
