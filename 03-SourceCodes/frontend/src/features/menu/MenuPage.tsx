import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { fetchMenu, type MenuCategory, type MenuItem } from '../../api/menu'
import { messageOf } from '../../api/errorMessages'
import { Alert } from '../../components/Alert'
import { Icon } from '../../components/Icon'
import { formatMoney } from '../../lib/format'
import { useAuth } from '../auth/useAuth'

type MenuState =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; categories: MenuCategory[] }

function ItemRow({ item, onAdd }: { item: MenuItem; onAdd?: () => void }) {
  return (
    <li className={`item${item.available ? '' : ' item-off'}`}>
      {item.imageUrl ? (
        <img className="thumb" src={item.imageUrl} alt="" loading="lazy" />
      ) : (
        <div className="thumb thumb-empty" aria-hidden="true">
          <Icon name="menu" />
        </div>
      )}
      <div className="item-body">
        <b>{item.name}</b>
        {item.description && <p>{item.description}</p>}
        <span className="price">{formatMoney(item.price)}</span>
        {!item.available && <span className="tag">Tạm hết</span>}
      </div>
      {item.available && onAdd && (
        <button
          type="button"
          className="add"
          onClick={onAdd}
          aria-label={`Thêm ${item.name} vào giỏ`}
        >
          <Icon name="plus" />
        </button>
      )}
    </li>
  )
}

/**
 * UC-03 Xem menu. Mở được khi chưa đăng nhập (BR-34): món đang bán theo nhóm, kèm giá; món tạm hết có nhãn và không
 * có nút thêm (BR-04); món ngừng bán không có trong dữ liệu (BR-03).
 */
export function MenuPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [state, setState] = useState<MenuState>({ kind: 'loading' })
  const [activeCategory, setActiveCategory] = useState<number | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    fetchMenu(controller.signal)
      .then((categories) => setState({ kind: 'ready', categories }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        // UC-03 E1: báo lỗi và cho thử lại; không hiện dữ liệu cũ như dữ liệu hiện tại.
        setState({ kind: 'error', message: messageOf(error) })
      })
    return () => controller.abort()
  }, [attempt])

  const retry = useCallback(() => {
    setState({ kind: 'loading' })
    setAttempt((value) => value + 1)
  }, [])

  // UC-03 A3: khách vãng lai bấm thêm vào giỏ thì phải đăng nhập; món chưa được thêm, khách chọn lại sau khi quay về.
  // TODO(T-17): khách đã đăng nhập sẽ thêm món vào giỏ; tới lúc đó nút chỉ hiện cho khách vãng lai.
  const onAdd = user ? undefined : () => navigate('/login', { state: { from: '/' } })

  return (
    <>
      <div className="hero">
        <strong>Đặt trước, đến là có</strong>
        <span>Chọn giờ nhận, cửa hàng làm sẵn cho bạn.</span>
      </div>

      {state.kind === 'loading' && (
        <p className="muted center" role="status">
          Đang tải thực đơn...
        </p>
      )}

      {state.kind === 'error' && (
        <div className="content">
          <Alert kind="danger">{state.message}</Alert>
          <button type="button" className="btn btn-primary btn-block" onClick={retry}>
            Thử lại
          </button>
        </div>
      )}

      {state.kind === 'ready' && state.categories.length === 0 && (
        <p className="empty">Hiện chưa có món.</p>
      )}

      {state.kind === 'ready' && state.categories.length > 0 && (
        <>
          <nav className="chips" aria-label="Nhóm món">
            <button
              type="button"
              className={`chip${activeCategory === null ? ' active' : ''}`}
              aria-pressed={activeCategory === null}
              onClick={() => setActiveCategory(null)}
            >
              Tất cả
            </button>
            {state.categories.map((category) => (
              <button
                key={category.id}
                type="button"
                className={`chip${activeCategory === category.id ? ' active' : ''}`}
                aria-pressed={activeCategory === category.id}
                onClick={() => setActiveCategory(category.id)}
              >
                {category.name}
              </button>
            ))}
          </nav>
          <div className="content menu-list">
            {state.categories
              .filter((category) => activeCategory === null || category.id === activeCategory)
              .map((category) => (
                <section key={category.id} aria-labelledby={`cat-${category.id}`}>
                  <h2 id={`cat-${category.id}`} className="section-title">
                    {category.name}
                  </h2>
                  <ul className="items">
                    {category.items.map((item) => (
                      <ItemRow key={item.id} item={item} onAdd={onAdd} />
                    ))}
                  </ul>
                </section>
              ))}
          </div>
        </>
      )}
    </>
  )
}
