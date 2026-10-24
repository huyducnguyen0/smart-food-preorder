import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { messageOf } from '../../api/errorMessages'
import { Alert } from '../../components/Alert'
import { homePathOf } from './homePath'
import { useAuth } from './useAuth'

type FieldErrors = { loginId?: string; password?: string }

/** UC-02 bước 1–6: nhập định danh và mật khẩu; quay về trang đang mở nếu bị chuyển tới từ thao tác cần đăng nhập. */
export function LoginPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const from = (location.state as { from?: string } | null)?.from

  if (auth.status === 'authenticated') {
    return <Navigate to={from ?? homePathOf(auth.user.role)} replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    // UC-02 E3: chỉ ra trường còn thiếu, không gửi lên backend.
    const errors: FieldErrors = {}
    if (!loginId.trim()) errors.loginId = 'Vui lòng nhập email hoặc số điện thoại'
    if (!password) errors.password = 'Vui lòng nhập mật khẩu'
    setFieldErrors(errors)
    setFormError(null)
    if (errors.loginId || errors.password) return

    setSubmitting(true)
    try {
      const user = await auth.signIn(loginId, password)
      navigate(from ?? homePathOf(user.role), { replace: true })
    } catch (error) {
      // UC-02 E1: giữ định danh đã nhập, xóa ô mật khẩu.
      setPassword('')
      setFormError(messageOf(error))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <div className="login-hero" aria-hidden="true" />
      <form className="content login-form" onSubmit={handleSubmit} noValidate>
        <h1 className="login-title">Chào bạn</h1>
        <p className="muted">Đăng nhập để đặt món và theo dõi đơn.</p>
        {formError && <Alert kind="danger">{formError}</Alert>}

        <label className="field">
          <span>Email hoặc số điện thoại</span>
          <input
            value={loginId}
            onChange={(e) => setLoginId(e.target.value)}
            autoComplete="username"
            aria-invalid={fieldErrors.loginId ? true : undefined}
          />
          {fieldErrors.loginId && <small className="field-error">{fieldErrors.loginId}</small>}
        </label>

        <label className="field">
          <span>Mật khẩu</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            aria-invalid={fieldErrors.password ? true : undefined}
          />
          {fieldErrors.password && <small className="field-error">{fieldErrors.password}</small>}
        </label>

        <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
      </form>
    </>
  )
}
