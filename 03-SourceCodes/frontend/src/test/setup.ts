import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Node 25 có sẵn một `localStorage` toàn cục chưa dùng được (cần cờ --localstorage-file) và che mất bản của jsdom.
// Thay bằng bản lưu trong bộ nhớ, đủ cho giỏ hàng lưu trên trình duyệt.
function createMemoryStorage(): Storage {
  const data = new Map<string, string>()
  return {
    get length() {
      return data.size
    },
    clear: () => data.clear(),
    getItem: (key) => data.get(key) ?? null,
    key: (index) => [...data.keys()][index] ?? null,
    removeItem: (key) => void data.delete(key),
    setItem: (key, value) => void data.set(key, String(value)),
  }
}

Object.defineProperty(window, 'localStorage', {
  value: createMemoryStorage(),
  configurable: true,
})

// Dọn giao diện đã dựng sau mỗi test để các test không ảnh hưởng nhau.
afterEach(() => {
  cleanup()
  window.localStorage.clear()
})
