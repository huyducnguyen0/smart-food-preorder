import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { CurrentUser } from '../../api/auth'
import { ApiError } from '../../api/client'
import * as ordersApi from '../../api/orders'
import type { OrderDetail, SlotList } from '../../api/orders'
import { renderWithAuth } from '../../test/renderWithAuth'
import type { CartLine } from '../cart/cartContext'
import { CartPage } from '../cart/CartPage'
import { loadCart, saveCart } from '../cart/cartStorage'
import { OrderDetailPage } from '../orders/OrderDetailPage'
import { CheckoutPage } from './CheckoutPage'

vi.mock('../../api/orders')

const customer: CurrentUser = { id: 1, displayName: 'Khách Một', role: 'CUSTOMER' }

const burger: CartLine = {
  menuItemId: 12,
  name: 'Burger bò',
  imageUrl: null,
  unitPrice: 45000,
  quantity: 2,
}
const tea: CartLine = {
  menuItemId: 31,
  name: 'Trà đào',
  imageUrl: null,
  unitPrice: 20000,
  quantity: 1,
}

const slotList: SlotList = {
  date: '2031-03-05',
  unavailableReason: null,
  slots: [
    { start: '2031-03-05T11:30', end: '2031-03-05T11:45', bookable: true },
    { start: '2031-03-05T11:45', end: '2031-03-05T12:00', bookable: false },
    { start: '2031-03-05T12:00', end: '2031-03-05T12:15', bookable: true },
  ],
}

const placedOrder: OrderDetail = {
  id: 41,
  pickupCode: 'K7M3QX',
  slotStart: '2031-03-05T11:30',
  slotEnd: '2031-03-05T11:45',
  status: 'CONFIRMED',
  paymentStatus: 'UNPAID',
  paymentMethod: null,
  totalAmount: 110000,
  confirmedAt: '2031-03-05T09:00:00+07:00',
  lines: [
    { menuItemId: 12, name: 'Burger bò', quantity: 2, unitPrice: 45000, lineTotal: 90000 },
    { menuItemId: 31, name: 'Trà đào', quantity: 1, unitPrice: 20000, lineTotal: 20000 },
  ],
}

const routes = (
  <>
    <Route path="/cart" element={<CartPage />} />
    <Route path="/orders/:id" element={<OrderDetailPage />} />
    <Route path="/orders" element={<p>TRANG ĐƠN CỦA TÔI</p>} />
    <Route path="/login" element={<p>TRANG ĐĂNG NHẬP</p>} />
  </>
)

function renderCheckout(lines: CartLine[] = [burger, tea]) {
  saveCart(customer.id, lines)
  return renderWithAuth(<CheckoutPage />, {
    path: '/checkout',
    auth: { user: customer },
    extraRoutes: routes,
  })
}

async function pickAndConfirm(time = '11:30') {
  await userEvent.click(await screen.findByRole('button', { name: time }))
  await userEvent.click(screen.getByRole('button', { name: 'Xác nhận đặt' }))
}

