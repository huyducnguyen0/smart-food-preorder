import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { fetchDashboard, type Dashboard } from '../../api/admin'
import { messageOf } from '../../api/errorMessages'
import { Alert } from '../../components/Alert'
import { slotRange, STATUS_LABELS, STATUS_TONES, timeOf } from '../../lib/orderLabels'
import { usePolling } from '../../lib/usePolling'

/** Bảng theo dõi tự làm mới mỗi 30 giây, như màn quầy và bếp. */
export const DASHBOARD_REFRESH_MS = 30_000

function Stat({
  label,
  value,
  alert = false,
}: {
  label: string
  value: string | number
  alert?: boolean
}) {
  return (
    <div className={`stat${alert ? ' alert-stat' : ''}`}>
      <span className="muted">{label}</span>
      <b>{value}</b>
    </div>
  )
}

/**
 * PB-17 Theo dõi vận hành trong ngày (FR-20): quota, điểm đã giữ và số đơn từng khung; đơn đang xử lý và đơn trễ
 * (BR-26); thống kê theo nguồn và kết quả. Số liệu là mức tải theo quota cửa hàng tự đặt, không đo năng lực bếp thật
 * (NT9).
 */
export function AdminDashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [tick, setTick] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    fetchDashboard(controller.signal)
      .then((result) => {
        setData(result)
        setError(null)
      })
      .catch((err: unknown) => {
        if (!controller.signal.aborted) setError(messageOf(err))
      })
    return () => controller.abort()
  }, [tick])

  const reload = useCallback(() => setTick((value) => value + 1), [])
  usePolling(reload, DASHBOARD_REFRESH_MS)

  const busySlots = data?.slots.filter((slot) => slot.heldPoints > 0 || slot.openOrders > 0) ?? []

  return (
    <div className="content">
      <div className="page-head">
        <h1>Theo dõi hôm nay</h1>
        <button type="button" className="link-btn" onClick={reload}>
          Tải lại
        </button>
      </div>
      {error && (
        <Alert kind="danger">
          {error}
          {data && ' Số liệu bên dưới có thể đã cũ.'}
        </Alert>
      )}
      {!data && !error && (
        <p className="muted center" role="status">
          Đang tải...
        </p>
      )}

      {data && (
        <>
          {data.stats.staleOpen > 0 && (
            <Alert kind="warning">
              Có {data.stats.staleOpen} đơn tồn từ ngày trước chưa xử lý. Vào{' '}
              <Link to="/counter">Quầy</Link> để hủy hoặc đánh dấu không đến nhận.
            </Alert>
          )}
          <section className="stat-grid" aria-label="Thống kê hôm nay">
            <Stat label="Đơn đặt trước" value={data.stats.preOrders} />
            <Stat label="Đơn tại quầy" value={data.stats.walkIns} />
            <Stat label="Đã nhận" value={data.stats.pickedUp} />
            <Stat label="Đang trễ" value={data.stats.lateNow} alert={data.stats.lateNow > 0} />
            <Stat label="Đã hủy" value={data.stats.cancelled} />
            <Stat label="Không đến nhận" value={data.stats.noShow} />
            <Stat
              label="Dùng quota"
              value={`${data.stats.quotaUsagePercent}% (${data.stats.heldPoints}/${data.stats.quotaPoints})`}
            />
          </section>
          <p className="muted">
            Quota là mức tải cửa hàng tự đặt cho mỗi khung, không phải năng lực bếp đo được.
          </p>

          <h2 className="section-title">Đơn đang xử lý ({data.activeOrders.length})</h2>
          {data.activeOrders.length === 0 ? (
            <p className="empty">Không có đơn đang xử lý.</p>
          ) : (
            <ul className="slot-rows" aria-label="Đơn đang xử lý">
              {data.activeOrders.map((order) => (
                <li key={order.id} className="slot-row">
                  <span className="slot-time">{order.pickupCode}</span>
                  <span className="muted">
                    Khung {timeOf(order.slotStart)} ·{' '}
                    {order.source === 'PRE_ORDER' ? 'Pre-order' : 'Walk-in'}
                  </span>
                  <span className={`status-pill tone-${STATUS_TONES[order.status]}`}>
                    {STATUS_LABELS[order.status]}
                  </span>
                  {order.late && <span className="status-pill tone-danger">Trễ</span>}
                </li>
              ))}
            </ul>
          )}

          <h2 className="section-title">Khung đang có đơn ({busySlots.length})</h2>
          {busySlots.length === 0 ? (
            <p className="empty">Chưa khung nào có đơn.</p>
          ) : (
            <ul className="slot-rows" aria-label="Tải theo khung">
              {busySlots.map((slot) => (
                <li key={slot.start} className="slot-row">
                  <span className="slot-time">{slotRange(slot.start, slot.end)}</span>
                  <span className="muted">
                    Giữ {slot.heldPoints}/{slot.quota} · {slot.openOrders} đơn
                  </span>
                  <span className="usage-bar" aria-hidden="true">
                    <span
                      style={{
                        width: `${slot.quota === 0 ? 100 : Math.min(100, (slot.heldPoints * 100) / slot.quota)}%`,
                      }}
                    />
                  </span>
                </li>
              ))}
            </ul>
          )}
          <p className="muted">
            Sửa quota từng khung ở <Link to="/admin/slots">Giờ và quota</Link>.
          </p>
        </>
      )}
    </div>
  )
}

/** Trang tổng "Quản lý": lối vào các màn cấu hình (giữ thanh tab ở đáy gọn trên điện thoại). */
export function AdminHubPage() {
  return (
    <div className="content">
      <h1>Quản lý</h1>
      <ul className="manage-links">
        <li>
          <Link className="btn btn-block" to="/admin/menu">
            Món và giá
          </Link>
        </li>
        <li>
          <Link className="btn btn-block" to="/admin/slots">
            Giờ mở cửa và quota
          </Link>
        </li>
        <li>
          <Link className="btn btn-block" to="/admin/staff">
            Tài khoản nhân viên
          </Link>
        </li>
      </ul>
    </div>
  )
}
