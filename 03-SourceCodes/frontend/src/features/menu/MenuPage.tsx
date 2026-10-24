import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { fetchMenu, fetchMenuItem, type MenuCategory, type MenuItem } from '../../api/menu'
import { ApiError } from '../../api/client'
import { messageOf } from '../../api/errorMessages'
import { Alert } from '../../components/Alert'
import { Icon } from '../../components/Icon'
import { formatMoney } from '../../lib/format'
import { useAuth } from '../auth/useAuth'
import { CartBar } from '../cart/CartBar'
import { useCart } from '../cart/useCart'

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
  const { add } = useCart()
  const navigate = useNavigate()
  const [state, setState] = useState<MenuState>({ kind: 'loading' })
  const [activeCategory, setActiveCategory] = useState<number | null>(null)
  const [attempt, setAttempt] = useState(0)
  const [notice, setNotice] = useState<string | null>(null)

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

  // UC-04 bước 1–2 và E1: hỏi lại tình trạng món rồi mới thêm, vì trang menu có thể đã cũ. Món vừa tạm hết hoặc ngừng
  // bán thì không thêm, báo khách và tải lại menu.
  const addToCart = useCallback(
    async (item: MenuItem) => {
      setNotice(null)
      try {
        const current = await fetchMenuItem(item.id)
        if (!current.available) {
          setNotice(`${item.name} vừa tạm hết, chưa thêm được vào giỏ.`)
          retry()
          return
        }
        add(item)
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) {
          setNotice(`${item.name} không còn trong thực đơn, chưa thêm được vào giỏ.`)
          retry()
          return
        }
        setNotice(messageOf(error))
      }
    },
    [add, retry],
  )

  // UC-03 A3: khách vãng lai bấm thêm vào giỏ thì phải đăng nhập; món chưa được thêm, khách chọn lại sau khi quay về.
  // Vai trò khác khách (bếp, quản lý) không có giỏ nên không hiện nút.
  const onAdd = (item: MenuItem) => {
    if (!user) return () => navigate('/login', { state: { from: '/' } })
    if (user.role === 'CUSTOMER') return () => void addToCart(item)
    return undefined
  }

  return (
    <>
      <div className="hero">
        <strong>Đặt trước, đến là có</strong>
        <span>Chọn giờ nhận, cửa hàng làm sẵn cho bạn.</span>
      </div>

      {notice && (
        <div className="content notice">
          <Alert kind="warning">{notice}</Alert>
        </div>
      )}

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
                      <ItemRow key={item.id} item={item} onAdd={onAdd(item)} />
                    ))}
                  </ul>
                </section>
              ))}
          </div>
        </>
      )}

      {user?.role === 'CUSTOMER' && <CartBar />}
    </>
  )
}
