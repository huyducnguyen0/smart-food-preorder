import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { ApiError } from '../../api/client'
import { messageOf } from '../../api/errorMessages'
import { cancelMyOrder, fetchOrder, isOpenStatus, type OrderDetail } from '../../api/orders'
import { Alert } from '../../components/Alert'
import { timeOf } from '../../lib/orderLabels'
import { usePolling } from '../../lib/usePolling'
import { OrderDetailView } from './OrderDetailView'

/** PB-14: trang đơn tự hỏi lại trạng thái mỗi 15 giây khi đơn chưa kết thúc. */
export const ORDER_POLL_MS = 15_000

type State =
  | { kind: 'loading' }
  | { kind: 'error'; message: string; notFound: boolean }
  | { kind: 'ready'; order: OrderDetail }

function cancelErrorMessage(error: unknown): string {
  if (
    error instanceof ApiError &&
    error.code === 'CANCEL_NOT_ALLOWED' &&
    error.extra.reason === 'PAID'
  ) {
    return 'Đơn đã thanh toán nên không tự hủy được, vui lòng liên hệ cửa hàng để hủy và hoàn tiền.'
  }
  if (error instanceof ApiError && error.code === 'CANCEL_NOT_ALLOWED') {
    const deadline =
      typeof error.extra.cancelDeadline === 'string' ? error.extra.cancelDeadline : null
    return deadline
      ? `Đơn đã vào thời gian chuẩn bị (từ ${timeOf(deadline)}) nên không tự hủy được, vui lòng liên hệ cửa hàng.`
      : 'Đơn đã vào thời gian chuẩn bị nên không tự hủy được, vui lòng liên hệ cửa hàng.'
  }
  if (error instanceof ApiError && error.code === 'INVALID_STATUS_TRANSITION') {
    return 'Cửa hàng đã bắt đầu làm món nên đơn không tự hủy được nữa, vui lòng liên hệ cửa hàng.'
  }
  return messageOf(error)
}

/**
 * Trang `/orders/:id` (UC-05 bước 3–5). Vừa đặt xong thì đơn được truyền theo điều hướng để hiện kết quả ngay (UC-04
 * bước 11); mở lại từ danh sách hoặc từ địa chỉ trang thì tải từ API. Đơn không phải của mình báo như không tồn tại
 * (E1). Đơn chưa kết thúc được tự làm mới và báo nổi bật khi chuyển sang sẵn sàng (PB-14). Khách hủy được đơn đã xác
 * nhận trước cửa sổ chuẩn bị (UC-06).
 */
export function OrderDetailPage() {
  const { id } = useParams()
  const navState = useLocation().state as { order?: OrderDetail; placed?: boolean } | null
  const [state, setState] = useState<State>(
    navState?.order ? { kind: 'ready', order: navState.order } : { kind: 'loading' },
  )
  const [placed, setPlaced] = useState(navState?.placed === true)
  const [attempt, setAttempt] = useState(0)
  const [becameReady, setBecameReady] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const [cancelError, setCancelError] = useState<string | null>(null)
  const lastStatus = useRef<string | null>(navState?.order?.status ?? null)
  const orderId = Number(id)

  /** Nhận đơn mới từ backend; đơn vừa chuyển sang READY thì bật thông báo (PB-14). */
  const receive = useCallback((order: OrderDetail) => {
    if (lastStatus.current !== null && lastStatus.current !== 'READY' && order.status === 'READY') {
      setBecameReady(true)
    }
    lastStatus.current = order.status
    setState({ kind: 'ready', order })
  }, [])

  useEffect(() => {
    // Lần đầu có sẵn đơn vừa tạo thì không cần tải lại; chỉ tải khi mở trực tiếp hoặc khi khách bấm "Tải lại".
    if (attempt === 0 && navState?.order) return
    const controller = new AbortController()
    fetchOrder(orderId, controller.signal)
      .then(receive)
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        const notFound = error instanceof ApiError && error.status === 404
        setState({ kind: 'error', message: messageOf(error), notFound })
      })
    return () => controller.abort()
  }, [attempt, orderId, navState, receive])

  const polling = state.kind === 'ready' && isOpenStatus(state.order.status)
  usePolling(
    () => {
      // Làm mới ngầm: lỗi mạng tạm thời thì giữ nguyên màn hình, lượt sau thử lại.
      fetchOrder(orderId).then(receive, () => undefined)
    },
    ORDER_POLL_MS,
    polling,
  )

  const reload = useCallback(() => {
    setPlaced(false)
    setCancelError(null)
    setState({ kind: 'loading' })
    setAttempt((value) => value + 1)
  }, [])

  const cancel = useCallback(async () => {
    setCancelling(true)
    setCancelError(null)
    try {
      await cancelMyOrder(orderId)
      setPlaced(false)
      receive(await fetchOrder(orderId))
    } catch (error) {
      setCancelError(cancelErrorMessage(error))
    } finally {
      setCancelling(false)
    }
  }, [orderId, receive])

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
      {becameReady && state.order.status === 'READY' && (
        <div className="content ready-banner">
          <Alert kind="success">Đơn của bạn đã sẵn sàng. Mời bạn đến quầy và đọc mã nhận.</Alert>
        </div>
      )}
      <OrderDetailView
        order={state.order}
        placed={placed}
        onCancel={cancel}
        cancelling={cancelling}
        cancelError={cancelError}
      />
      <div className="content reload-row">
        <button type="button" className="btn btn-block" onClick={reload}>
          Tải lại trạng thái
        </button>
        {polling && <p className="muted center">Trang tự cập nhật trạng thái mỗi 15 giây.</p>}
      </div>
    </>
  )
}
