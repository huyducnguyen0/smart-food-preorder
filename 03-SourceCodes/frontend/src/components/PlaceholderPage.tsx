import { Link } from 'react-router'

/** Trang giữ chỗ cho màn hình chưa làm; được thay bằng màn thật ở task tương ứng. */
export function PlaceholderPage({ title, task }: { title: string; task: string }) {
  return (
    <div className="empty">
      <h1>{title}</h1>
      <p className="muted">Màn hình này đang được xây dựng ({task}).</p>
      <Link className="btn btn-primary" to="/">
        Về thực đơn
      </Link>
    </div>
  )
}
