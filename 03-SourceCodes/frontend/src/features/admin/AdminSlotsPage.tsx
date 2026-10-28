import { useCallback, useEffect, useState, type FormEvent } from 'react'
import {
  fetchAdminDay,
  setSlotQuota,
  updateSettings,
  type AdminDay,
  type AdminSlot,
} from '../../api/admin'
import { ApiError } from '../../api/client'
import { messageOf } from '../../api/errorMessages'
import { Alert } from '../../components/Alert'
import { slotRange, timeOf } from '../../lib/orderLabels'

function describe(error: unknown): string {
  if (error instanceof ApiError) {
    if (Array.isArray(error.extra.errors)) {
      const details = (error.extra.errors as { message?: string }[])
        .map((item) => item.message)
        .filter(Boolean)
      if (details.length > 0) return details.join(' ')
    }
    // Lỗi BR-11 có câu cụ thể (khung nào, đang giữ bao nhiêu) từ backend.
    if (error.code === 'QUOTA_BELOW_HELD' || error.code === 'HOURS_CONFLICT_WITH_ORDERS') {
      return error.message
    }
  }
  return messageOf(error)
}

function SlotRow({
  slot,
  busy,
  onSave,
}: {
  slot: AdminSlot
  busy: boolean
  onSave: (slot: AdminSlot, quota: number | null) => void
}) {
  const [value, setValue] = useState(String(slot.quota))
  const label = timeOf(slot.start)
  const parsed = Number(value)
  const valid = value !== '' && Number.isInteger(parsed) && parsed >= 0
  return (
    <li className="slot-row">
      <span className="slot-time">{slotRange(slot.start, slot.end)}</span>
      <span className="muted">
        Giữ {slot.heldPoints}/{slot.quota}
        {slot.quotaOverride !== null ? ' (riêng)' : ''} · {slot.openOrders} đơn
      </span>
      <span className="slot-edit">
        <input
          className="c-search"
          inputMode="numeric"
          aria-label={`Quota khung ${label}`}
          value={value}
          onChange={(e) => setValue(e.target.value)}
        />
        <button
          type="button"
          className="btn"
          disabled={busy || !valid || parsed === slot.quota}
          aria-label={`Lưu quota khung ${label}`}
          onClick={() => onSave(slot, parsed)}
        >
          Lưu
        </button>
        {slot.quotaOverride !== null && (
          <button
            type="button"
            className="link-btn"
            disabled={busy}
            aria-label={`Về mặc định khung ${label}`}
            onClick={() => onSave(slot, null)}
          >
            Mặc định
          </button>
        )}
      </span>
    </li>
  )
}

/**
 * UC-14 Cấu hình giờ mở cửa và quota (quản lý): xem quota, điểm đã giữ và số đơn từng khung hôm nay; đặt giờ mở cửa,
 * quota mặc định; sửa quota riêng một khung. Không bao giờ thấp hơn điểm đã giữ (BR-11).
 */
export function AdminSlotsPage() {
  const [day, setDay] = useState<AdminDay | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [settings, setSettings] = useState({ openTime: '', closeTime: '', defaultQuotaPoints: '' })
  const [message, setMessage] = useState<{ kind: 'success' | 'danger'; text: string } | null>(null)
  const [busy, setBusy] = useState(false)
  const [tick, setTick] = useState(0)

  const apply = useCallback((data: AdminDay) => {
    setDay(data)
    setSettings({
      openTime: data.openTime,
      closeTime: data.closeTime,
      defaultQuotaPoints: String(data.defaultQuotaPoints),
    })
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    fetchAdminDay(controller.signal)
      .then((data) => {
        apply(data)
        setLoadError(null)
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) setLoadError(messageOf(error))
      })
    return () => controller.abort()
  }, [tick, apply])

  async function run(action: () => Promise<AdminDay>, success: string) {
    setBusy(true)
    setMessage(null)
    try {
      apply(await action())
      setMessage({ kind: 'success', text: success })
    } catch (error) {
      setMessage({ kind: 'danger', text: describe(error) })
      setTick((value) => value + 1)
    } finally {
      setBusy(false)
    }
  }

  function handleSettings(event: FormEvent) {
    event.preventDefault()
    const quota = Number(settings.defaultQuotaPoints)
    if (!Number.isInteger(quota) || quota < 0 || settings.defaultQuotaPoints === '') {
      setMessage({ kind: 'danger', text: 'Quota mặc định phải là số nguyên không âm.' })
      return
    }
    void run(
      () =>
        updateSettings({
          openTime: settings.openTime,
          closeTime: settings.closeTime,
          defaultQuotaPoints: quota,
        }),
      'Đã lưu giờ mở cửa và quota mặc định.',
    )
  }

  return (
    <div className="content">
      <h1>Giờ mở cửa và quota</h1>
      {message && <Alert kind={message.kind}>{message.text}</Alert>}
      {loadError && <Alert kind="danger">{loadError}</Alert>}

      <form
        className="admin-form"
        onSubmit={handleSettings}
        noValidate
        aria-label="Cấu hình cửa hàng"
      >
        <div className="admin-row">
          <label className="field">
            <span>Mở cửa</span>
            <input
              type="time"
              step={900}
              value={settings.openTime}
              onChange={(e) => setSettings((s) => ({ ...s, openTime: e.target.value }))}
            />
          </label>
          <label className="field">
            <span>Đóng cửa</span>
            <input
              type="time"
              step={900}
              value={settings.closeTime}
              onChange={(e) => setSettings((s) => ({ ...s, closeTime: e.target.value }))}
            />
          </label>
          <label className="field">
            <span>Quota mặc định (điểm)</span>
            <input
              inputMode="numeric"
              value={settings.defaultQuotaPoints}
              onChange={(e) => setSettings((s) => ({ ...s, defaultQuotaPoints: e.target.value }))}
            />
          </label>
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy}>
          Lưu cấu hình
        </button>
        <p className="muted">
          Giờ đúng mốc 15 phút. Không thu hẹp giờ làm mất khung đang có đơn, không đặt quota thấp
          hơn điểm đã giữ.
        </p>
      </form>

      {day && (
        <section aria-label="Khung hôm nay">
          <h2 className="section-title">Khung hôm nay ({day.slots.length})</h2>
          <ul className="slot-rows">
            {day.slots.map((slot) => (
              <SlotRow
                key={`${slot.start}-${slot.quota}-${slot.quotaOverride}`}
                slot={slot}
                busy={busy}
                onSave={(target, quota) =>
                  void run(
                    () => setSlotQuota(timeOf(target.start), quota),
                    `Đã cập nhật quota khung ${timeOf(target.start)}.`,
                  )
                }
              />
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}
