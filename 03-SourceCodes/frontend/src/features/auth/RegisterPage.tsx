import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { ApiError } from '../../api/client'
import { messageOf } from '../../api/errorMessages'
import { Alert } from '../../components/Alert'
import { homePathOf } from './homePath'
import { useAuth } from './useAuth'

type Fields = { loginId: string; displayName: string; password: string }
type FieldErrors = Partial<Record<keyof Fields, string>>

/** Lỗi theo trường backend gửi kèm VALIDATION_ERROR, để hiện đúng dưới ô nhập. */
function fieldErrorsOf(error: unknown): FieldErrors {
  if (!(error instanceof ApiError) || !Array.isArray(error.extra.errors)) return {}
  const result: FieldErrors = {}
  for (const item of error.extra.errors as { field?: string; message?: string }[]) {
    if (item.field === 'loginId' || item.field === 'displayName' || item.field === 'password') {
      result[item.field] = item.message
    }
  }
  return result
}

/**
 * UC-01 Đăng ký tài khoản khách bằng email hoặc số điện thoại (BR-31). Đăng ký xong thì đã đăng nhập, về thực đơn để
 * đặt món ngay.
 */
export function RegisterPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const [fields, setFields] = useState<Fields>({ loginId: '', displayName: '', password: '' })
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (auth.status === 'authenticated') {
    return <Navigate to={homePathOf(auth.user.role)} replace />
  }

  function set(name: keyof Fields, value: string) {
    setFields((current) => ({ ...current, [name]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const errors: FieldErrors = {}
    if (!fields.loginId.trim()) errors.loginId = 'Vui lòng nhập email hoặc số điện thoại'
    if (!fields.displayName.trim()) errors.displayName = 'Vui lòng nhập tên'
    if (fields.password.length < 8) errors.password = 'Mật khẩu tối thiểu 8 ký tự'
    setFieldErrors(errors)
    setFormError(null)
    if (Object.keys(errors).length > 0) return

    setSubmitting(true)
    try {
      const user = await auth.signUp(fields)
      navigate(homePathOf(user.role), { replace: true })
    } catch (error) {
      setFieldErrors(fieldErrorsOf(error))
      setFormError(messageOf(error))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="content login-form" onSubmit={handleSubmit} noValidate>
      <h1 className="login-title">Tạo tài khoản</h1>
      <p className="muted">Đăng ký để đặt món trước và theo dõi đơn.</p>
      {formError && <Alert kind="danger">{formError}</Alert>}

      <label className="field">
        <span>Email hoặc số điện thoại</span>
        <input
          value={fields.loginId}
          onChange={(e) => set('loginId', e.target.value)}
          autoComplete="username"
          aria-invalid={fieldErrors.loginId ? true : undefined}
        />
        {fieldErrors.loginId && <small className="field-error">{fieldErrors.loginId}</small>}
      </label>

      <label className="field">
        <span>Tên hiển thị</span>
        <input
          value={fields.displayName}
          onChange={(e) => set('displayName', e.target.value)}
          autoComplete="name"
          aria-invalid={fieldErrors.displayName ? true : undefined}
        />
        {fieldErrors.displayName && (
          <small className="field-error">{fieldErrors.displayName}</small>
        )}
      </label>

      <label className="field">
        <span>Mật khẩu</span>
        <input
          type="password"
          value={fields.password}
          onChange={(e) => set('password', e.target.value)}
          autoComplete="new-password"
          aria-invalid={fieldErrors.password ? true : undefined}
        />
        {fieldErrors.password && <small className="field-error">{fieldErrors.password}</small>}
      </label>

      <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
        {submitting ? 'Đang tạo tài khoản...' : 'Đăng ký'}
      </button>
      <p className="muted center">
        Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
      </p>
    </form>
  )
}
