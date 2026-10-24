import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Dọn giao diện đã dựng sau mỗi test để các test không ảnh hưởng nhau.
afterEach(() => {
  cleanup()
})
