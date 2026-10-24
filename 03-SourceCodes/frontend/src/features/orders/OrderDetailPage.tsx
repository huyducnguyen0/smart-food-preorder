import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { ApiError } from '../../api/client'
import { messageOf } from '../../api/errorMessages'
import { fetchOrder, type OrderDetail } from '../../api/orders'
import { Alert } from '../../components/Alert'
import { OrderDetailView } from './OrderDetailView'

type State =
  | { kind: 'loading' }
  | { kind: 'error'; message: string; notFound: boolean }
  | { kind: 'ready'; order: OrderDetail }

/**
 * Trang `/orders/:id` (UC-05 bước 3–5). Vừa đặt xong thì đơn được truyền theo điều hướng để hiện kết quả ngay (UC-04
 * bước 11); mở lại từ danh sách hoặc từ địa chỉ trang thì tải từ API. Đơn không phải của mình báo như không tồn tại
 * (E1).
 */
export function OrderDetailPage() {
  const { id } = useParams()
  const navState = useLocation().state as { order?: OrderDetail; placed?: boolean } | null
  const [state, setState] = useState<State>(
    navState?.order ? { kind: 'ready', order: navState.order } : { kind: 'loading' },
  )
  const [placed, setPlaced] = useState(navState?.placed === true)
  const [attempt, setAttempt] = useState(0)
  const orderId = Number(id)

  useEffect(() => {
    // Lần đầu có sẵn đơn vừa tạo thì không cần tải lại; chỉ tải khi mở trực tiếp hoặc khi khách bấm "Tải lại".
    if (attempt === 0 && navState?.order) return
    const controller = new AbortController()
    fetchOrder(orderId, controller.signal)
      .then((order) => setState({ kind: 'ready', order }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        const notFound = error instanceof ApiError && error.status === 404
        setState({ kind: 'error', message: messageOf(error), notFound })
      })
    return () => controller.abort()
  }, [attempt, orderId, navState])

  const reload = useCallback(() => {
    setPlaced(false)
    setState({ kind: 'loading' })
    setAttempt((value) => value + 1)
  }, [])

  if (state.kind === 'loading') {
    return (
      <p className="muted center" role="status">
        Đang tải đơn...
      </p>
    )
  }
  if (state.kind === 'error') {
    return (
      <div className="content">
        <Alert kind="danger">{state.message}</Alert>
        {state.notFound ? (
          <Link className="btn btn-primary btn-block" to="/orders">
            Về đơn của tôi
          </Link>
        ) : (
          <button type="button" className="btn btn-primary btn-block" onClick={reload}>
            Thử lại
          </button>
        )}
      </div>
    )
  }
  return (
    <>
      <OrderDetailView order={state.order} placed={placed} />
      <div className="content reload-row">
        <button type="button" className="btn btn-block" onClick={reload}>
          Tải lại trạng thái
        </button>
      </div>
    </>
  )
}
