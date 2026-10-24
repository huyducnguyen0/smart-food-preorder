import type { ReactNode } from 'react'
import { Icon } from './Icon'

/** Khung thông báo: lỗi (đỏ), cảnh báo (vàng) hoặc thành công (xanh). Có chữ nên không chỉ dựa vào màu. */
export function Alert({
  kind,
  children,
}: {
  kind: 'danger' | 'warning' | 'success'
  children: ReactNode
}) {
  return (
    <div className={`alert alert-${kind}`} role="alert">
      <Icon name={kind === 'success' ? 'check' : 'alert'} />
      <div>{children}</div>
    </div>
  )
}
