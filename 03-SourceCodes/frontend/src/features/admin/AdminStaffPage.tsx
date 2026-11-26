import { useCallback, useEffect, useState, type FormEvent } from 'react'
import {
  createStaff,
  fetchStaff,
  setStaffLocked,
  type StaffAccount,
  type StaffRole,
} from '../../api/admin'
import { ApiError } from '../../api/client'
import { messageOf } from '../../api/errorMessages'
import { Alert } from '../../components/Alert'

const ROLE_LABELS: Record<StaffRole, string> = { COUNTER: 'Quầy', KITCHEN: 'Bếp' }

const EMPTY = { loginId: '', displayName: '', password: '', role: 'COUNTER' as StaffRole }

function describe(error: unknown): string {
  if (error instanceof ApiError && Array.isArray(error.extra.errors)) {
    const details = (error.extra.errors as { message?: string }[])
      .map((item) => item.message)
      .filter(Boolean)
    if (details.length > 0) return details.join(' ')
  }
  if (error instanceof ApiError && error.code === 'LOGIN_ID_TAKEN') {
    return 'Email hoặc số điện thoại này đã có tài khoản.'
  }
  return messageOf(error)
}

/**
 * UC-15 Quản lý tài khoản nhân viên (quản lý): xem, tạo tài khoản quầy/bếp, khóa và mở khóa (BR-32). Nhân viên đăng
 * nhập bằng email hoặc số điện thoại như khách. Tài khoản bị khóa không đăng nhập lại được; phiên đang mở vẫn dùng tới
 * khi hết hạn hoặc đăng xuất.
 */
export function AdminStaffPage() {
  const [staff, setStaff] = useState<StaffAccount[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [form, setForm] = useState(EMPTY)
  const [message, setMessage] = useState<{ kind: 'success' | 'danger'; text: string } | null>(null)
  const [busy, setBusy] = useState(false)
  const [tick, setTick] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    fetchStaff(controller.signal)
      .then((list) => {
        setStaff(list)
        setLoadError(null)
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) setLoadError(messageOf(error))
      })
    return () => controller.abort()
  }, [tick])

  const reload = useCallback(() => setTick((value) => value + 1), [])

  async function handleCreate(event: FormEvent) {
    event.preventDefault()
    if (form.loginId.trim() === '' || form.displayName.trim() === '' || form.password.length < 8) {
      setMessage({
        kind: 'danger',
        text: 'Nhập email hoặc số điện thoại, tên và mật khẩu tối thiểu 8 ký tự.',
      })
      return
    }
    setBusy(true)
    setMessage(null)
    try {
      const created = await createStaff(form)
      setForm(EMPTY)
      setMessage({ kind: 'success', text: `Đã tạo tài khoản ${created.loginId}.` })
      reload()
    } catch (error) {
      setMessage({ kind: 'danger', text: describe(error) })
    } finally {
      setBusy(false)
    }
  }

  async function toggle(account: StaffAccount) {
    setBusy(true)
    setMessage(null)
    try {
      setStaff(await setStaffLocked(account.id, !account.locked))
      setMessage({
        kind: 'success',
        text: `${account.locked ? 'Đã mở khóa' : 'Đã khóa'} tài khoản ${account.loginId}.`,
      })
    } catch (error) {
      setMessage({ kind: 'danger', text: describe(error) })
      reload()
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="content">
      <h1>Tài khoản nhân viên</h1>
      {message && <Alert kind={message.kind}>{message.text}</Alert>}
      {loadError && <Alert kind="danger">{loadError}</Alert>}

      <form className="admin-form" onSubmit={handleCreate} noValidate aria-label="Tạo tài khoản">
        <h2 className="section-title">Tạo tài khoản</h2>
        <div className="admin-row">
          <label className="field">
            <span>Email hoặc số điện thoại</span>
            <input
              value={form.loginId}
              autoComplete="off"
              onChange={(e) => setForm((f) => ({ ...f, loginId: e.target.value }))}
            />
          </label>
          <label className="field">
            <span>Tên hiển thị</span>
            <input
              value={form.displayName}
              onChange={(e) => setForm((f) => ({ ...f, displayName: e.target.value }))}
            />
          </label>
          <label className="field">
            <span>Mật khẩu ban đầu</span>
            <input
              type="password"
              autoComplete="new-password"
              value={form.password}
              onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
            />
          </label>
          <label className="field">
            <span>Vai trò</span>
            <select
              value={form.role}
              onChange={(e) => setForm((f) => ({ ...f, role: e.target.value as StaffRole }))}
            >
              <option value="COUNTER">Quầy</option>
              <option value="KITCHEN">Bếp</option>
            </select>
          </label>
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy}>
          Tạo tài khoản
        </button>
      </form>

      {staff && (
        <section aria-label="Danh sách nhân viên">
          <h2 className="section-title">Nhân viên ({staff.length})</h2>
          <ul className="slot-rows">
            {staff.map((account) => (
              <li
                key={account.id}
                className={`slot-row staff-row${account.locked ? ' locked' : ''}`}
              >
                <span className="slot-time">{account.displayName}</span>
                <span className="muted">
                  {account.loginId} · {ROLE_LABELS[account.role]}
                  {account.locked ? ' · Đã khóa' : ''}
                </span>
                <span className="slot-edit">
                  <button
                    type="button"
                    className="btn"
                    disabled={busy}
                    aria-label={`${account.locked ? 'Mở khóa' : 'Khóa'} ${account.loginId}`}
                    onClick={() => void toggle(account)}
                  >
                    {account.locked ? 'Mở khóa' : 'Khóa'}
                  </button>
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}
