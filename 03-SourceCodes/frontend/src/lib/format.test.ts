import { describe, expect, it } from 'vitest'
import { formatMoney } from './format'

describe('formatMoney', () => {
  it('ngăn cách hàng nghìn bằng dấu chấm và thêm đ', () => {
    expect(formatMoney(45000)).toBe('45.000đ')
    expect(formatMoney(1250000)).toBe('1.250.000đ')
  })

  it('giá 0 và giá nhỏ vẫn đúng', () => {
    expect(formatMoney(0)).toBe('0đ')
    expect(formatMoney(500)).toBe('500đ')
  })
})
