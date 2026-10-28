import { useEffect, useRef } from 'react'

/**
 * Gọi `tick` mỗi `intervalMs` khi `enabled`. Bỏ qua lượt khi tab đang ẩn để không gọi API vô ích; lượt kế tiếp sau
 * khi khách quay lại sẽ lấy trạng thái mới. Không có email/SMS: trang tự hỏi lại backend (PB-14).
 */
export function usePolling(tick: () => void, intervalMs: number, enabled = true) {
  const latest = useRef(tick)
  useEffect(() => {
    latest.current = tick
  }, [tick])

  useEffect(() => {
    if (!enabled) return
    const timer = window.setInterval(() => {
      if (document.visibilityState !== 'hidden') latest.current()
    }, intervalMs)
    return () => window.clearInterval(timer)
  }, [intervalMs, enabled])
}