describe('CheckoutPage', () => {
  beforeEach(() => {
    vi.mocked(ordersApi.fetchSlots).mockReset().mockResolvedValue(slotList)
    vi.mocked(ordersApi.placeOrder).mockReset().mockResolvedValue(placedOrder)
  })

  it('liệt kê khung theo giờ; khung hết chỗ không chọn được (UC-04 bước 6, BR-36)', async () => {
    renderCheckout()

    const group = await screen.findByRole('region', { name: '11 giờ' })
    expect(within(group).getByRole('button', { name: '11:30' })).toBeEnabled()
    expect(within(group).getByRole('button', { name: '11:45, không đủ chỗ' })).toBeDisabled()
    expect(screen.getByRole('region', { name: '12 giờ' })).toBeInTheDocument()
    expect(ordersApi.fetchSlots).toHaveBeenCalledWith(
      [
        { menuItemId: 12, quantity: 2 },
        { menuItemId: 31, quantity: 1 },
      ],
      expect.anything(),
    )
  })

  it('chưa chọn khung thì chưa xác nhận được; chọn rồi thì hiện giờ nhận và tóm tắt (bước 7)', async () => {
    renderCheckout()
    const confirm = await screen.findByRole('button', { name: 'Xác nhận đặt' })
    expect(confirm).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: '12:00' }))

    expect(confirm).toBeEnabled()
    expect(screen.getByText(/Nhận lúc 12:00 – 12:15/)).toBeInTheDocument()
    expect(screen.getByText('110.000đ')).toBeInTheDocument()
    expect(screen.getByText(/Thanh toán tại quầy/)).toBeInTheDocument()
  })

  it('đặt thành công: gửi đúng nội dung, hiện mã nhận, xóa giỏ (bước 8–11)', async () => {
    renderCheckout()

    await pickAndConfirm()

    expect(await screen.findByText('K7M3QX')).toBeInTheDocument()
    expect(screen.getByText(/Đặt đơn thành công/)).toBeInTheDocument()
    expect(screen.getByText('Đã xác nhận')).toBeInTheDocument()
    expect(screen.getByText(/Nhận lúc/)).toHaveTextContent('11:30 – 11:45')
    const request = vi.mocked(ordersApi.placeOrder).mock.calls[0][0]
    expect(request).toMatchObject({
      slotStart: '2031-03-05T11:30',
      lines: [
        { menuItemId: 12, quantity: 2, unitPrice: 45000 },
        { menuItemId: 31, quantity: 1, unitPrice: 20000 },
      ],
    })
    expect(request.requestId).toMatch(/^[0-9a-f-]{36}$/)
    expect(loadCart(customer.id)).toEqual([])
  })

  it('bấm xác nhận nhiều lần khi đang gửi thì chỉ gửi một lần (A1)', async () => {
    let finish: (order: OrderDetail) => void = () => undefined
    vi.mocked(ordersApi.placeOrder).mockReturnValue(
      new Promise<OrderDetail>((resolve) => {
        finish = resolve
      }),
    )
    renderCheckout()
    await userEvent.click(await screen.findByRole('button', { name: '11:30' }))

    const confirm = screen.getByRole('button', { name: 'Xác nhận đặt' })
    await userEvent.click(confirm)
    expect(screen.getByRole('button', { name: 'Đang đặt...' })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: 'Đang đặt...' }))

    expect(ordersApi.placeOrder).toHaveBeenCalledTimes(1)
    finish(placedOrder)
    expect(await screen.findByText('K7M3QX')).toBeInTheDocument()
  })

  it('lỗi hệ thống: giữ giỏ, báo lỗi, gửi lại dùng cùng mã yêu cầu (E10, A1)', async () => {
    vi.mocked(ordersApi.placeOrder)
      .mockRejectedValueOnce(new ApiError(500, 'INTERNAL_ERROR', 'x'))
      .mockResolvedValueOnce(placedOrder)
    renderCheckout()

    await pickAndConfirm()
    expect(await screen.findByRole('alert')).toHaveTextContent('lỗi hệ thống')
    expect(loadCart(customer.id)).toHaveLength(2)

    await userEvent.click(screen.getByRole('button', { name: 'Xác nhận đặt' }))
    expect(await screen.findByText('K7M3QX')).toBeInTheDocument()

    const [first, second] = vi.mocked(ordersApi.placeOrder).mock.calls
    expect(second[0].requestId).toBe(first[0].requestId)
  })

  it('khung vừa hết chỗ: ở lại, bỏ chọn và tải lại danh sách khung (E8)', async () => {
    vi.mocked(ordersApi.placeOrder).mockRejectedValue(new ApiError(409, 'SLOT_FULL', 'x'))
    renderCheckout()

    await pickAndConfirm()

    expect(await screen.findByRole('alert')).toHaveTextContent('vừa hết chỗ')
    expect(screen.getByRole('button', { name: 'Xác nhận đặt' })).toBeDisabled()
    expect(ordersApi.fetchSlots).toHaveBeenCalledTimes(2)
    expect(loadCart(customer.id)).toHaveLength(2)
  })

  it('khung không còn hợp lệ: ở lại và tải lại danh sách khung (E6)', async () => {
    vi.mocked(ordersApi.placeOrder).mockRejectedValue(new ApiError(409, 'SLOT_NOT_BOOKABLE', 'x'))
    renderCheckout()

    await pickAndConfirm()

    expect(await screen.findByRole('alert')).toHaveTextContent('không còn đặt được')
    expect(ordersApi.fetchSlots).toHaveBeenCalledTimes(2)
  })

  it('món đổi giá hoặc hết: cập nhật giỏ rồi về giỏ kèm thông báo, chưa tạo đơn (E7)', async () => {
    vi.mocked(ordersApi.placeOrder).mockRejectedValue(
      new ApiError(409, 'ITEMS_CHANGED', 'x', {
        items: [
          { menuItemId: 12, reason: 'PRICE_CHANGED', currentPrice: 50000 },
          { menuItemId: 31, reason: 'UNAVAILABLE' },
        ],
      }),
    )
    renderCheckout()

    await pickAndConfirm()

    expect(await screen.findByText(/Giỏ hàng đã được cập nhật/)).toBeInTheDocument()
    expect(screen.getByText('Burger bò: giá mới 50.000đ.')).toBeInTheDocument()
    expect(screen.getByText('Trà đào: không còn bán, đã bỏ khỏi giỏ.')).toBeInTheDocument()
    expect(loadCart(customer.id)).toEqual([{ ...burger, unitPrice: 50000 }])
    expect(screen.getAllByText('100.000đ')).toHaveLength(2) // thành tiền dòng và tổng: 2 × 50.000
  })

  it('đơn quá lớn cho một khung: về giỏ để bớt món (E9)', async () => {
    vi.mocked(ordersApi.placeOrder).mockRejectedValue(new ApiError(409, 'ORDER_TOO_LARGE', 'x'))
    renderCheckout()

    await pickAndConfirm()

    expect(await screen.findByText(/Đơn quá lớn/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Giỏ hàng' })).toBeInTheDocument()
    expect(loadCart(customer.id)).toHaveLength(2)
  })

  it('đã đủ số đơn chưa kết thúc: giữ giỏ, báo và dẫn tới Đơn của tôi (E5)', async () => {
    vi.mocked(ordersApi.placeOrder).mockRejectedValue(
      new ApiError(409, 'OPEN_ORDER_LIMIT_REACHED', 'x'),
    )
    renderCheckout()

    await pickAndConfirm()

    expect(await screen.findByRole('alert')).toHaveTextContent('quá nhiều đơn')
    await userEvent.click(screen.getByRole('link', { name: 'Xem đơn của tôi' }))
    expect(screen.getByText('TRANG ĐƠN CỦA TÔI')).toBeInTheDocument()
    expect(loadCart(customer.id)).toHaveLength(2)
  })

  it('phiên hết hạn khi gửi: chuyển tới đăng nhập, giỏ vẫn còn (E11)', async () => {
    vi.mocked(ordersApi.placeOrder).mockRejectedValue(new ApiError(401, 'UNAUTHENTICATED', 'x'))
    renderCheckout()

    await pickAndConfirm()

    expect(await screen.findByText('TRANG ĐĂNG NHẬP')).toBeInTheDocument()
    expect(loadCart(customer.id)).toHaveLength(2)
  })

  it('không còn khung hôm nay hoặc hết chỗ cả ngày: báo lý do, giữ giỏ (E2, E3, E4)', async () => {
    const cases = [
      ['NO_SLOT_LEFT_TODAY', 'không còn khung nhận'],
      ['ALL_SLOTS_FULL', 'đã hết chỗ'],
      ['ORDER_TOO_LARGE', 'Đơn quá lớn'],
    ] as const
    for (const [reason, text] of cases) {
      vi.mocked(ordersApi.fetchSlots).mockResolvedValue({
        date: '2031-03-05',
        slots: [],
        unavailableReason: reason,
      })
      const view = renderCheckout()

      expect(await screen.findByRole('alert')).toHaveTextContent(text)
      expect(screen.getByRole('link', { name: 'Về giỏ hàng' })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Xác nhận đặt' })).toBeDisabled()
      view.unmount()
    }
  })

  it('không tải được khung: báo lỗi và thử lại được', async () => {
    vi.mocked(ordersApi.fetchSlots)
      .mockRejectedValueOnce(new ApiError(0, null, 'x'))
      .mockResolvedValueOnce(slotList)
    renderCheckout()

    expect(await screen.findByRole('alert')).toHaveTextContent('kết nối')
    await userEvent.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('button', { name: '11:30' })).toBeInTheDocument()
  })

  it('giỏ trống thì quay về giỏ, không gọi API (A2)', async () => {
    renderCheckout([])

    expect(await screen.findByText('Giỏ hàng đang trống')).toBeInTheDocument()
    expect(ordersApi.fetchSlots).not.toHaveBeenCalled()
  })
})
