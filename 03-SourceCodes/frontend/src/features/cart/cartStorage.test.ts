import { describe, expect, it, vi } from 'vitest'
import type { CartLine } from './cartContext'
import { loadCart, saveCart } from './cartStorage'

const burger: CartLine = {
  menuItemId: 1,
  name: 'Burger bò',
  imageUrl: null,
  unitPrice: 45000,
  quantity: 2,
}

describe('cartStorage', () => {
  it('lưu rồi đọc lại đúng giỏ của từng tài khoản', () => {
    saveCart(1, [burger])
    saveCart(2, [{ ...burger, menuItemId: 2, name: 'Trà đào', quantity: 1 }])

    expect(loadCart(1)).toEqual([burger])
    expect(loadCart(2)).toHaveLength(1)
    expect(loadCart(2)[0].name).toBe('Trà đào')
    expect(loadCart(3)).toEqual([])
  })

  it('giỏ trống thì xóa khỏi bộ nhớ', () => {
    saveCart(1, [burger])
    saveCart(1, [])

    expect(window.localStorage.length).toBe(0)
    expect(loadCart(1)).toEqual([])
  })

  it('dữ liệu hỏng thì coi như giỏ trống, không báo lỗi', () => {
    window.localStorage.setItem('nhom15.cart.v1.1', '{không phải json')
    expect(loadCart(1)).toEqual([])

    window.localStorage.setItem('nhom15.cart.v1.1', '{"menuItemId":1}')
    expect(loadCart(1)).toEqual([])
  })

  it('bỏ dòng sai kiểu, số lượng ngoài khoảng và món lặp, giữ dòng hợp lệ', () => {
    window.localStorage.setItem(
      'nhom15.cart.v1.1',
      JSON.stringify([
        burger,
        { ...burger, quantity: 0 },
        { ...burger, menuItemId: 5, quantity: 1000 },
        { ...burger, menuItemId: 6, unitPrice: -1 },
        { ...burger, menuItemId: 7, quantity: 1.5 },
        { ...burger, menuItemId: 8, name: 99 },
        { ...burger, quantity: 3 },
        null,
        'x',
      ]),
    )

    expect(loadCart(1)).toEqual([burger])
  })

  it('bộ nhớ trình duyệt không dùng được thì không làm hỏng ứng dụng', () => {
    const getItem = vi.spyOn(window.localStorage, 'getItem').mockImplementation(() => {
      throw new Error('bị chặn')
    })
    const setItem = vi.spyOn(window.localStorage, 'setItem').mockImplementation(() => {
      throw new Error('bị chặn')
    })

    expect(loadCart(1)).toEqual([])
    expect(() => saveCart(1, [burger])).not.toThrow()

    getItem.mockRestore()
    setItem.mockRestore()
  })
})
