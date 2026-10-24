import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { CurrentUser } from '../../api/auth'
import { ApiError } from '../../api/client'
import * as ordersApi from '../../api/orders'
import type { OrderDetail, OrderSummary } from '../../api/orders'
import { renderWithAuth } from '../../test/renderWithAuth'
import { OrderDetailPage } from './OrderDetailPage'
import { OrdersPage } from './OrdersPage'

vi.mock('../../api/orders')

const customer: CurrentUser = { id: 1, displayName: 'Khách Một', role: 'CUSTOMER' }

const summaries: OrderSummary[] = [
  {
    id: 41,
    pickupCode: 'K7M3QX',
    slotStart: '2031-03-05T11:30',
    slotEnd: '2031-03-05T11:45',
    status: 'READY',
    paymentStatus: 'UNPAID',
    totalAmount: 110000,
  },
  {
    id: 40,
    pickupCode: 'B2C4DE',
    slotStart: '2031-03-04T12:00',
    slotEnd: '2031-03-04T12:15',
    status: 'PICKED_UP',
    paymentStatus: 'PAID',
    totalAmount: 45000,
  },
]

const detail: OrderDetail = {
  ...summaries[0],
  paymentMethod: null,
  confirmedAt: '2031-03-05T09:00:00+07:00',
  lines: [{ menuItemId: 12, name: 'Burger bò', quantity: 2, unitPrice: 45000, lineTotal: 90000 }],
}

