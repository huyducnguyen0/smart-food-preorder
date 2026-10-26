import { describe, expect, it } from 'vitest'
import { dateOf, slotRange, STATUS_LABELS, timeOf } from './orderLabels'

describe('orderLabels', () => {
  it('tên sáu trạng thái hiển thị cho khách khớp SRS B.5 và UC-05', () => {
    expect(STATUS_LABELS).toEqual({
      CONFIRMED: 'Đã xác nhận',
      PREPARING: 'Đang chuẩn bị',
      READY: 'Sẵn sàng nhận',
      PICKED_UP: 'Đã nhận',
      CANCELLED: 'Đã hủy',
      NO_SHOW: 'Không đến nhận',
    })
  })

  it('đổi thời điểm ISO thành ngày và giờ hiển thị', () => {
    expect(dateOf('2026-10-17T11:30')).toBe('17/10/2026')
    expect(timeOf('2026-10-17T11:30')).toBe('11:30')
    expect(slotRange('2026-10-17T11:30', '2026-10-17T11:45')).toBe('11:30 – 11:45')
  })
})
