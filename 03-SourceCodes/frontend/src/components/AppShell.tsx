import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { useAuth } from '../features/auth/useAuth'
import { useCart } from '../features/cart/useCart'
import { Icon } from './Icon'

function Brand() {
  return (
    <Link className="brand" to="/">
      <span className="logo">15</span>
      <span>
        <b>Nhóm 15 Food</b>
        <small>
          <Icon name="walk" />
          Đặt trước · Tự đến lấy
        </small>
      </span>
    </Link>
  )
}

/**
 * Khung chung của mọi trang (UI Prototype mục 2). Khách và khách vãng lai: cột hẹp kiểu ứng dụng điện thoại, thanh
 * tab ở đáy. Nhân viên bếp: màn rộng, đầu trang tối, không có thanh tab.
 */
export function AppShell() {
  const { user, signOut } = useAuth()
  const { count } = useCart()
  const navigate = useNavigate()

  async function handleSignOut() {
    // Rời trang hiện tại trước: nếu đăng xuất trước, trang có bảo vệ vai trò (ví dụ /orders/2) tự chuyển tới đăng nhập
    // và nhớ địa chỉ cũ, người đăng nhập tiếp theo với vai trò khác sẽ bị đưa tới trang "Không có quyền".
    navigate('/', { replace: true })
    await signOut()
  }

  if (user?.role === 'KITCHEN' || user?.role === 'COUNTER') {
    return (
      <div className="app wide">
        <header className="k-top">
          <div className="k-top-inner">
            <span className="logo">15</span>
            <h1>{user.role === 'KITCHEN' ? 'Hàng chờ bếp' : 'Quầy'}</h1>
            <span className="muted-light">{user.displayName}</span>
            <button type="button" className="btn btn-dark" onClick={handleSignOut}>
              Đăng xuất
            </button>
          </div>
        </header>
        <main>
          <Outlet />
        </main>
      </div>
    )
  }

  return (
    <div className="app">
      <header className="topbar">
        <Brand />
        {!user && (
          <Link className="link-btn" to="/login">
            Đăng nhập
          </Link>
        )}
        {user?.role === 'CUSTOMER' && (
          <Link className="cart-link" to="/cart" aria-label={`Giỏ hàng, ${count} phần`}>
            <Icon name="bag" />
            {count > 0 && <span className="badge">{count}</span>}
          </Link>
        )}
      </header>
      <main>
        <Outlet />
      </main>
      <nav className="tabbar" aria-label="Điều hướng chính">
        <NavLink to="/" end>
          <Icon name="menu" />
          Thực đơn
        </NavLink>
        {user?.role === 'MANAGER' && (
          <>
            <NavLink to="/admin" end>
              <Icon name="alert" />
              Theo dõi
            </NavLink>
            <NavLink to="/counter">
              <Icon name="receipt" />
              Quầy
            </NavLink>
            <NavLink to="/admin/manage">
              <Icon name="check" />
              Quản lý
            </NavLink>
          </>
        )}
        {user?.role === 'CUSTOMER' && (
          <NavLink to="/orders">
            <Icon name="receipt" />
            Đơn của tôi
          </NavLink>
        )}
        {user ? (
          <button type="button" onClick={handleSignOut}>
            <Icon name="user" />
            Đăng xuất
          </button>
        ) : (
          <NavLink to="/login">
            <Icon name="user" />
            Đăng nhập
          </NavLink>
        )}
      </nav>
    </div>
  )
}