describe('OrdersPage (UC-05 bước 1–2)', () => {
  beforeEach(() => {
    vi.mocked(ordersApi.fetchMyOrders).mockReset()
  })

  it('liệt kê đơn với mã nhận, ngày, khung, trạng thái, thanh toán, tổng tiền', async () => {
    vi.mocked(ordersApi.fetchMyOrders).mockResolvedValue(summaries)
    renderWithAuth(<OrdersPage />, { auth: { user: customer } })

    expect(await screen.findByText('K7M3QX')).toBeInTheDocument()
    expect(screen.getByText('05/03/2031 · 11:30 – 11:45')).toBeInTheDocument()
    expect(screen.getByText('Sẵn sàng nhận')).toBeInTheDocument()
    expect(screen.getByText('Chưa thanh toán')).toBeInTheDocument()
    expect(screen.getByText('110.000đ')).toBeInTheDocument()
    expect(screen.getByText('Đã nhận')).toBeInTheDocument()
    expect(screen.getByText('Đã thanh toán')).toBeInTheDocument()
    // Giữ nguyên thứ tự backend trả về.
    const codes = screen.getAllByText(/^[A-Z0-9]{6}$/).map((el) => el.textContent)
    expect(codes).toEqual(['K7M3QX', 'B2C4DE'])
  })

  it('chưa có đơn thì báo và dẫn tới thực đơn (A1)', async () => {
    vi.mocked(ordersApi.fetchMyOrders).mockResolvedValue([])
    renderWithAuth(<OrdersPage />, {
      path: '/orders',
      auth: { user: customer },
      extraRoutes: <Route path="/" element={<p>TRANG THỰC ĐƠN</p>} />,
    })

    expect(await screen.findByText('Bạn chưa có đơn.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('link', { name: 'Xem thực đơn' }))
    expect(screen.getByText('TRANG THỰC ĐƠN')).toBeInTheDocument()
  })

  it('chọn một đơn thì mở chi tiết (bước 3)', async () => {
    vi.mocked(ordersApi.fetchMyOrders).mockResolvedValue(summaries)
    renderWithAuth(<OrdersPage />, {
      auth: { user: customer },
      extraRoutes: <Route path="/orders/:id" element={<p>CHI TIẾT ĐƠN</p>} />,
    })

    await userEvent.click(await screen.findByRole('link', { name: /K7M3QX/ }))

    expect(screen.getByText('CHI TIẾT ĐƠN')).toBeInTheDocument()
  })

  it('tải lại để thấy trạng thái mới (bước 5)', async () => {
    vi.mocked(ordersApi.fetchMyOrders)
      .mockResolvedValueOnce([summaries[0]])
      .mockResolvedValueOnce([{ ...summaries[0], status: 'PREPARING' }])
    renderWithAuth(<OrdersPage />, { auth: { user: customer } })
    expect(await screen.findByText('Sẵn sàng nhận')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Tải lại' }))

    expect(await screen.findByText('Đang chuẩn bị')).toBeInTheDocument()
  })

  it('không tải được thì báo lỗi, không hiện đơn, và thử lại được (E3)', async () => {
    vi.mocked(ordersApi.fetchMyOrders)
      .mockRejectedValueOnce(new ApiError(0, null, 'x'))
      .mockResolvedValueOnce(summaries)
    renderWithAuth(<OrdersPage />, { auth: { user: customer } })

    expect(await screen.findByRole('alert')).toHaveTextContent('kết nối')
    expect(screen.queryByText('K7M3QX')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByText('K7M3QX')).toBeInTheDocument()
  })
})

describe('OrderDetailPage (UC-05 bước 3–5, E1, E3)', () => {
  beforeEach(() => {
    vi.mocked(ordersApi.fetchOrder).mockReset()
  })

  function renderDetail(path = '/orders/41') {
    return renderWithAuth(<p />, {
      path,
      auth: { user: customer },
      extraRoutes: (
        <>
          <Route path="/orders/:id" element={<OrderDetailPage />} />
          <Route path="/orders" element={<p>TRANG ĐƠN CỦA TÔI</p>} />
        </>
      ),
    })
  }

  it('mở theo địa chỉ thì tải đơn từ API và hiện mã nhận, món, trạng thái', async () => {
    vi.mocked(ordersApi.fetchOrder).mockResolvedValue(detail)
    renderDetail()

    expect(await screen.findByText('K7M3QX')).toBeInTheDocument()
    expect(ordersApi.fetchOrder).toHaveBeenCalledWith(41, expect.anything())
    expect(screen.getByText('Sẵn sàng nhận')).toBeInTheDocument()
    expect(screen.getByText('2 × Burger bò')).toBeInTheDocument()
    expect(screen.getByText(/Chưa thanh toán: thanh toán tại quầy/)).toBeInTheDocument()
    expect(screen.getByText(/Đặt lúc 09:00/)).toBeInTheDocument()
    expect(screen.queryByText(/Đặt đơn thành công/)).not.toBeInTheDocument()
  })

  it('đơn của người khác hoặc không có: báo không tìm thấy, về danh sách (E1)', async () => {
    vi.mocked(ordersApi.fetchOrder).mockRejectedValue(new ApiError(404, 'ORDER_NOT_FOUND', 'x'))
    renderDetail('/orders/999')

    expect(await screen.findByRole('alert')).toHaveTextContent('Không tìm thấy đơn')
    await userEvent.click(screen.getByRole('link', { name: 'Về đơn của tôi' }))
    expect(screen.getByText('TRANG ĐƠN CỦA TÔI')).toBeInTheDocument()
  })

  it('không tải được: báo lỗi và thử lại được (E3)', async () => {
    vi.mocked(ordersApi.fetchOrder)
      .mockRejectedValueOnce(new ApiError(0, null, 'x'))
      .mockResolvedValueOnce(detail)
    renderDetail()

    expect(await screen.findByRole('alert')).toHaveTextContent('kết nối')
    await userEvent.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByText('K7M3QX')).toBeInTheDocument()
  })

  it('tải lại trạng thái lấy dữ liệu mới nhất (bước 5)', async () => {
    vi.mocked(ordersApi.fetchOrder)
      .mockResolvedValueOnce(detail)
      .mockResolvedValueOnce({
        ...detail,
        status: 'PICKED_UP',
        paymentStatus: 'PAID',
        paymentMethod: 'CASH',
      })
    renderDetail()
    expect(await screen.findByText('Sẵn sàng nhận')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Tải lại trạng thái' }))

    expect(await screen.findByText('Đã nhận')).toBeInTheDocument()
    expect(screen.getByText(/Đã thanh toán bằng tiền mặt/)).toBeInTheDocument()
  })
})
