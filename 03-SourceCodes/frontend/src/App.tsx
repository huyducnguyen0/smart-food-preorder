import { useEffect, useState } from 'react'
import { fetchHealth, type HealthResponse } from './api/health'

type CheckState =
  | { kind: 'loading' }
  | { kind: 'done'; health: HealthResponse }
  | { kind: 'error'; message: string }

// Màn hình khung (T-03): xác nhận frontend gọi được backend và backend truy cập được database.
// Sẽ được thay bằng các màn hình nghiệp vụ từ các task sau.
function App() {
  const [state, setState] = useState<CheckState>({ kind: 'loading' })
  // Mỗi lần tăng attempt, effect bên dưới chạy lại và gọi API lần nữa.
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    fetchHealth(controller.signal)
      .then((health) => setState({ kind: 'done', health }))
      .catch((error: unknown) => {
        // Bỏ qua lỗi do chính effect hủy request khi component unmount hoặc chạy lại.
        if (controller.signal.aborted) return
        const message = error instanceof Error ? error.message : String(error)
        setState({ kind: 'error', message: `Không kết nối được backend: ${message}` })
      })
    return () => controller.abort()
  }, [attempt])

  function handleRetry() {
    setState({ kind: 'loading' })
    setAttempt((value) => value + 1)
  }

  return (
    <main className="container">
      <h1>Hệ thống đặt món trước</h1>
      <p className="subtitle">Kiểm tra kết nối frontend → backend → database</p>

      <section className="card" aria-live="polite">
        {state.kind === 'loading' && <p>Đang kiểm tra...</p>}

        {state.kind === 'error' && <p className="status down">{state.message}</p>}

        {state.kind === 'done' && (
          <dl>
            <dt>Trạng thái chung</dt>
            <dd className={`status ${state.health.status === 'UP' ? 'up' : 'down'}`}>
              {state.health.status}
            </dd>
            <dt>Database</dt>
            <dd className={`status ${state.health.database === 'UP' ? 'up' : 'down'}`}>
              {state.health.database}
            </dd>
            <dt>Phiên bản schema</dt>
            <dd>{state.health.schemaVersion ?? '—'}</dd>
            <dt>Giờ máy chủ</dt>
            <dd>{state.health.serverTime}</dd>
          </dl>
        )}
      </section>

      <button type="button" onClick={handleRetry} disabled={state.kind === 'loading'}>
        Kiểm tra lại
      </button>
    </main>
  )
}

export default App
